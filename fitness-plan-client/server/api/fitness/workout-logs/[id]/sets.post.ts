import { eq, and } from 'drizzle-orm'
import { db } from '../../../../utils/db'
import { exerciseLogs, setLogs, workoutLogs } from '../../../../../database/schema/tracking'

export default defineEventHandler(async (event) => {
  const user = event.context.user
  const workoutLogId = getRouterParam(event, 'id')
  const body = await readBody(event)

  if (!workoutLogId) {
    throw createError({ statusCode: 400, statusMessage: 'Workout ID required' })
  }

  try {
    const workout = await db.query.workoutLogs.findFirst({
      where: and(eq(workoutLogs.id, workoutLogId), eq(workoutLogs.userId, user.id)),
      columns: { id: true },
    })

    if (!workout) {
      throw createError({ statusCode: 404, statusMessage: 'Workout not found' })
    }

    let exerciseLogId = body.exerciseLogId as string | undefined
    const exerciseName = body.exerciseName as string | undefined
    const templateExerciseId = body.templateExerciseId as string | undefined

    if (!exerciseLogId) {
      const conditions = [eq(exerciseLogs.workoutLogId, workoutLogId)]

      if (exerciseName) {
        conditions.push(eq(exerciseLogs.exerciseName, exerciseName))
      }

      const existing = await db.query.exerciseLogs.findFirst({
        where: and(...conditions),
        columns: { id: true },
      })

      if (existing) {
        exerciseLogId = existing.id
      } else {
        const [created] = await db.insert(exerciseLogs).values({
          workoutLogId,
          templateExerciseId: templateExerciseId || null,
          exerciseName: exerciseName || 'Exercise',
          sortOrder: body.sortOrder ?? 0,
        }).returning({ id: exerciseLogs.id })
        exerciseLogId = created.id
      }
    }

    if (!exerciseLogId) {
      throw createError({ statusCode: 400, statusMessage: 'Exercise identification required' })
    }

    const [setLog] = await db.insert(setLogs).values({
      exerciseLogId,
      setNumber: body.setNumber,
      weightKg: body.weightKg,
      reps: body.reps,
      rpe: body.rpe,
      completed: true,
    }).returning({ id: setLogs.id })

    return { id: setLog.id }
  } catch (err: any) {
    if (err.statusCode) throw err
    console.error('Set logging error:', err.message)
    throw createError({ statusCode: 500, statusMessage: 'Failed to log set' })
  }
})
