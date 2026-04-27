import { eq, and, isNull, desc } from 'drizzle-orm'
import { db } from '../../../utils/db'
import { coachingEvents } from '../../../../database/schema/coaching'

export default defineEventHandler(async (event) => {
  const user = event.context.user
  if (!user) throw createError({ statusCode: 401, statusMessage: 'Unauthorized' })

  const suggestions = await db.select({
    id: coachingEvents.id,
    eventType: coachingEvents.eventType,
    details: coachingEvents.details,
    accepted: coachingEvents.accepted,
    suggestedAt: coachingEvents.suggestedAt,
  })
    .from(coachingEvents)
    .where(and(
      eq(coachingEvents.userId, user.id),
      isNull(coachingEvents.resolvedAt),
    ))
    .orderBy(desc(coachingEvents.suggestedAt))
    .limit(10)

  return suggestions
})
