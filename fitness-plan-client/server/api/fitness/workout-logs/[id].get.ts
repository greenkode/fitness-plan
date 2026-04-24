import { eq, and } from 'drizzle-orm'
import { db } from '../../../utils/db'
import { workoutLogs } from '../../../../database/schema/tracking'

export default defineEventHandler(async (event) => {
  const user = event.context.user
  const id = getRouterParam(event, 'id')

  if (!id) {
    throw createError({ statusCode: 400, statusMessage: 'Workout ID required' })
  }

  const workout = await db.query.workoutLogs.findFirst({
    where: and(eq(workoutLogs.id, id), eq(workoutLogs.userId, user.id)),
  })

  if (!workout) {
    throw createError({ statusCode: 404, statusMessage: 'Workout not found' })
  }

  return workout
})
