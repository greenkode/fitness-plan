import { eq, and } from 'drizzle-orm'
import { generateText } from 'ai'
import { db } from '../../../../utils/db'
import { templateExercises, templateWorkoutBlocks, templateWorkouts, templatePhases, programTemplates } from '../../../../../database/schema/templates'
import { userPrograms } from '../../../../../database/schema/user-config'
import { resolveModelSet } from '../../../../utils/ai-model'

const SYSTEM = `You write concise exercise descriptions for a fitness app. Given an exercise name (and optional context), return ONE description of 1-2 sentences covering execution cues and target muscles. No headers, no labels, no quotes — just the plain description text.`

export default defineEventHandler(async (event) => {
  const user = event.context.user
  if (!user) throw createError({ statusCode: 401, statusMessage: 'Unauthorized' })

  const id = getRouterParam(event, 'id')
  if (!id) throw createError({ statusCode: 400, statusMessage: 'Exercise ID required' })

  const [exercise] = await db.select({
    id: templateExercises.id,
    name: templateExercises.name,
    prescription: templateExercises.prescription,
    workoutTitle: templateWorkouts.title,
    workoutType: templateWorkouts.workoutType,
    blockTitle: templateWorkoutBlocks.title,
  })
    .from(templateExercises)
    .innerJoin(templateWorkoutBlocks, eq(templateExercises.blockId, templateWorkoutBlocks.id))
    .innerJoin(templateWorkouts, eq(templateWorkoutBlocks.templateWorkoutId, templateWorkouts.id))
    .innerJoin(templatePhases, eq(templateWorkouts.phaseId, templatePhases.id))
    .innerJoin(programTemplates, eq(templatePhases.templateId, programTemplates.id))
    .innerJoin(userPrograms, eq(userPrograms.templateId, programTemplates.id))
    .where(and(eq(templateExercises.id, id), eq(userPrograms.userId, user.id)))
    .limit(1)

  if (!exercise) {
    throw createError({ statusCode: 404, statusMessage: 'Exercise not found' })
  }

  const models = await resolveModelSet(user.id)

  const prompt = `Exercise: ${exercise.name}
Prescription: ${exercise.prescription || 'n/a'}
Workout: ${exercise.workoutTitle} (${exercise.workoutType})
Block: ${exercise.blockTitle}

Write the description.`

  try {
    const { text } = await generateText({
      model: models.small,
      system: SYSTEM,
      prompt,
    })

    const description = text.trim().replace(/^["']|["']$/g, '').trim()
    if (!description) {
      throw createError({ statusCode: 502, statusMessage: 'AI returned an empty description' })
    }

    const [updated] = await db.update(templateExercises)
      .set({ description })
      .where(eq(templateExercises.id, id))
      .returning({ id: templateExercises.id, description: templateExercises.description })

    return updated
  } catch (err: any) {
    if (err.statusCode) throw err
    console.error('Generate description error:', err.message)
    throw createError({ statusCode: 502, statusMessage: 'Failed to generate description' })
  }
})
