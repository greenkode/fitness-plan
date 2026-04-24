import { eq } from 'drizzle-orm'
import { db } from '../../../utils/db'
import { programTemplates, templatePhases, templateWorkouts, templateWorkoutBlocks, templateExercises, templateWorkoutTips } from '../../../../database/schema'

export default defineEventHandler(async (event) => {
  const id = getRouterParam(event, 'id')
  if (!id) {
    throw createError({ statusCode: 400, statusMessage: 'Template ID required' })
  }

  const template = await db.query.programTemplates.findFirst({
    where: eq(programTemplates.id, id),
  })

  if (!template) {
    throw createError({ statusCode: 404, statusMessage: 'Template not found' })
  }

  const phases = await db.select().from(templatePhases)
    .where(eq(templatePhases.templateId, id))
    .orderBy(templatePhases.phaseNumber)

  const phasesWithWorkouts = await Promise.all(phases.map(async (phase) => {
    const workouts = await db.select().from(templateWorkouts)
      .where(eq(templateWorkouts.phaseId, phase.id))
      .orderBy(templateWorkouts.sortOrder)

    const workoutsWithDetails = await Promise.all(workouts.map(async (workout) => {
      const [blocks, tips] = await Promise.all([
        db.select().from(templateWorkoutBlocks)
          .where(eq(templateWorkoutBlocks.templateWorkoutId, workout.id))
          .orderBy(templateWorkoutBlocks.sortOrder),
        db.select().from(templateWorkoutTips)
          .where(eq(templateWorkoutTips.templateWorkoutId, workout.id))
          .orderBy(templateWorkoutTips.sortOrder),
      ])

      const blocksWithExercises = await Promise.all(blocks.map(async (block) => {
        const exercises = await db.select().from(templateExercises)
          .where(eq(templateExercises.blockId, block.id))
          .orderBy(templateExercises.sortOrder)
        return { ...block, exercises }
      }))

      return { ...workout, blocks: blocksWithExercises, tips }
    }))

    return { ...phase, workouts: workoutsWithDetails }
  }))

  return { ...template, phases: phasesWithWorkouts }
})
