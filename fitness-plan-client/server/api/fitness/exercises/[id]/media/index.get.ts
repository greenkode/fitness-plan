import { eq } from 'drizzle-orm'
import { db } from '../../../../../utils/db'
import { exerciseMedia } from '../../../../../../database/schema/templates'

export default defineEventHandler(async (event) => {
  const user = event.context.user
  if (!user) throw createError({ statusCode: 401, statusMessage: 'Unauthorized' })

  const id = getRouterParam(event, 'id')
  if (!id) throw createError({ statusCode: 400, statusMessage: 'Exercise ID required' })

  return await db.select({
    id: exerciseMedia.id,
    url: exerciseMedia.url,
    mediaType: exerciseMedia.mediaType,
    label: exerciseMedia.label,
    sortOrder: exerciseMedia.sortOrder,
  })
    .from(exerciseMedia)
    .where(eq(exerciseMedia.exerciseId, id))
    .orderBy(exerciseMedia.sortOrder)
})
