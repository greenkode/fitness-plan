<template>
  <div>
    <div v-if="loading" class="loading-state">Loading...</div>

    <div v-else-if="!programs.length" class="empty-state">
      <h2 class="empty-title">Welcome to Krachtix</h2>
      <p class="empty-text">You don't have any programs yet. Create your first AI-powered workout program to get started.</p>
      <a href="/programs/new" class="create-btn">Create Your First Program</a>
    </div>

    <div v-else>
      <div class="section-header">
        <h2 class="section-title">My Programs</h2>
        <NuxtLink to="/programs/new" class="new-link">+ New</NuxtLink>
      </div>

      <div class="program-list">
        <div
          v-for="p in programs"
          :key="p.id"
          class="program-card"
          :class="{ active: p.status === 'active' }"
          @click="navigateTo('/programs')"
        >
          <div class="card-left">
            <span class="status-dot" :class="'dot-' + p.status"></span>
            <div>
              <h3 class="program-name">{{ p.templateName }}</h3>
              <div class="program-meta">
                <span v-if="p.currentPhase" class="meta-phase">{{ p.currentPhase }}</span>
                <span v-if="p.category" class="meta-cat">{{ p.category }}</span>
              </div>
            </div>
          </div>
          <div class="card-right">
            <span class="phase-count">{{ p.currentPhaseNumber || 1 }}/{{ p.totalPhases }}</span>
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="chevron"><polyline points="9 18 15 12 9 6" /></svg>
          </div>
        </div>
      </div>

      <div v-if="todayWorkout" class="today-section">
        <div class="today-label">Today's Workout</div>
        <div class="today-card" @click="navigateTo(`/workout/${todayDate}`)">
          <div class="today-info">
            <h3 class="today-title">{{ todayWorkout.title }}</h3>
            <span class="today-type" :class="'ttype-' + todayWorkout.workoutType">{{ todayWorkout.workoutType }}</span>
          </div>
          <span class="today-action">Start</span>
        </div>
      </div>

      <div v-if="upcomingWorkouts.length" class="upcoming-section">
        <div class="upcoming-label">This Week</div>
        <div class="upcoming-list">
          <div
            v-for="w in upcomingWorkouts"
            :key="w.date"
            class="upcoming-item"
            @click="navigateTo(`/workout/${w.date}`)"
          >
            <span class="upcoming-day">{{ formatDay(w.date) }}</span>
            <span class="upcoming-title">{{ w.title }}</span>
            <span class="upcoming-type" :class="'ttype-' + w.workoutType">{{ w.workoutType }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ middleware: 'auth' })

interface ProgramData {
  id: string
  templateName: string
  category: string | null
  status: string
  currentPhase: string | null
  currentPhaseNumber: number | null
  totalPhases: number
}

interface AssignmentData {
  date: string
  title: string
  workoutType: string
}

const todayWorkout = ref<AssignmentData | null>(null)
const upcomingWorkouts = ref<AssignmentData[]>([])

const todayDate = new Date().toISOString().split('T')[0]

const { data: programs, status } = await useFetch<ProgramData[]>('/api/fitness/programs', {
  default: () => [],
})

const { data: assignments } = await useFetch<AssignmentData[]>('/api/fitness/assignments/upcoming', {
  default: () => [],
})

watchEffect(() => {
  const all = assignments.value || []
  todayWorkout.value = all.find(a => a.date === todayDate) || null
  upcomingWorkouts.value = all.filter(a => a.date !== todayDate).slice(0, 6)
})

const loading = computed(() => status.value === 'pending')

function formatDay(dateStr: string): string {
  const d = new Date(dateStr + 'T00:00:00')
  return d.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' })
}
</script>

<style scoped>
.loading-state {
  text-align: center;
  padding: 4rem;
  color: var(--text-muted);
}
.empty-state {
  text-align: center;
  padding: 4rem 2rem;
}
.empty-title {
  font-family: 'Oswald', sans-serif;
  font-size: 2rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  background: linear-gradient(135deg, var(--accent-orange), var(--accent-yellow));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 0.75rem;
}
.empty-text {
  color: var(--text-secondary);
  font-size: 1.05rem;
  max-width: 450px;
  margin: 0 auto 2rem;
}
.create-btn {
  display: inline-block;
  background: linear-gradient(135deg, var(--accent-orange), #ff8c5a);
  color: #fff;
  font-family: 'Oswald', sans-serif;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-size: 1.1rem;
  padding: 1rem 2.5rem;
  border-radius: 12px;
  text-decoration: none;
  transition: all 0.2s;
}
.create-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(232, 93, 37, 0.4);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}
.section-title {
  font-family: 'Oswald', sans-serif;
  font-size: 1.25rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-primary);
  margin: 0;
}
.new-link {
  color: var(--accent-orange);
  text-decoration: none;
  font-weight: 600;
  font-size: 0.9rem;
}
.new-link:hover { text-decoration: underline; }

.program-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 2rem;
}
.program-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: 10px;
  padding: 1rem 1.25rem;
  cursor: pointer;
  transition: all 0.2s;
}
.program-card:hover {
  border-color: var(--accent-orange);
  transform: translateX(4px);
}
.program-card.active {
  border-left: 3px solid var(--accent-orange);
}
.card-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
.dot-active { background: var(--accent-orange); }
.dot-paused { background: var(--accent-yellow); }
.dot-completed { background: var(--accent-green); }
.program-name {
  font-family: 'Oswald', sans-serif;
  font-size: 1rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--text-primary);
  margin: 0;
}
.program-meta {
  display: flex;
  gap: 0.5rem;
  margin-top: 0.15rem;
}
.meta-phase {
  font-size: 0.75rem;
  color: var(--accent-orange);
}
.meta-cat {
  font-size: 0.75rem;
  color: var(--text-muted);
}
.card-right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.phase-count {
  font-family: 'Oswald', sans-serif;
  font-size: 0.85rem;
  color: var(--text-muted);
}
.chevron { color: var(--text-muted); }

.today-section {
  margin-bottom: 2rem;
}
.today-label, .upcoming-label {
  font-family: 'Oswald', sans-serif;
  font-size: 1rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--text-secondary);
  margin-bottom: 0.75rem;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.today-label::after, .upcoming-label::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--border-subtle);
}
.today-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, rgba(232, 93, 37, 0.08), rgba(212, 160, 18, 0.08));
  border: 2px solid var(--accent-orange);
  border-radius: 12px;
  padding: 1.25rem;
  cursor: pointer;
  transition: all 0.2s;
}
.today-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(232, 93, 37, 0.3);
}
.today-title {
  font-family: 'Oswald', sans-serif;
  font-size: 1.2rem;
  font-weight: 600;
  text-transform: uppercase;
  color: var(--text-primary);
  margin: 0 0 0.25rem;
}
.today-type, .upcoming-type {
  font-size: 0.65rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  font-weight: 600;
}
.ttype-gym { background: var(--accent-orange); color: #000; }
.ttype-cardio { background: var(--accent-green); color: #000; }
.ttype-recovery { background: var(--accent-purple); color: #fff; }
.today-action {
  font-family: 'Oswald', sans-serif;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--accent-orange);
  font-size: 1rem;
}

.upcoming-list {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}
.upcoming-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1rem;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.upcoming-item:hover {
  border-color: var(--accent-orange);
}
.upcoming-day {
  font-size: 0.8rem;
  color: var(--text-muted);
  width: 100px;
  flex-shrink: 0;
}
.upcoming-title {
  flex: 1;
  font-size: 0.9rem;
  color: var(--text-primary);
  font-weight: 500;
}
</style>
