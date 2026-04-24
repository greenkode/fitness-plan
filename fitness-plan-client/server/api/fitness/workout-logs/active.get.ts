import { eq, and, isNull } from 'drizzle-orm'
import { db } from '../../../utils/db'
import { workoutLogs, exerciseLogs, setLogs } from '../../../../database/schema/tracking'

export default defineEventHandler(async (event) => {
  const user = event.context.user
  const query = getQuery(event)
  const date = query.date as string | undefined

  try {
    const conditions = [
      eq(workoutLogs.userId, user.id),
      isNull(workoutLogs.completedAt),
    ]

    if (date) {
      conditions.push(eq(workoutLogs.workoutDate, date))
    }

    const workout = await db.query.workoutLogs.findFirst({
      where: and(...conditions),
      orderBy: (wl, { desc }) => [desc(wl.startedAt)],
    })

    if (!workout) return null

    const exercises = await db.select().from(exerciseLogs)
      .where(eq(exerciseLogs.workoutLogId, workout.id))
      .orderBy(exerciseLogs.sortOrder)

    const exercisesWithSets = await Promise.all(exercises.map(async (ex) => {
      const sets = await db.select({
        id: setLogs.id,
        setNumber: setLogs.setNumber,
        weightKg: setLogs.weightKg,
        reps: setLogs.reps,
        rpe: setLogs.rpe,
        completed: setLogs.completed,
      }).from(setLogs)
        .where(eq(setLogs.exerciseLogId, ex.id))
        .orderBy(setLogs.setNumber)

      return {
        exerciseName: ex.exerciseName,
        sortOrder: ex.sortOrder,
        sets,
      }
    }))

    return {
      id: workout.id,
      workoutDate: workout.workoutDate,
      startedAt: workout.startedAt,
      exerciseLogs: exercisesWithSets,
    }
  } catch (err: any) {
    if (err.statusCode) throw err
    console.error('Active workout fetch error:', err.message)
    throw createError({ statusCode: 500, statusMessage: 'Failed to fetch active workout' })
  }
})
