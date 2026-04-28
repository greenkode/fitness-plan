import { eq, and, desc } from 'drizzle-orm'
import { db } from '../../../../utils/db'
import { templateExercises, exerciseMedia, templateWorkoutBlocks, templateWorkouts, templatePhases, programTemplates } from '../../../../../database/schema/templates'
import { userPrograms } from '../../../../../database/schema/user-config'
import { exerciseLogs, setLogs, workoutLogs } from '../../../../../database/schema/tracking'

export default defineEventHandler(async (event) => {
  const user = event.context.user
  if (!user) throw createError({ statusCode: 401, statusMessage: 'Unauthorized' })

  const id = getRouterParam(event, 'id')
  if (!id) throw createError({ statusCode: 400, statusMessage: 'Exercise ID required' })

  const [exercise] = await db.select({
    id: templateExercises.id,
    name: templateExercises.name,
    prescription: templateExercises.prescription,
    description: templateExercises.description,
  })
    .from(templateExercises)
    .innerJoin(templateWorkoutBlocks, eq(templateExercises.blockId, templateWorkoutBlocks.id))
    .innerJoin(templateWorkouts, eq(templateWorkoutBlocks.templateWorkoutId, templateWorkouts.id))
    .innerJoin(templatePhases, eq(templateWorkouts.phaseId, templatePhases.id))
    .innerJoin(programTemplates, eq(templatePhases.templateId, programTemplates.id))
    .innerJoin(userPrograms, eq(userPrograms.templateId, programTemplates.id))
    .where(and(eq(templateExercises.id, id), eq(userPrograms.userId, user.id)))
    .limit(1)

  if (!exercise) {
    throw createError({ statusCode: 404, statusMessage: 'Exercise not found' })
  }

  const media = await db.select({
    id: exerciseMedia.id,
    url: exerciseMedia.url,
    mediaType: exerciseMedia.mediaType,
    label: exerciseMedia.label,
    sortOrder: exerciseMedia.sortOrder,
  })
    .from(exerciseMedia)
    .where(eq(exerciseMedia.exerciseId, id))
    .orderBy(exerciseMedia.sortOrder)

  const sets = await db.select({
    id: setLogs.id,
    setNumber: setLogs.setNumber,
    weightKg: setLogs.weightKg,
    reps: setLogs.reps,
    durationSeconds: setLogs.durationSeconds,
    rpe: setLogs.rpe,
    workoutDate: workoutLogs.workoutDate,
  })
    .from(setLogs)
    .innerJoin(exerciseLogs, eq(setLogs.exerciseLogId, exerciseLogs.id))
    .innerJoin(workoutLogs, eq(exerciseLogs.workoutLogId, workoutLogs.id))
    .where(and(
      eq(workoutLogs.userId, user.id),
      eq(exerciseLogs.exerciseName, exercise.name),
    ))
    .orderBy(desc(workoutLogs.workoutDate), setLogs.setNumber)

  type SessionSet = {
    setNumber: number
    weightKg: number | null
    reps: number | null
    durationSeconds: number | null
    rpe: number | null
  }
  const sessionMap = new Map<string, SessionSet[]>()
  for (const s of sets) {
    const date = s.workoutDate
    if (!sessionMap.has(date)) sessionMap.set(date, [])
    sessionMap.get(date)!.push({
      setNumber: s.setNumber,
      weightKg: s.weightKg,
      reps: s.reps,
      durationSeconds: s.durationSeconds,
      rpe: s.rpe,
    })
  }
  const history = Array.from(sessionMap.entries()).map(([date, sessionSets]) => ({
    date,
    sets: sessionSets.sort((a, b) => a.setNumber - b.setNumber),
  }))

  let maxWeight = 0
  let maxReps = 0
  let maxVolume = 0
  let bestEstimatedOneRm = 0
  for (const s of sets) {
    const w = s.weightKg || 0
    const r = s.reps || 0
    if (w > maxWeight) maxWeight = w
    if (r > maxReps) maxReps = r
    const volume = w * r
    if (volume > maxVolume) maxVolume = volume
    if (w > 0 && r > 0) {
      const oneRm = w * (1 + r / 30)
      if (oneRm > bestEstimatedOneRm) bestEstimatedOneRm = oneRm
    }
  }

  return {
    exercise,
    media,
    history,
    stats: {
      totalSessions: sessionMap.size,
      totalSets: sets.length,
      maxWeight,
      maxReps,
      maxVolume,
      estimatedOneRm: Math.round(bestEstimatedOneRm * 10) / 10,
    },
  }
})
