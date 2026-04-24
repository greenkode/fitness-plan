import { db } from '../../../utils/db'
import { workoutLogs, exerciseLogs, setLogs } from '../../../../database/schema/tracking'
import { workoutLogSchema } from '../../../utils/validation'

export default defineEventHandler(async (event) => {
  const user = event.context.user
  const body = await readValidatedBody(event, workoutLogSchema.parse)

  const [workoutLog] = await db.insert(workoutLogs).values({
    userId: user.id,
    assignmentId: body.assignmentId,
    workoutDate: body.workoutDate,
    startedAt: new Date(),
    notes: body.notes,
    overallRpe: body.overallRpe,
  }).returning({ id: workoutLogs.id, startedAt: workoutLogs.startedAt })

  for (let i = 0; i < body.exercises.length; i++) {
    const exercise = body.exercises[i]

    const [exerciseLog] = await db.insert(exerciseLogs).values({
      workoutLogId: workoutLog.id,
      templateExerciseId: exercise.templateExerciseId,
      exerciseName: exercise.exerciseName,
      sortOrder: i,
    }).returning({ id: exerciseLogs.id })

    if (exercise.sets.length > 0) {
      await db.insert(setLogs).values(
        exercise.sets.map((set) => ({
          exerciseLogId: exerciseLog.id,
          setNumber: set.setNumber,
          weightKg: set.weightKg,
          reps: set.reps,
          durationSeconds: set.durationSeconds,
          rpe: set.rpe,
          completed: set.completed,
          notes: set.notes,
        })),
      )
    }
  }

  return { id: workoutLog.id, startedAt: workoutLog.startedAt }
})
