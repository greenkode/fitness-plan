import { eq, and } from 'drizzle-orm'
import { db } from '../../../../utils/db'
import { coachingEvents } from '../../../../../database/schema/coaching'

export default defineEventHandler(async (event) => {
  const user = event.context.user
  if (!user) throw createError({ statusCode: 401, statusMessage: 'Unauthorized' })

  const id = getRouterParam(event, 'id')
  if (!id) throw createError({ statusCode: 400, statusMessage: 'ID required' })

  const { accepted } = await readBody(event)

  const [updated] = await db.update(coachingEvents)
    .set({ accepted, resolvedAt: new Date() })
    .where(and(eq(coachingEvents.id, id), eq(coachingEvents.userId, user.id)))
    .returning()

  if (!updated) throw createError({ statusCode: 404, statusMessage: 'Suggestion not found' })
  return updated
})
