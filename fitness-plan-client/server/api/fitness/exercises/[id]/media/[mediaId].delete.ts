import { eq, and } from 'drizzle-orm'
import { db } from '../../../../../utils/db'
import { exerciseMedia, templateExercises, templateWorkoutBlocks, templateWorkouts, templatePhases, programTemplates } from '../../../../../../database/schema/templates'
import { userPrograms } from '../../../../../../database/schema/user-config'

export default defineEventHandler(async (event) => {
  const user = event.context.user
  if (!user) throw createError({ statusCode: 401, statusMessage: 'Unauthorized' })

  const id = getRouterParam(event, 'id')
  const mediaId = getRouterParam(event, 'mediaId')
  if (!id || !mediaId) throw createError({ statusCode: 400, statusMessage: 'IDs required' })

  const ownership = await db.select({ id: templateExercises.id })
    .from(templateExercises)
    .innerJoin(templateWorkoutBlocks, eq(templateExercises.blockId, templateWorkoutBlocks.id))
    .innerJoin(templateWorkouts, eq(templateWorkoutBlocks.templateWorkoutId, templateWorkouts.id))
    .innerJoin(templatePhases, eq(templateWorkouts.phaseId, templatePhases.id))
    .innerJoin(programTemplates, eq(templatePhases.templateId, programTemplates.id))
    .innerJoin(userPrograms, eq(userPrograms.templateId, programTemplates.id))
    .where(and(eq(templateExercises.id, id), eq(userPrograms.userId, user.id)))
    .limit(1)

  if (!ownership.length) throw createError({ statusCode: 404, statusMessage: 'Not found' })

  await db.delete(exerciseMedia)
    .where(and(eq(exerciseMedia.id, mediaId), eq(exerciseMedia.exerciseId, id)))

  setResponseStatus(event, 204)
  return null
})
