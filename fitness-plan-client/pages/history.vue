<template>
  <div class="history-page">
    <h2 class="page-title">Workout History</h2>

    <div v-if="insights && insights.totalWorkouts > 0" class="insights-grid">
      <div class="insight-card">
        <span class="insight-value">{{ insights.totalWorkouts }}</span>
        <span class="insight-label">Total Workouts</span>
      </div>
      <div class="insight-card highlight">
        <span class="insight-value">{{ insights.currentStreak }}</span>
        <span class="insight-label">Day Streak</span>
      </div>
      <div class="insight-card">
        <span class="insight-value">{{ insights.longestStreak }}</span>
        <span class="insight-label">Longest Streak</span>
      </div>
      <div class="insight-card">
        <span class="insight-value">{{ formatVolume(insights.totalVolume) }}</span>
        <span class="insight-label">Total Volume</span>
      </div>
      <div class="insight-card">
        <span class="insight-value">{{ insights.avgDuration }}<small>min</small></span>
        <span class="insight-label">Avg Duration</span>
      </div>
    </div>

    <div v-if="insights && insights.weeklyVolume.length > 0" class="chart-section">
      <h3 class="chart-title">Weekly Volume</h3>
      <div class="chart">
        <div
          v-for="w in insights.weeklyVolume"
          :key="w.week"
          class="chart-bar"
          :style="{ height: `${(w.volume / maxWeeklyVolume) * 100}%` }"
          :title="`Week of ${w.week}: ${w.volume.toLocaleString()}kg`"
        >
          <span class="bar-value">{{ formatVolume(w.volume) }}</span>
        </div>
      </div>
    </div>

    <div v-if="insights && insights.prs.length > 0" class="prs-section">
      <h3 class="section-heading">Personal Records</h3>
      <div class="pr-list">
        <div v-for="pr in insights.prs" :key="pr.name" class="pr-card">
          <div class="pr-name">{{ pr.name }}</div>
          <div class="pr-detail">{{ pr.weightKg }}kg × {{ pr.reps }} <span class="pr-date">{{ formatDate(pr.date) }}</span></div>
        </div>
      </div>
    </div>

    <div v-if="insights && insights.topExercises.length > 0" class="top-section">
      <h3 class="section-heading">Most Frequent Exercises</h3>
      <div class="top-list">
        <div v-for="ex in insights.topExercises" :key="ex.name" class="top-item">
          <span class="top-name">{{ ex.name }}</span>
          <span class="top-stats">{{ ex.sessions }} sessions · {{ formatVolume(ex.totalVolume) }} volume</span>
        </div>
      </div>
    </div>

    <h3 class="section-heading">All Workouts</h3>

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

interface Insights {
  totalWorkouts: number
  currentStreak: number
  longestStreak: number
  totalVolume: number
  weeklyVolume: { week: string; volume: number }[]
  topExercises: { name: string; sessions: number; totalVolume: number }[]
  prs: { name: string; weightKg: number; reps: number; date: string }[]
  avgDuration: number
}

const workouts = ref<WorkoutData[]>([])
const insights = ref<Insights | null>(null)
const loading = ref(true)

const maxWeeklyVolume = computed(() => {
  if (!insights.value?.weeklyVolume.length) return 1
  return Math.max(...insights.value.weeklyVolume.map(w => w.volume), 1)
})

function formatVolume(v: number): string {
  if (v >= 1000) return (v / 1000).toFixed(1) + 'k'
  return String(Math.round(v))
}

onMounted(async () => {
  try {
    const [workoutData, insightsData] = await Promise.all([
      $fetch<WorkoutData[]>('/api/fitness/workout-logs/history').catch(() => []),
      $fetch<Insights>('/api/fitness/insights').catch(() => null),
    ])
    workouts.value = workoutData
    insights.value = insightsData
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
.insights-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(110px, 1fr));
  gap: 0.75rem;
  margin-bottom: 1.5rem;
}
.insight-card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: 10px;
  padding: 0.85rem 1rem;
  text-align: center;
}
.insight-card.highlight {
  border-color: var(--accent-orange);
  background: linear-gradient(135deg, rgba(232, 93, 37, 0.08), rgba(212, 160, 18, 0.06));
}
.insight-value {
  display: block;
  font-family: 'Oswald', sans-serif;
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--accent-orange);
  line-height: 1.1;
}
.insight-value small {
  font-size: 0.65rem;
  font-weight: 400;
  color: var(--text-muted);
  margin-left: 0.15rem;
}
.insight-label {
  display: block;
  font-size: 0.65rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--text-muted);
  margin-top: 0.2rem;
}
.chart-section {
  margin-bottom: 2rem;
}
.chart-title {
  font-family: 'Oswald', sans-serif;
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.15em;
  color: var(--text-secondary);
  margin: 0 0 0.75rem;
}
.chart {
  display: flex;
  align-items: flex-end;
  gap: 0.4rem;
  height: 120px;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: 10px;
  padding: 0.75rem;
}
.chart-bar {
  flex: 1;
  min-height: 8px;
  background: linear-gradient(to top, var(--accent-orange), var(--accent-yellow));
  border-radius: 4px 4px 0 0;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 4px;
  position: relative;
  cursor: default;
  transition: filter 0.2s;
}
.chart-bar:hover { filter: brightness(1.15); }
.bar-value {
  font-size: 0.6rem;
  color: #fff;
  font-weight: 600;
  font-family: 'Oswald', sans-serif;
  white-space: nowrap;
}
.section-heading {
  font-family: 'Oswald', sans-serif;
  font-size: 1rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--text-primary);
  margin: 1.5rem 0 0.75rem;
}
.prs-section, .top-section { margin-bottom: 1.5rem; }
.pr-list, .top-list {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}
.pr-card, .top-item {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: 8px;
  padding: 0.6rem 0.85rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.pr-name, .top-name {
  font-size: 0.9rem;
  color: var(--text-primary);
  font-weight: 500;
}
.pr-detail {
  font-family: 'Oswald', sans-serif;
  font-size: 0.95rem;
  color: var(--accent-orange);
  font-weight: 600;
}
.pr-date {
  font-size: 0.7rem;
  color: var(--text-muted);
  margin-left: 0.5rem;
  font-weight: 400;
}
.top-stats {
  font-size: 0.75rem;
  color: var(--text-muted);
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
