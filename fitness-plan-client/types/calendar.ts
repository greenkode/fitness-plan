export interface WorkoutAssignment {
  id: string
  userProgramId: string
  assignedDate: string
  templateWorkoutId: string
  status: 'pending' | 'completed' | 'skipped' | 'rescheduled'
  notes: string | null
  workout: {
    title: string
    workoutType: string
    estimatedDuration: number | null
  }
}

export interface TemplateExercise {
  id: string
  name: string
  prescription: string | null
  sortOrder: number
  blockTitle: string
}
