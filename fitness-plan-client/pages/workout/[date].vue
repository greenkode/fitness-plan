<template>
  <div class="workout-page">
    <div v-if="loading" class="loading-state">
      <p>Loading workout...</p>
    </div>
    <div v-else-if="noAssignment" class="preview-state">
      <h2 class="workout-heading">No Workout Today</h2>
      <p class="workout-date">{{ formattedDate }}</p>
      <p class="empty-msg">There's no workout scheduled for this day. Either it's a rest day, or you haven't created a program yet.</p>
      <NuxtLink to="/programs" class="start-btn">View Programs</NuxtLink>
      <NuxtLink to="/" class="back-link">Back to Calendar</NuxtLink>
    </div>
    <div v-else-if="step === 'preview'" class="preview-state">
      <h2 class="workout-heading">{{ workoutTitle }}</h2>
      <p class="workout-date">{{ formattedDate }}</p>
      <div class="exercise-list">
        <template v-for="(ex, i) in templateExercises" :key="ex.id">
          <div v-if="i === 0 || ex.blockTitle !== templateExercises[i - 1].blockTitle" class="block-header">
            {{ ex.blockTitle || 'Main' }}
          </div>
          <div class="exercise-item">
            <div class="exercise-row">
              <span class="ex-name">{{ ex.name }}</span>
              <span class="ex-rx">{{ ex.prescription }}</span>
            </div>
            <ExerciseMedia
              :exercise-id="ex.id"
              :initial-media="ex.media"
              :can-edit="true"
              @updated="(m) => ex.media = m"
            />
          </div>
        </template>
      </div>
      <button class="start-btn" @click="start">Start Workout</button>
      <NuxtLink to="/" class="back-link">Back to Calendar</NuxtLink>
    </div>

    <div v-else-if="step === 'active'" class="active-state">
      <div class="sticky-header">
        <NuxtLink to="/" class="header-back">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6" /></svg>
          Calendar
        </NuxtLink>
        <WorkoutTimer :started-at="store.startedAt" />
        <WorkoutProgress :completed="totalCompletedSets" :total="totalPrescribedSets" />
      </div>

      <div class="exercise-cards">
        <div
          v-for="(ex, i) in templateExercises"
          :key="ex.name"
          class="exercise-card"
          :class="{ done: exerciseSets(ex.name).length >= prescribedSetCount(ex.prescription) }"
          @click="openExercise(i)"
        >
          <div class="ex-card-left">
            <span class="ex-card-name">{{ ex.name }}</span>
            <span class="ex-card-rx">{{ ex.prescription }}</span>
          </div>
          <div class="ex-card-right">
            <span class="ex-card-count">{{ exerciseSets(ex.name).length }}/{{ prescribedSetCount(ex.prescription) }}</span>
            <svg v-if="exerciseSets(ex.name).length >= prescribedSetCount(ex.prescription)" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" class="check-icon">
              <polyline points="20 6 9 17 4 12" />
            </svg>
            <svg v-else xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="chevron-icon">
              <polyline points="9 18 15 12 9 6" />
            </svg>
          </div>
        </div>
      </div>

      <div class="workout-actions">
        <button class="finish-btn" :disabled="totalCompletedSets === 0" @click="finish">
          Finish & Save Workout
        </button>
        <button class="discard-btn" @click="showDiscardConfirm = true">
          Discard Workout
        </button>
        <button class="secondary-btn" @click="logout">Logout</button>
      </div>

      <div v-if="showDiscardConfirm" class="modal-overlay" @click.self="showDiscardConfirm = false">
        <div class="confirm-modal">
          <h3 class="confirm-title">Discard Workout?</h3>
          <p class="confirm-text">This will delete all logged sets and the workout record. This cannot be undone.</p>
          <div class="confirm-actions">
            <button class="discard-confirm-btn" @click="discardWorkout">Yes, Discard</button>
            <button class="cancel-btn" @click="showDiscardConfirm = false">Keep Going</button>
          </div>
        </div>
      </div>

      <SetLogModal
        :open="modalOpen"
        :exercise-id="activeExercise?.id || ''"
        :exercise-name="activeExercise?.name || ''"
        :prescription="activeExercise?.prescription || ''"
        :existing-sets="activeExerciseSets"
        :media="activeExercise?.media || []"
        @close="modalOpen = false"
        @set-logged="onSetLogged"
      />
    </div>

    <div v-else-if="step === 'feedback'">
      <FeedbackForm
        :workout-log-id="store.workoutLogId!"
        :workout-date="date"
        @submitted="goToSummary"
      />
    </div>

    <div v-else-if="step === 'summary'">
      <WorkoutSummary
        :duration-minutes="durationMinutes"
        :sets-completed="totalCompletedSets"
        :total-sets="totalPrescribedSets"
        :total-volume="totalVolume"
        :estimated-calories="calories"
        :average-rpe="avgRpe"
        :avg-rest-seconds="avgRest"
        @back="goHome"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { estimateCalories } from '~/utils/calories'
import { useWorkoutStore } from '~/stores/workout'

definePageMeta({ middleware: 'auth' })

const route = useRoute()
const date = route.params.date as string
const store = useWorkoutStore()
const { clear } = useUserSession()
const toast = useToast()

const step = ref<'preview' | 'active' | 'feedback' | 'summary'>('preview')
const modalOpen = ref(false)
const activeExerciseIndex = ref(0)
const showDiscardConfirm = ref(false)
const loading = ref(true)

const workoutTitle = ref('Workout')
const workoutType = ref<'gym' | 'cardio' | 'recovery' | 'rest'>('gym')
const noAssignment = ref(false)

interface MediaItem {
  id: string
  url: string
  mediaType: string
  label: string | null
  sortOrder: number
}

interface TemplateExercise {
  id: string
  name: string
  prescription: string
  blockKey?: string
  blockTitle?: string
  media: MediaItem[]
}

interface LoggedSet {
  weightKg: number
  reps: number
  rpe: number
}

const templateExercises = ref<TemplateExercise[]>([])

const loggedSets = ref<Map<string, LoggedSet[]>>(new Map())

async function loadAssignment() {
  try {
    const assignment = await $fetch<any>(`/api/fitness/assignments/${date}`)
    if (!assignment) {
      noAssignment.value = true
      return
    }

    workoutTitle.value = assignment.title
    workoutType.value = assignment.workoutType

    const exercises: TemplateExercise[] = []
    for (const block of assignment.blocks || []) {
      for (const ex of block.exercises || []) {
        exercises.push({
          id: ex.id,
          name: ex.name,
          prescription: ex.prescription,
          media: ex.media || [],
          blockKey: block.blockKey,
          blockTitle: block.title,
        })
      }
    }
    templateExercises.value = exercises
  } catch {
    noAssignment.value = true
  }
}

async function hydrateFromServer() {
  await loadAssignment()

  try {
    const active = await $fetch<any>(`/api/fitness/workout-logs/active`, {
      query: { date },
    })

    if (active) {
      store.workoutLogId = active.id
      store.startedAt = active.startedAt
      store.isActive = true

      for (const ex of active.exerciseLogs || []) {
        const sets: LoggedSet[] = (ex.sets || [])
          .filter((s: any) => s.completed)
          .map((s: any) => ({
            weightKg: s.weightKg || 0,
            reps: s.reps || 0,
            rpe: s.rpe || 7,
          }))
        if (sets.length > 0) {
          loggedSets.value.set(ex.exerciseName, sets)
        }
      }

      step.value = 'active'
    }
  } catch {
    // no active workout found, stay on preview
  } finally {
    loading.value = false
  }
}

onMounted(() => hydrateFromServer())

const formattedDate = computed(() => {
  const d = new Date(date + 'T00:00:00')
  return d.toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' })
})

function prescribedSetCount(prescription: string): number {
  const match = prescription.match(/(\d+)\s*[×x]/i)
  return match ? parseInt(match[1]) : 3
}

function exerciseSets(exerciseName: string): LoggedSet[] {
  return loggedSets.value.get(exerciseName) || []
}

const activeExercise = computed(() => templateExercises.value[activeExerciseIndex.value])

const activeExerciseSets = computed(() => {
  const ex = activeExercise.value
  return ex ? exerciseSets(ex.name) : []
})

const totalCompletedSets = computed(() => {
  let total = 0
  loggedSets.value.forEach(sets => total += sets.length)
  return total
})

const totalPrescribedSets = computed(() =>
  templateExercises.value.reduce((sum, ex) => sum + prescribedSetCount(ex.prescription), 0))

const totalVolume = computed(() => {
  let vol = 0
  loggedSets.value.forEach(sets => {
    for (const s of sets) vol += s.weightKg * s.reps
  })
  return vol
})

const avgRpe = computed(() => {
  const all: number[] = []
  loggedSets.value.forEach(sets => {
    for (const s of sets) if (s.rpe) all.push(s.rpe)
  })
  return all.length > 0 ? Math.round((all.reduce((a, b) => a + b, 0) / all.length) * 10) / 10 : null
})

async function start() {
  try {
    await store.startWorkout(date)
    step.value = 'active'
  } catch (e: any) {
    toast.add({ title: 'Failed to start workout', description: e.data?.statusMessage || 'Something went wrong.', color: 'error' })
  }
}

function openExercise(index: number) {
  activeExerciseIndex.value = index
  modalOpen.value = true
}

async function onSetLogged(set: LoggedSet) {
  const ex = activeExercise.value
  if (!ex) return

  if (!loggedSets.value.has(ex.name)) {
    loggedSets.value.set(ex.name, [])
  }
  loggedSets.value.get(ex.name)!.push(set)

  store.trackRest()

  if (store.workoutLogId) {
    try {
      await $fetch(`/api/fitness/workout-logs/${store.workoutLogId}/sets`, {
        method: 'POST',
        body: {
          exerciseName: ex.name,
          sortOrder: activeExerciseIndex.value,
          setNumber: loggedSets.value.get(ex.name)!.length,
          weightKg: set.weightKg,
          reps: set.reps,
          rpe: set.rpe,
        },
      })
    } catch {
      toast.add({ title: 'Save warning', description: 'Set may not have saved to server.', color: 'warning' })
    }
  }

  const prescribed = prescribedSetCount(ex.prescription)
  if (loggedSets.value.get(ex.name)!.length >= prescribed) {
    modalOpen.value = false
    toast.add({ title: ex.name, description: 'All sets completed!', color: 'success' })

    const nextIncomplete = templateExercises.value.findIndex((e) =>
      exerciseSets(e.name).length < prescribedSetCount(e.prescription))
    if (nextIncomplete >= 0) {
      setTimeout(() => {
        activeExerciseIndex.value = nextIncomplete
        modalOpen.value = true
      }, 500)
    }
  }
}

async function finish() {
  try {
    await store.finishWorkout()
    step.value = 'feedback'
  } catch {
    toast.add({ title: 'Failed to save workout', description: 'Please try again.', color: 'error' })
  }
}

async function discardWorkout() {
  if (store.workoutLogId) {
    try {
      await $fetch(`/api/fitness/workout-logs/${store.workoutLogId}`, { method: 'DELETE' })
    } catch {
      // best effort
    }
  }
  store.resetWorkout()
  loggedSets.value.clear()
  showDiscardConfirm.value = false
  toast.add({ title: 'Workout discarded', color: 'neutral' })
  navigateTo('/')
}

const durationMinutes = computed(() => {
  if (!store.startedAt) return 0
  return Math.round((Date.now() - new Date(store.startedAt).getTime()) / 60000)
})

const calories = computed(() => estimateCalories({
  exercises: templateExercises.value.map((ex) => ({
    name: ex.name,
    sets: exerciseSets(ex.name).map(s => ({ weightKg: s.weightKg, reps: s.reps })),
  })),
  bodyWeightKg: 85,
  totalDurationMinutes: durationMinutes.value,
}))

const avgRest = computed(() => {
  if (store.restIntervals.length === 0) return 0
  return Math.round(store.restIntervals.reduce((a, b) => a + b, 0) / store.restIntervals.length)
})

function goToSummary() {
  step.value = 'summary'
}

function goHome() {
  store.resetWorkout()
  loggedSets.value.clear()
  navigateTo('/')
}

async function logout() {
  await $fetch('/api/auth/logout', { method: 'POST' })
  await clear()
  store.resetWorkout()
  navigateTo('/login')
}
</script>

<style scoped>
.workout-page {
  max-width: 600px;
  margin: 0 auto;
}

.preview-state { text-align: center; }
.workout-heading {
  font-family: 'Oswald', sans-serif;
  font-size: 2rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-primary);
  margin-bottom: 0.25rem;
}
.workout-date {
  color: var(--text-secondary);
  margin-bottom: 2rem;
}
.exercise-list {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 2rem;
  text-align: left;
}
.exercise-item {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--border-subtle);
  font-size: 0.95rem;
}
.exercise-item:last-child { border-bottom: none; }
.exercise-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
}
.ex-name { font-weight: 500; color: var(--text-primary); }
.ex-rx { color: var(--accent-orange); font-size: 0.85rem; }
.block-header {
  font-family: 'Oswald', sans-serif;
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.15em;
  color: var(--text-muted);
  font-weight: 600;
  padding: 0.75rem 1rem 0.4rem;
  background: var(--section-bg);
  border-bottom: 1px solid var(--border-subtle);
}
.empty-msg {
  color: var(--text-secondary);
  font-size: 0.95rem;
  margin: 0 0 2rem;
  max-width: 400px;
  margin-left: auto;
  margin-right: auto;
}
.start-btn {
  display: block;
  width: 100%;
  background: linear-gradient(135deg, var(--accent-orange), #ff8c5a);
  color: #fff;
  font-family: 'Oswald', sans-serif;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-size: 1.25rem;
  border: none;
  border-radius: 12px;
  padding: 1.1rem;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 1rem;
}
.start-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(232, 93, 37, 0.4);
}
.back-link {
  display: block;
  text-align: center;
  color: var(--text-muted);
  font-size: 0.9rem;
  text-decoration: none;
}
.back-link:hover { color: var(--accent-orange); }
.header-back {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  color: var(--text-muted);
  text-decoration: none;
  font-family: 'Oswald', sans-serif;
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  margin-bottom: 0.5rem;
  transition: color 0.2s;
}
.header-back:hover { color: var(--accent-orange); }

.sticky-header {
  position: sticky;
  top: 0;
  z-index: 10;
  background: var(--bg-app);
  padding: 0.5rem 0 1rem;
  border-bottom: 1px solid var(--border-subtle);
  margin-bottom: 1.5rem;
}

.exercise-cards {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 2rem;
}
.exercise-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: 10px;
  padding: 1rem 1.25rem;
  cursor: pointer;
  transition: all 0.2s ease;
}
.exercise-card:hover {
  border-color: var(--accent-orange);
  transform: translateX(4px);
}
.exercise-card.done {
  opacity: 0.6;
  border-color: var(--accent-green);
}
.exercise-card.done:hover {
  opacity: 0.8;
  border-color: var(--accent-green);
}
.ex-card-name {
  font-family: 'Oswald', sans-serif;
  font-size: 1.05rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--text-primary);
  display: block;
}
.ex-card-rx {
  font-size: 0.8rem;
  color: var(--accent-orange);
}
.ex-card-right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.ex-card-count {
  font-family: 'Oswald', sans-serif;
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--text-muted);
}
.check-icon { color: var(--accent-green); }
.chevron-icon { color: var(--text-muted); }

.workout-actions {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding-bottom: 2rem;
}
.finish-btn {
  width: 100%;
  background: linear-gradient(135deg, var(--accent-orange), #ff8c5a);
  color: #fff;
  font-family: 'Oswald', sans-serif;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-size: 1.1rem;
  border: none;
  border-radius: 10px;
  padding: 1rem;
  cursor: pointer;
  transition: all 0.2s ease;
}
.finish-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(232, 93, 37, 0.4);
}
.finish-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.discard-btn {
  width: 100%;
  background: none;
  border: 1px solid #ef4444;
  border-radius: 10px;
  padding: 0.75rem;
  font-family: 'Oswald', sans-serif;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-size: 0.9rem;
  color: #ef4444;
  cursor: pointer;
  transition: all 0.2s ease;
}
.discard-btn:hover {
  background: rgba(239, 68, 68, 0.1);
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 200;
  padding: 1rem;
}
.confirm-modal {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: 16px;
  padding: 2rem;
  max-width: 380px;
  width: 100%;
  text-align: center;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  animation: slideUp 0.2s ease;
}
@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
.confirm-title {
  font-family: 'Oswald', sans-serif;
  font-size: 1.25rem;
  font-weight: 700;
  text-transform: uppercase;
  color: var(--text-primary);
  margin-bottom: 0.5rem;
}
.confirm-text {
  color: var(--text-secondary);
  font-size: 0.9rem;
  margin-bottom: 1.5rem;
}
.confirm-actions {
  display: flex;
  gap: 0.75rem;
}
.discard-confirm-btn {
  flex: 1;
  background: #ef4444;
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 0.75rem;
  font-family: 'Oswald', sans-serif;
  font-weight: 600;
  text-transform: uppercase;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s;
}
.discard-confirm-btn:hover {
  filter: brightness(1.1);
}
.cancel-btn {
  flex: 1;
  background: none;
  border: 1px solid var(--border-subtle);
  border-radius: 8px;
  padding: 0.75rem;
  font-size: 0.9rem;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}
.cancel-btn:hover {
  border-color: var(--accent-orange);
  color: var(--accent-orange);
}

.secondary-btn {
  width: 100%;
  background: none;
  border: 1px solid var(--border-subtle);
  border-radius: 10px;
  padding: 0.75rem;
  font-size: 0.9rem;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: 'Source Sans 3', sans-serif;
}
.secondary-btn:hover {
  border-color: var(--text-secondary);
  color: var(--text-secondary);
}
@media (max-width: 768px) {
  .workout-page {
    padding-bottom: 1rem;
  }
  .workout-heading {
    font-size: 1.5rem;
  }
  .workout-date {
    font-size: 0.9rem;
    margin-bottom: 1.25rem;
  }
  .exercise-list {
    margin-bottom: 1.25rem;
  }
  .exercise-item {
    padding: 0.65rem 0.85rem;
    font-size: 0.9rem;
  }
  .start-btn {
    font-size: 1.1rem;
    padding: 1rem;
    min-height: 56px;
  }
  .sticky-header {
    padding: 0.5rem 0;
  }
  .exercise-card {
    padding: 0.85rem 1rem;
    min-height: 64px;
  }
  .ex-card-name {
    font-size: 0.95rem;
  }
  .finish-btn,
  .discard-btn,
  .secondary-btn {
    min-height: 48px;
    font-size: 1rem;
  }
}
</style>
