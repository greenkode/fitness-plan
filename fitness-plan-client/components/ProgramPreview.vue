<template>
  <div class="program-preview">
    <div class="program-header">
      <h3 class="program-name">{{ program.name }}</h3>
      <p v-if="program.description" class="program-desc">{{ program.description }}</p>
      <div class="program-badges">
        <span v-if="program.category" class="badge badge-category">{{ program.category }}</span>
        <span v-if="program.difficultyLevel" class="badge badge-difficulty">{{ program.difficultyLevel }}</span>
      </div>
    </div>

    <div v-if="program.schedule" class="schedule-bar">
      <div
        v-for="day in sortedSchedule"
        :key="day.dayOfWeek"
        class="schedule-day"
        :class="'type-' + day.trainingType"
      >
        <span class="day-abbr">{{ dayNames[day.dayOfWeek] }}</span>
        <span class="day-type-label">{{ day.trainingType }}</span>
      </div>
    </div>

    <div v-for="phase in program.phases" :key="phase.phaseNumber" class="phase-section">
      <button class="phase-toggle" @click="togglePhase(phase.phaseNumber)">
        <div class="phase-info">
          <span class="phase-number">Phase {{ phase.phaseNumber }}</span>
          <span class="phase-name">{{ phase.name }}</span>
          <span v-if="phase.theme" class="phase-theme">{{ phase.theme }}</span>
        </div>
        <svg
          class="phase-chevron"
          :class="{ open: openPhases.has(phase.phaseNumber) }"
          xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24"
          fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
        ><polyline points="6 9 12 15 18 9" /></svg>
      </button>

      <div v-show="openPhases.has(phase.phaseNumber)" class="phase-content">
        <p v-if="phase.description" class="phase-desc">{{ phase.description }}</p>

        <div v-for="(workout, wi) in phase.workouts" :key="wi" class="workout-section">
          <button class="workout-toggle" @click="toggleWorkout(phase.phaseNumber + '-' + wi)">
            <span class="workout-title">{{ workout.title }}</span>
            <span class="workout-meta-info">
              <span v-if="workout.estimatedDuration" class="workout-dur">{{ workout.estimatedDuration }}min</span>
              <span class="workout-type-badge" :class="'wtype-' + workout.workoutType">{{ workout.workoutType }}</span>
            </span>
          </button>

          <div v-show="openWorkouts.has(phase.phaseNumber + '-' + wi)" class="workout-content">
            <div v-for="block in workout.blocks" :key="block.blockKey" class="block-section">
              <span class="block-label">{{ block.title }}</span>
              <div v-for="ex in block.exercises" :key="ex.name" class="exercise-row">
                <span class="ex-name">{{ ex.name }}</span>
                <span class="ex-rx">{{ ex.prescription }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Exercise { name: string; prescription: string; sortOrder: number }
interface Block { blockKey: string; title: string; sortOrder: number; exercises: Exercise[] }
interface Workout { workoutType: string; title: string; estimatedDuration: number; sortOrder: number; blocks: Block[] }
interface Phase { phaseNumber: number; name: string; theme?: string; description?: string; workouts: Workout[] }
interface Schedule { dayOfWeek: number; trainingType: string }
interface Program {
  name: string; description?: string; category?: string; difficultyLevel?: string
  schedule?: Schedule[]; phases: Phase[]
}

const props = defineProps<{ program: Program }>()

const dayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
const openPhases = ref(new Set([1]))
const openWorkouts = ref(new Set<string>())

const sortedSchedule = computed(() =>
  [...(props.program.schedule || [])].sort((a, b) => a.dayOfWeek - b.dayOfWeek))

function togglePhase(n: number) {
  if (openPhases.value.has(n)) openPhases.value.delete(n)
  else openPhases.value.add(n)
}

function toggleWorkout(key: string) {
  if (openWorkouts.value.has(key)) openWorkouts.value.delete(key)
  else openWorkouts.value.add(key)
}
</script>

<style scoped>
.program-preview {
  border: 1px solid var(--border-subtle);
  border-radius: 10px;
  overflow: hidden;
  margin-top: 0.5rem;
}
.program-header {
  padding: 1rem;
  border-bottom: 1px solid var(--border-subtle);
  background: linear-gradient(135deg, rgba(232, 93, 37, 0.06), transparent);
}
.program-name {
  font-family: 'Oswald', sans-serif;
  font-size: 1.15rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--text-primary);
  margin: 0 0 0.25rem;
}
.program-desc {
  font-size: 0.85rem;
  color: var(--text-secondary);
  margin: 0 0 0.5rem;
}
.program-badges {
  display: flex;
  gap: 0.4rem;
}
.badge {
  font-size: 0.6rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  font-weight: 600;
}
.badge-category { background: var(--accent-orange); color: #000; }
.badge-difficulty { background: var(--accent-purple); color: #fff; }

.schedule-bar {
  display: flex;
  border-bottom: 1px solid var(--border-subtle);
}
.schedule-day {
  flex: 1;
  text-align: center;
  padding: 0.5rem 0.25rem;
  border-right: 1px solid var(--border-subtle);
}
.schedule-day:last-child { border-right: none; }
.day-abbr {
  display: block;
  font-size: 0.6rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-muted);
  margin-bottom: 0.15rem;
}
.day-type-label {
  display: block;
  font-size: 0.6rem;
  font-weight: 600;
  text-transform: uppercase;
}
.type-gym .day-type-label { color: var(--accent-orange); }
.type-cardio .day-type-label { color: var(--accent-green); }
.type-recovery .day-type-label { color: var(--accent-purple); }
.type-rest .day-type-label { color: var(--text-muted); }

.phase-section {
  border-bottom: 1px solid var(--border-subtle);
}
.phase-section:last-child { border-bottom: none; }
.phase-toggle {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  background: none;
  border: none;
  cursor: pointer;
  text-align: left;
  color: var(--text-primary);
  transition: background 0.15s;
}
.phase-toggle:hover { background: var(--section-bg); }
.phase-number {
  font-family: 'Oswald', sans-serif;
  font-size: 0.65rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--accent-orange);
  margin-right: 0.5rem;
}
.phase-name {
  font-family: 'Oswald', sans-serif;
  font-size: 0.95rem;
  font-weight: 600;
}
.phase-theme {
  font-size: 0.75rem;
  color: var(--text-muted);
  margin-left: 0.5rem;
}
.phase-chevron {
  color: var(--text-muted);
  transition: transform 0.2s;
  flex-shrink: 0;
}
.phase-chevron.open { transform: rotate(180deg); }
.phase-content { padding: 0 1rem 0.75rem; }
.phase-desc {
  font-size: 0.8rem;
  color: var(--text-secondary);
  margin: 0 0 0.75rem;
}

.workout-section {
  border: 1px solid var(--border-subtle);
  border-radius: 8px;
  margin-bottom: 0.5rem;
  overflow: hidden;
}
.workout-toggle {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.6rem 0.75rem;
  background: var(--section-bg);
  border: none;
  cursor: pointer;
  text-align: left;
  color: var(--text-primary);
}
.workout-title {
  font-family: 'Oswald', sans-serif;
  font-size: 0.85rem;
  font-weight: 500;
}
.workout-meta-info {
  display: flex;
  gap: 0.4rem;
  align-items: center;
}
.workout-dur {
  font-size: 0.7rem;
  color: var(--text-muted);
}
.workout-type-badge {
  font-size: 0.55rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 0.15rem 0.4rem;
  border-radius: 3px;
  font-weight: 600;
}
.wtype-gym { background: var(--accent-orange); color: #000; }
.wtype-cardio { background: var(--accent-green); color: #000; }
.wtype-recovery { background: var(--accent-purple); color: #fff; }
.workout-content { padding: 0.5rem 0.75rem; }

.block-section {
  margin-bottom: 0.5rem;
}
.block-section:last-child { margin-bottom: 0; }
.block-label {
  display: block;
  font-size: 0.6rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--accent-orange);
  font-weight: 600;
  margin-bottom: 0.3rem;
}
.exercise-row {
  display: flex;
  justify-content: space-between;
  padding: 0.25rem 0;
  font-size: 0.8rem;
  border-bottom: 1px solid var(--border-subtle);
}
.exercise-row:last-child { border-bottom: none; }
.ex-name { color: var(--text-primary); }
.ex-rx { color: var(--text-muted); font-size: 0.75rem; }
</style>
