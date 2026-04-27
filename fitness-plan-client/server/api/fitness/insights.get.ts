import { eq, desc } from 'drizzle-orm'
import { db } from '../../utils/db'
import { workoutLogs, exerciseLogs, setLogs } from '../../../database/schema/tracking'

export default defineEventHandler(async (event) => {
  const user = event.context.user
  if (!user) throw createError({ statusCode: 401, statusMessage: 'Unauthorized' })

  try {
    const workouts = await db.select({
      id: workoutLogs.id,
      workoutDate: workoutLogs.workoutDate,
      startedAt: workoutLogs.startedAt,
      completedAt: workoutLogs.completedAt,
    })
      .from(workoutLogs)
      .where(eq(workoutLogs.userId, user.id))
      .orderBy(desc(workoutLogs.workoutDate))
      .limit(60)

    if (workouts.length === 0) {
      return {
        totalWorkouts: 0,
        currentStreak: 0,
        longestStreak: 0,
        totalVolume: 0,
        weeklyVolume: [],
        topExercises: [],
        prs: [],
        avgDuration: 0,
      }
    }

    const completedWorkouts = workouts.filter(w => w.completedAt)

    const dates = new Set(completedWorkouts.map(w => w.workoutDate))
    const sortedDates = [...dates].sort().reverse()

    let currentStreak = 0
    let checkDate = new Date()
    checkDate.setHours(0, 0, 0, 0)
    while (true) {
      const dateStr = checkDate.toISOString().split('T')[0]
      if (dates.has(dateStr)) {
        currentStreak++
        checkDate.setDate(checkDate.getDate() - 1)
      } else {
        const yesterday = new Date(checkDate)
        yesterday.setDate(yesterday.getDate() - 1)
        if (currentStreak === 0 && dates.has(yesterday.toISOString().split('T')[0])) {
          checkDate = yesterday
          continue
        }
        break
      }
    }

    let longestStreak = 0
    let runningStreak = 0
    let lastDate: Date | null = null
    for (const dateStr of [...dates].sort()) {
      const d = new Date(dateStr)
      if (lastDate && (d.getTime() - lastDate.getTime()) <= 86400000 + 1000) {
        runningStreak++
      } else {
        runningStreak = 1
      }
      longestStreak = Math.max(longestStreak, runningStreak)
      lastDate = d
    }

    const exerciseStats = new Map<string, { sessions: number; totalVolume: number; maxWeight: number; maxReps: number; bestSet: { weightKg: number; reps: number; date: string } | null }>()
    let totalVolume = 0

    for (const w of workouts) {
      const exercises = await db.select({
        id: exerciseLogs.id,
        name: exerciseLogs.exerciseName,
      }).from(exerciseLogs).where(eq(exerciseLogs.workoutLogId, w.id))

      for (const ex of exercises) {
        const sets = await db.select({
          weight: setLogs.weightKg,
          reps: setLogs.reps,
        }).from(setLogs).where(eq(setLogs.exerciseLogId, ex.id))

        const stat = exerciseStats.get(ex.name) || { sessions: 0, totalVolume: 0, maxWeight: 0, maxReps: 0, bestSet: null }
        stat.sessions++

        for (const s of sets) {
          const w_ = s.weight || 0
          const r = s.reps || 0
          const vol = w_ * r
          stat.totalVolume += vol
          totalVolume += vol
          if (w_ > stat.maxWeight) {
            stat.maxWeight = w_
            stat.bestSet = { weightKg: w_, reps: r, date: w.workoutDate }
          }
          if (r > stat.maxReps) stat.maxReps = r
        }

        exerciseStats.set(ex.name, stat)
      }
    }

    const topExercises = [...exerciseStats.entries()]
      .map(([name, s]) => ({ name, sessions: s.sessions, totalVolume: s.totalVolume }))
      .sort((a, b) => b.sessions - a.sessions)
      .slice(0, 5)

    const prs = [...exerciseStats.entries()]
      .filter(([, s]) => s.bestSet && s.bestSet.weightKg > 0)
      .map(([name, s]) => ({ name, weightKg: s.bestSet!.weightKg, reps: s.bestSet!.reps, date: s.bestSet!.date }))
      .sort((a, b) => b.weightKg - a.weightKg)
      .slice(0, 5)

    const weekMap = new Map<string, number>()
    for (const w of workouts) {
      const d = new Date(w.workoutDate)
      const weekStart = new Date(d)
      weekStart.setDate(d.getDate() - d.getDay())
      const weekKey = weekStart.toISOString().split('T')[0]
      const weekVol = weekMap.get(weekKey) || 0

      const exs = await db.select({ id: exerciseLogs.id }).from(exerciseLogs).where(eq(exerciseLogs.workoutLogId, w.id))
      let workoutVol = 0
      for (const ex of exs) {
        const sets = await db.select({ weight: setLogs.weightKg, reps: setLogs.reps }).from(setLogs).where(eq(setLogs.exerciseLogId, ex.id))
        for (const s of sets) workoutVol += (s.weight || 0) * (s.reps || 0)
      }
      weekMap.set(weekKey, weekVol + workoutVol)
    }

    const weeklyVolume = [...weekMap.entries()]
      .sort(([a], [b]) => a.localeCompare(b))
      .slice(-8)
      .map(([week, volume]) => ({ week, volume }))

    let totalDuration = 0
    let durationCount = 0
    for (const w of completedWorkouts) {
      if (w.startedAt && w.completedAt) {
        totalDuration += new Date(w.completedAt).getTime() - new Date(w.startedAt).getTime()
        durationCount++
      }
    }
    const avgDuration = durationCount > 0 ? Math.round(totalDuration / durationCount / 60000) : 0

    return {
      totalWorkouts: completedWorkouts.length,
      currentStreak,
      longestStreak,
      totalVolume: Math.round(totalVolume),
      weeklyVolume,
      topExercises,
      prs,
      avgDuration,
    }
  } catch (err: any) {
    console.error('Insights error:', err.message)
    throw createError({ statusCode: 500, statusMessage: 'Failed to compute insights' })
  }
})
