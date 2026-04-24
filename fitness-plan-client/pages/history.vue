<template>
  <div class="history-page">
    <h2 class="page-title">Workout History</h2>

    <div v-if="loading" class="loading">Loading...</div>
    <div v-else-if="!workouts.length" class="empty">No workouts logged yet.</div>

    <div v-else class="workout-list">
      <div
        v-for="w in workouts"
        :key="w.id"
        class="workout-card"
        @click="openWorkout(w)"
      >
        <div class="workout-header">
          <div class="workout-info">
            <span class="workout-date-label">{{ formatDate(w.workoutDate) }}</span>
            <div class="workout-meta">
              <span v-if="w.durationMinutes" class="meta-item">{{ w.durationMinutes }}min</span>
              <span class="meta-item">{{ w.totalSets }} sets</span>
              <span v-if="w.totalVolume > 0" class="meta-item">{{ w.totalVolume.toLocaleString() }}kg</span>
            </div>
          </div>
          <div class="workout-status-row">
            <span v-if="w.completedAt" class="status-complete">Completed</span>
            <span v-else class="status-active">In Progress</span>
            <div v-if="!w.completedAt" class="action-btns" @click.stop>
              <button class="action-btn complete-btn" @click="completeWorkout(w)">Complete</button>
              <button class="action-btn delete-btn" @click="deleteWorkout(w)">Delete</button>
            </div>
            <svg v-else xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="chevron-icon"><polyline points="9 18 15 12 9 6" /></svg>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ middleware: 'auth' })

const toast = useToast()

interface WorkoutData {
  id: string
  workoutDate: string
  startedAt: string | null
  completedAt: string | null
  durationMinutes: number | null
  totalSets: number
  totalVolume: number
}

const workouts = ref<WorkoutData[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    workouts.value = await $fetch<WorkoutData[]>('/api/fitness/workout-logs/history')
  } catch {
    workouts.value = []
  } finally {
    loading.value = false
  }
})

function openWorkout(w: WorkoutData) {
  navigateTo(`/workout/${w.workoutDate}`)
}

async function completeWorkout(w: WorkoutData) {
  try {
    await $fetch(`/api/fitness/workout-logs/${w.id}`, {
      method: 'PATCH',
      body: { completedAt: w.startedAt ? new Date(new Date(w.startedAt).getTime() + 3600000).toISOString() : new Date().toISOString() },
    })
    w.completedAt = new Date().toISOString()
    toast.add({ title: 'Workout marked complete', color: 'success' })
  } catch {
    toast.add({ title: 'Failed to update', color: 'error' })
  }
}

async function deleteWorkout(w: WorkoutData) {
  try {
    await $fetch(`/api/fitness/workout-logs/${w.id}`, { method: 'DELETE' })
    workouts.value = workouts.value.filter(x => x.id !== w.id)
    toast.add({ title: 'Workout deleted', color: 'neutral' })
  } catch {
    toast.add({ title: 'Failed to delete', color: 'error' })
  }
}

function formatDate(dateStr: string): string {
  const d = new Date(dateStr + 'T00:00:00')
  return d.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' })
}
</script>

<style scoped>
.history-page {
  max-width: 700px;
  margin: 0 auto;
}
.page-title {
  font-family: 'Oswald', sans-serif;
  font-size: 1.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  background: linear-gradient(135deg, var(--accent-orange), var(--accent-yellow));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 1.5rem;
}
.loading, .empty {
  text-align: center;
  padding: 3rem;
  color: var(--text-muted);
}
.workout-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.workout-card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.workout-card:hover {
  border-color: var(--accent-orange);
  transform: translateX(4px);
}
.workout-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.25rem;
  gap: 1rem;
}
.workout-date-label {
  font-family: 'Oswald', sans-serif;
  font-size: 1.05rem;
  font-weight: 600;
  color: var(--text-primary);
  display: block;
}
.workout-meta {
  display: flex;
  gap: 1rem;
  margin-top: 0.2rem;
}
.meta-item {
  font-size: 0.8rem;
  color: var(--text-muted);
}
.workout-status-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
}
.status-complete {
  font-size: 0.65rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--accent-green);
  font-weight: 600;
}
.status-active {
  font-size: 0.65rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--accent-yellow);
  font-weight: 600;
}
.chevron-icon {
  color: var(--text-muted);
}
.action-btns {
  display: flex;
  gap: 0.4rem;
}
.action-btn {
  font-size: 0.7rem;
  font-family: 'Oswald', sans-serif;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  padding: 0.35rem 0.75rem;
  border-radius: 6px;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}
.complete-btn {
  background: var(--accent-green);
  color: #000;
}
.complete-btn:hover {
  filter: brightness(1.1);
}
.delete-btn {
  background: none;
  border: 1px solid #ef4444;
  color: #ef4444;
}
.delete-btn:hover {
  background: rgba(239, 68, 68, 0.1);
}
</style>
