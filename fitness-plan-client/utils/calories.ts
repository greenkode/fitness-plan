import type { ExerciseCategory } from '~/types/workout'

const MET_VALUES: Record<ExerciseCategory, number> = {
  compound_heavy: 6.0,
  compound_moderate: 5.0,
  isolation: 3.5,
  bodyweight: 4.0,
  cardio_high: 8.0,
  cardio_moderate: 5.5,
  recovery: 2.5,
}

const EXERCISE_PATTERNS: [string, ExerciseCategory][] = [
  ['squat', 'compound_heavy'],
  ['deadlift', 'compound_heavy'],
  ['bench press', 'compound_heavy'],
  ['overhead press', 'compound_moderate'],
  ['row', 'compound_moderate'],
  ['lunge', 'compound_moderate'],
  ['leg press', 'compound_moderate'],
  ['pull-up', 'bodyweight'],
  ['push-up', 'bodyweight'],
  ['dip', 'bodyweight'],
  ['plank', 'bodyweight'],
  ['curl', 'isolation'],
  ['extension', 'isolation'],
  ['lateral raise', 'isolation'],
  ['fly', 'isolation'],
  ['calf raise', 'isolation'],
  ['shrug', 'isolation'],
  ['sprint', 'cardio_high'],
  ['hiit', 'cardio_high'],
  ['interval', 'cardio_high'],
  ['run', 'cardio_moderate'],
  ['walk', 'cardio_moderate'],
  ['cycle', 'cardio_moderate'],
  ['swim', 'cardio_moderate'],
  ['stretch', 'recovery'],
  ['yoga', 'recovery'],
  ['foam roll', 'recovery'],
]

export function categorizeExercise(name: string): ExerciseCategory {
  const lower = name.toLowerCase()
  for (const [pattern, category] of EXERCISE_PATTERNS) {
    if (lower.includes(pattern)) return category
  }
  return 'compound_moderate'
}

export function estimateCalories(params: {
  exercises: { name: string; sets: { weightKg: number | null; reps: number | null }[] }[]
  bodyWeightKg: number
  totalDurationMinutes: number
}): number {
  if (params.totalDurationMinutes <= 0) return 0

  const categories = params.exercises.map(ex => categorizeExercise(ex.name))
  const avgMET = categories.length > 0
    ? categories.reduce((sum, cat) => sum + MET_VALUES[cat], 0) / categories.length
    : 4.0

  const baseCalories = avgMET * params.bodyWeightKg * (params.totalDurationMinutes / 60)

  const totalVolume = params.exercises.reduce((sum, ex) =>
    sum + ex.sets.reduce((s, set) =>
      s + ((set.weightKg || 0) * (set.reps || 0)), 0), 0)
  const volumeBonus = totalVolume / 500

  return Math.round(baseCalories + volumeBonus)
}
