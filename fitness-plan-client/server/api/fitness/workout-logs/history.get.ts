import { eq, desc, and, isNull, lt } from 'drizzle-orm'
import { db } from '../../../utils/db'
import { workoutLogs, exerciseLogs, setLogs } from '../../../../database/schema/tracking'

export default defineEventHandler(async (event) => {
  const user = event.context.user

  try {
    const staleThreshold = new Date(Date.now() - 24 * 60 * 60 * 1000)
    await db.update(workoutLogs)
      .set({ completedAt: staleThreshold })
      .where(and(
        eq(workoutLogs.userId, user.id),
        isNull(workoutLogs.completedAt),
        lt(workoutLogs.startedAt, staleThreshold),
      ))
    const workouts = await db.select({
      id: workoutLogs.id,
      workoutDate: workoutLogs.workoutDate,
      startedAt: workoutLogs.startedAt,
      completedAt: workoutLogs.completedAt,
      notes: workoutLogs.notes,
      overallRpe: workoutLogs.overallRpe,
    }).from(workoutLogs)
      .where(eq(workoutLogs.userId, user.id))
      .orderBy(desc(workoutLogs.workoutDate))
      .limit(50)

    const results = await Promise.all(workouts.map(async (w) => {
      const exercises = await db.select({
        id: exerciseLogs.id,
        exerciseName: exerciseLogs.exerciseName,
        sortOrder: exerciseLogs.sortOrder,
      }).from(exerciseLogs)
        .where(eq(exerciseLogs.workoutLogId, w.id))
        .orderBy(exerciseLogs.sortOrder)

      const exercisesWithSets = await Promise.all(exercises.map(async (ex) => {
        const sets = await db.select({
          setNumber: setLogs.setNumber,
          weightKg: setLogs.weightKg,
          reps: setLogs.reps,
          durationSeconds: setLogs.durationSeconds,
          rpe: setLogs.rpe,
          completed: setLogs.completed,
        }).from(setLogs)
          .where(eq(setLogs.exerciseLogId, ex.id))
          .orderBy(setLogs.setNumber)

        return { ...ex, sets }
      }))

      const totalSets = exercisesWithSets.reduce((sum, ex) => sum + ex.sets.length, 0)
      const totalVolume = exercisesWithSets.reduce((sum, ex) =>
        sum + ex.sets.reduce((s, set) => s + ((set.weightKg || 0) * (set.reps || 0)), 0), 0)

      let durationMinutes: number | null = null
      if (w.startedAt && w.completedAt) {
        durationMinutes = Math.round((new Date(w.completedAt).getTime() - new Date(w.startedAt).getTime()) / 60000)
      }

      return {
        id: w.id,
        workoutDate: w.workoutDate,
        startedAt: w.startedAt,
        completedAt: w.completedAt,
        durationMinutes,
        totalSets,
        totalVolume,
        exercises: exercisesWithSets,
      }
    }))

    return results
  } catch (err: any) {
    if (err.statusCode) throw err
    console.error('History fetch error:', err.message)
    throw createError({ statusCode: 500, statusMessage: 'Failed to fetch workout history' })
  }
})
