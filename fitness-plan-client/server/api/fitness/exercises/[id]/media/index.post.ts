import { eq, and } from 'drizzle-orm'
import { db } from '../../../../../utils/db'
import { templateExercises, exerciseMedia, templateWorkoutBlocks, templateWorkouts, templatePhases, programTemplates } from '../../../../../../database/schema/templates'
import { userPrograms } from '../../../../../../database/schema/user-config'
import { detectMediaType } from '../../../../../utils/media'

export default defineEventHandler(async (event) => {
  const user = event.context.user
  if (!user) throw createError({ statusCode: 401, statusMessage: 'Unauthorized' })

  const id = getRouterParam(event, 'id')
  if (!id) throw createError({ statusCode: 400, statusMessage: 'Exercise ID required' })

  const { url, label } = await readBody(event)

  if (!url?.trim()) {
    throw createError({ statusCode: 400, statusMessage: 'URL required' })
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

  const existing = await db.select({ count: exerciseMedia.id })
    .from(exerciseMedia)
    .where(eq(exerciseMedia.exerciseId, id))

  const [created] = await db.insert(exerciseMedia).values({
    exerciseId: id,
    url: url.trim(),
    mediaType: detectMediaType(url),
    label: label?.trim() || null,
    sortOrder: existing.length,
  }).returning()

  return created
})
