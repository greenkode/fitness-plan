import { eq, and } from 'drizzle-orm'
import { db } from '../../../utils/db'
import { workoutLogs } from '../../../../database/schema/tracking'

export default defineEventHandler(async (event) => {
  const user = event.context.user
  const id = getRouterParam(event, 'id')

  if (!id) {
    throw createError({ statusCode: 400, statusMessage: 'Workout ID required' })
  }

  const result = await db.delete(workoutLogs)
    .where(and(eq(workoutLogs.id, id), eq(workoutLogs.userId, user.id)))
    .returning({ id: workoutLogs.id })

  if (result.length === 0) {
    throw createError({ statusCode: 404, statusMessage: 'Workout not found' })
  }

  setResponseStatus(event, 204)
  return null
})
