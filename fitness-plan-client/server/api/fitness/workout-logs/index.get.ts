import { eq, and, between } from 'drizzle-orm'
import { db } from '../../../utils/db'
import { workoutLogs } from '../../../../database/schema/tracking'

export default defineEventHandler(async (event) => {
  const user = event.context.user
  const query = getQuery(event)
  const from = query.from as string
  const to = query.to as string

  const conditions = [eq(workoutLogs.userId, user.id)]

  if (from && to) {
    conditions.push(between(workoutLogs.workoutDate, from, to))
  }

  return db.select()
    .from(workoutLogs)
    .where(and(...conditions))
})
