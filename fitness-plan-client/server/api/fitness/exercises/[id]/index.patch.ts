import { eq, and } from 'drizzle-orm'
import { db } from '../../../../utils/db'
import { templateExercises, templateWorkoutBlocks, templateWorkouts, templatePhases, programTemplates } from '../../../../../database/schema/templates'
import { userPrograms } from '../../../../../database/schema/user-config'

export default defineEventHandler(async (event) => {
  const user = event.context.user
  if (!user) throw createError({ statusCode: 401, statusMessage: 'Unauthorized' })

  const id = getRouterParam(event, 'id')
  if (!id) throw createError({ statusCode: 400, statusMessage: 'Exercise ID required' })

  const body = await readBody(event)
  const description: string | null | undefined = body?.description === undefined
    ? undefined
    : (typeof body.description === 'string' ? body.description.trim() : null)

  if (description === undefined) {
    throw createError({ statusCode: 400, statusMessage: 'No fields to update' })
  }

  const ownership = await db.select({ id: templateExercises.id })
    .from(templateExercises)
    .innerJoin(templateWorkoutBlocks, eq(templateExercises.blockId, templateWorkoutBlocks.id))
    .innerJoin(templateWorkouts, eq(templateWorkoutBlocks.templateWorkoutId, templateWorkouts.id))
    .innerJoin(templatePhases, eq(templateWorkouts.phaseId, templatePhases.id))
    .innerJoin(programTemplates, eq(templatePhases.templateId, programTemplates.id))
    .innerJoin(userPrograms, eq(userPrograms.templateId, programTemplates.id))
    .where(and(eq(templateExercises.id, id), eq(userPrograms.userId, user.id)))
    .limit(1)

  if (!ownership.length) {
    throw createError({ statusCode: 404, statusMessage: 'Exercise not found' })
  }

  const [updated] = await db.update(templateExercises)
    .set({ description: description || null })
    .where(eq(templateExercises.id, id))
    .returning({ id: templateExercises.id, description: templateExercises.description })

  return updated
})
