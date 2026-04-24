export interface SetLog {
  id: string | null
  setNumber: number
  weightKg: number | null
  reps: number | null
  rpe: number | null
  completed: boolean
  completedAt: Date | null
  notes: string | null
}

export interface ExerciseLog {
  id: string | null
  templateExerciseId: string | null
  exerciseName: string
  sortOrder: number
  sets: SetLog[]
}

export interface WorkoutLog {
  id: string
  userId: string
  assignmentId: string | null
  workoutDate: string
  startedAt: string | null
  completedAt: string | null
  notes: string | null
  overallRpe: number | null
  exercises: ExerciseLog[]
}

export interface SessionFeedback {
  workoutLogId: string
  feedbackDate: string
  fatigueLevel: number | null
  sorenessLevel: number | null
  motivationLevel: number | null
  sleepQuality: number | null
  stressLevel: number | null
  freeText: string | null
}

export interface WorkoutSummaryData {
  workoutLogId: string
  totalDurationMinutes: number
  setsCompleted: number
  totalSets: number
  totalVolumeKg: number
  averageRpe: number | null
  estimatedCalories: number
  restIntervals: number[]
}

export type ExerciseCategory =
  | 'compound_heavy'
  | 'compound_moderate'
  | 'isolation'
  | 'bodyweight'
  | 'cardio_high'
  | 'cardio_moderate'
  | 'recovery'
