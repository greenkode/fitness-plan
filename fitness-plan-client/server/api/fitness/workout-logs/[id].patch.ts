import { eq, and } from 'drizzle-orm'
import { db } from '../../../utils/db'
import { workoutLogs } from '../../../../database/schema/tracking'

export default defineEventHandler(async (event) => {
  const user = event.context.user
  const id = getRouterParam(event, 'id')!
  const body = await readBody(event)

  const [updated] = await db.update(workoutLogs)
    .set({
      completedAt: body.completedAt ? new Date(body.completedAt) : undefined,
      notes: body.notes,
      overallRpe: body.overallRpe,
    })
    .where(and(eq(workoutLogs.id, id), eq(workoutLogs.userId, user.id)))
    .returning()

  if (!updated) {
    throw createError({ statusCode: 404, statusMessage: 'Workout log not found' })
  }

  return updated
})
