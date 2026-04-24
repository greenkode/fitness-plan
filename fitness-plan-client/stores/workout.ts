import { defineStore } from 'pinia'

interface LoggedSet {
  weightKg: number
  reps: number
  rpe: number
}

interface WorkoutState {
  workoutLogId: string | null
  startedAt: string | null
  isActive: boolean
  lastSetCompletedAt: string | null
  restIntervals: number[]
}

export const useWorkoutStore = defineStore('workout', {
  state: (): WorkoutState => ({
    workoutLogId: null,
    startedAt: null,
    isActive: false,
    lastSetCompletedAt: null,
    restIntervals: [],
  }),

  actions: {
    async startWorkout(date: string) {
      const response = await $fetch<{ id: string; startedAt: string }>('/api/fitness/workout-logs', {
        method: 'POST',
        body: { workoutDate: date, exercises: [] },
      })
      this.workoutLogId = response.id
      this.startedAt = response.startedAt ?? new Date().toISOString()
      this.isActive = true
      this.lastSetCompletedAt = null
      this.restIntervals = []
    },

    trackRest() {
      const now = new Date().toISOString()
      if (this.lastSetCompletedAt) {
        const restSec = Math.floor((new Date(now).getTime() - new Date(this.lastSetCompletedAt).getTime()) / 1000)
        this.restIntervals.push(restSec)
      }
      this.lastSetCompletedAt = now
    },

    async finishWorkout() {
      if (!this.workoutLogId) return
      await $fetch(`/api/fitness/workout-logs/${this.workoutLogId}`, {
        method: 'PATCH',
        body: { completedAt: new Date().toISOString() },
      })
      this.isActive = false
    },

    resetWorkout() {
      this.workoutLogId = null
      this.startedAt = null
      this.isActive = false
      this.lastSetCompletedAt = null
      this.restIntervals = []
    },
  },
})
