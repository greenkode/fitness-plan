export type WorkoutDay = {
  date: string
  label: string
  movement: string
  focus: 'Strength' | 'Conditioning' | 'Recovery'
  sets: { weight: number; reps: string; rpe: number }[]
}

export const program: WorkoutDay[] = [
  {
    date: '2026-02-22',
    label: 'Leg Day',
    movement: 'Back Squat',
    focus: 'Strength',
    sets: [
      { weight: 140, reps: '5', rpe: 7 },
      { weight: 150, reps: '3', rpe: 8 },
      { weight: 160, reps: '2', rpe: 8.5 }
    ]
  },
  {
    date: '2026-02-23',
    label: 'Push Hypertrophy',
    movement: 'Bench Press',
    focus: 'Strength',
    sets: [
      { weight: 90, reps: '8', rpe: 7 },
      { weight: 95, reps: '6', rpe: 7.5 },
      { weight: 100, reps: '4', rpe: 8 }
    ]
  },
  {
    date: '2026-02-24',
    label: 'Conditioning Flow',
    movement: 'Row Intervals',
    focus: 'Conditioning',
    sets: [
      { weight: 0, reps: '4 x 3 min', rpe: 6 }
    ]
  },
  {
    date: '2026-02-25',
    label: 'Pull Power',
    movement: 'Deadlift',
    focus: 'Strength',
    sets: [
      { weight: 170, reps: '5', rpe: 7.5 },
      { weight: 185, reps: '3', rpe: 8 },
      { weight: 195, reps: '1', rpe: 9 }
    ]
  }
]
