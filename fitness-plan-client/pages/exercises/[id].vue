<template>
  <div class="exercise-page">
    <PageHeader :title="data?.exercise.name || 'Exercise'" :subtitle="data?.exercise.prescription || ''">
      <template #actions>
        <button class="back-btn" @click="goBack">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6" /></svg>
          Back
        </button>
      </template>
    </PageHeader>

    <div v-if="loading" class="loading">Loading…</div>
    <div v-else-if="!data" class="empty">Exercise not found.</div>

    <template v-else>
      <section class="card">
        <div class="section-row">
          <h3 class="section-heading">How to perform</h3>
          <div class="section-actions">
            <button
              v-if="!editingDescription"
              class="text-btn"
              :disabled="generating"
              @click="generateDescription"
            >
              {{ generating ? 'Generating…' : (data.exercise.description ? 'Regenerate' : 'Generate with AI') }}
            </button>
            <button
              v-if="!editingDescription"
              class="text-btn"
              @click="startEditDescription"
            >Edit</button>
          </div>
        </div>

        <div v-if="editingDescription" class="edit-block">
          <textarea
            v-model="descriptionDraft"
            class="description-input"
            rows="3"
            placeholder="Execution cues and target muscles…"
          />
          <div class="edit-actions">
            <button class="save-btn" :disabled="savingDescription" @click="saveDescription">
              {{ savingDescription ? 'Saving…' : 'Save' }}
            </button>
            <button class="cancel-btn-sm" :disabled="savingDescription" @click="editingDescription = false">Cancel</button>
          </div>
        </div>
        <p v-else-if="data.exercise.description" class="description">{{ data.exercise.description }}</p>
        <p v-else class="empty-inline">No description yet.</p>
      </section>

      <section class="card">
        <h3 class="section-heading">References</h3>
        <ExerciseMedia
          :exercise-id="data.exercise.id"
          :initial-media="data.media"
          :can-edit="true"
        />
      </section>

      <section v-if="data.stats.totalSets > 0" class="card">
        <h3 class="section-heading">Personal records</h3>
        <div class="stats-grid">
          <div class="stat">
            <span class="stat-value">{{ data.stats.maxWeight }}<small>kg</small></span>
            <span class="stat-label">Max weight</span>
          </div>
          <div class="stat">
            <span class="stat-value">{{ data.stats.maxReps }}</span>
            <span class="stat-label">Max reps</span>
          </div>
          <div class="stat">
            <span class="stat-value">{{ Math.round(data.stats.maxVolume) }}<small>kg</small></span>
            <span class="stat-label">Max set volume</span>
          </div>
          <div class="stat highlight">
            <span class="stat-value">{{ data.stats.estimatedOneRm }}<small>kg</small></span>
            <span class="stat-label">Est. 1RM</span>
          </div>
          <div class="stat">
            <span class="stat-value">{{ data.stats.totalSessions }}</span>
            <span class="stat-label">Sessions</span>
          </div>
          <div class="stat">
            <span class="stat-value">{{ data.stats.totalSets }}</span>
            <span class="stat-label">Total sets</span>
          </div>
        </div>
      </section>

      <section v-if="trendPoints.length > 1" class="card">
        <h3 class="section-heading">Top set per session</h3>
        <div class="trend-chart">
          <div
            v-for="(p, i) in trendPoints"
            :key="i"
            class="trend-bar"
            :style="{ height: `${(p.weight / maxTrendWeight) * 100}%` }"
            :title="`${formatDate(p.date)}: ${p.weight}kg × ${p.reps}`"
          >
            <span class="trend-value">{{ p.weight }}</span>
          </div>
        </div>
        <div class="trend-axis">
          <span>{{ formatDate(trendPoints[0]!.date) }}</span>
          <span>{{ formatDate(trendPoints[trendPoints.length - 1]!.date) }}</span>
        </div>
      </section>

      <section class="card">
        <h3 class="section-heading">History</h3>
        <div v-if="data.history.length === 0" class="empty-inline">No sets logged yet.</div>
        <div v-else class="history-list">
          <div v-for="session in data.history" :key="session.date" class="session">
            <div class="session-header">
              <span class="session-date">{{ formatDate(session.date) }}</span>
              <span class="session-meta">{{ session.sets.length }} sets</span>
            </div>
            <div class="set-list">
              <div v-for="set in session.sets" :key="set.setNumber" class="set-row">
                <span class="set-num">#{{ set.setNumber }}</span>
                <span class="set-detail">
                  <template v-if="set.weightKg !== null && set.reps !== null">{{ set.weightKg }}kg × {{ set.reps }}</template>
                  <template v-else-if="set.durationSeconds !== null">{{ set.durationSeconds }}s</template>
                  <template v-else>{{ set.reps }}</template>
                </span>
                <span v-if="set.rpe !== null" class="set-rpe">RPE {{ set.rpe }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ middleware: 'auth' })

interface MediaItem {
  id: string
  url: string
  mediaType: string
  label: string | null
  sortOrder: number
}

interface SetEntry {
  setNumber: number
  weightKg: number | null
  reps: number | null
  durationSeconds: number | null
  rpe: number | null
}

interface SessionEntry {
  date: string
  sets: SetEntry[]
}

interface ExerciseDetail {
  exercise: { id: string; name: string; prescription: string | null; description: string | null }
  media: MediaItem[]
  history: SessionEntry[]
  stats: {
    totalSessions: number
    totalSets: number
    maxWeight: number
    maxReps: number
    maxVolume: number
    estimatedOneRm: number
  }
}

const route = useRoute()
const router = useRouter()
const toast = useToast()
const data = ref<ExerciseDetail | null>(null)
const loading = ref(true)

const editingDescription = ref(false)
const descriptionDraft = ref('')
const savingDescription = ref(false)
const generating = ref(false)

function startEditDescription() {
  if (!data.value) return
  descriptionDraft.value = data.value.exercise.description || ''
  editingDescription.value = true
}

async function saveDescription() {
  if (!data.value) return
  savingDescription.value = true
  try {
    const updated = await $fetch<{ id: string; description: string | null }>(`/api/fitness/exercises/${data.value.exercise.id}`, {
      method: 'PATCH',
      body: { description: descriptionDraft.value },
    })
    data.value.exercise.description = updated.description
    editingDescription.value = false
    toast.add({ title: 'Description saved', color: 'success' })
  } catch {
    toast.add({ title: 'Failed to save', color: 'error' })
  } finally {
    savingDescription.value = false
  }
}

async function generateDescription() {
  if (!data.value) return
  generating.value = true
  try {
    const updated = await $fetch<{ id: string; description: string | null }>(`/api/fitness/exercises/${data.value.exercise.id}/generate-description`, {
      method: 'POST',
    })
    data.value.exercise.description = updated.description
    toast.add({ title: 'Description generated', color: 'success' })
  } catch (e: any) {
    toast.add({ title: 'Failed to generate', description: e.data?.statusMessage || 'Try again', color: 'error' })
  } finally {
    generating.value = false
  }
}

onMounted(async () => {
  try {
    data.value = await $fetch<ExerciseDetail>(`/api/fitness/exercises/${route.params.id}`)
  } catch {
    data.value = null
  } finally {
    loading.value = false
  }
})

const trendPoints = computed(() => {
  if (!data.value) return []
  const points = data.value.history.map(session => {
    let bestWeight = 0
    let bestReps = 0
    for (const s of session.sets) {
      const w = s.weightKg || 0
      if (w > bestWeight) {
        bestWeight = w
        bestReps = s.reps || 0
      }
    }
    return { date: session.date, weight: bestWeight, reps: bestReps }
  }).filter(p => p.weight > 0)
  return points.reverse()
})

const maxTrendWeight = computed(() => {
  if (!trendPoints.value.length) return 1
  return Math.max(...trendPoints.value.map(p => p.weight), 1)
})

function formatDate(d: string): string {
  return new Date(d + 'T00:00:00').toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
}

function goBack() {
  if (window.history.length > 1) router.back()
  else navigateTo('/')
}
</script>

<style scoped>
.exercise-page {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  background: none;
  border: 1px solid var(--border-subtle);
  color: var(--text-secondary);
  font-size: 0.8rem;
  padding: 0.45rem 0.8rem;
  border-radius: 6px;
  cursor: pointer;
  font-family: 'Oswald', sans-serif;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  transition: all 0.2s;
}
.back-btn:hover {
  border-color: var(--accent-orange);
  color: var(--accent-orange);
}
.loading, .empty {
  text-align: center;
  padding: 3rem;
  color: var(--text-muted);
}
.empty-inline {
  color: var(--text-muted);
  font-size: 0.85rem;
  font-style: italic;
}
.card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
  padding: 1rem 1.25rem;
}
.section-heading {
  font-family: 'Oswald', sans-serif;
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: var(--text-secondary);
  margin: 0 0 0.75rem;
}
.description {
  margin: 0;
  font-size: 0.95rem;
  color: var(--text-primary);
  line-height: 1.5;
}
.section-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}
.section-row .section-heading {
  margin: 0;
}
.section-actions {
  display: flex;
  gap: 0.4rem;
}
.text-btn {
  background: none;
  border: 1px solid var(--border-subtle);
  color: var(--text-muted);
  font-size: 0.7rem;
  font-family: 'Oswald', sans-serif;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 0.3rem 0.6rem;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}
.text-btn:hover:not(:disabled) {
  border-color: var(--accent-orange);
  color: var(--accent-orange);
}
.text-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.edit-block {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.description-input {
  width: 100%;
  background: var(--section-bg);
  border: 1px solid var(--border-subtle);
  border-radius: 8px;
  padding: 0.6rem 0.75rem;
  font-size: 0.9rem;
  color: var(--text-primary);
  font-family: 'Source Sans 3', sans-serif;
  resize: vertical;
  outline: none;
  line-height: 1.4;
}
.description-input:focus { border-color: var(--accent-orange); }
.edit-actions {
  display: flex;
  gap: 0.4rem;
  justify-content: flex-end;
}
.save-btn {
  background: var(--accent-orange);
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 0.4rem 0.85rem;
  font-family: 'Oswald', sans-serif;
  font-weight: 600;
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  cursor: pointer;
  transition: filter 0.2s;
}
.save-btn:hover:not(:disabled) { filter: brightness(1.1); }
.save-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.cancel-btn-sm {
  background: none;
  border: 1px solid var(--border-subtle);
  color: var(--text-muted);
  border-radius: 6px;
  padding: 0.4rem 0.85rem;
  font-family: 'Oswald', sans-serif;
  font-weight: 600;
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  cursor: pointer;
}
.cancel-btn-sm:hover:not(:disabled) { border-color: var(--text-secondary); color: var(--text-secondary); }
.cancel-btn-sm:disabled { opacity: 0.5; cursor: not-allowed; }

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(110px, 1fr));
  gap: 0.6rem;
}
.stat {
  background: var(--section-bg);
  border: 1px solid var(--border-subtle);
  border-radius: 8px;
  padding: 0.65rem 0.85rem;
  text-align: center;
}
.stat.highlight {
  border-color: var(--accent-orange);
  background: linear-gradient(135deg, rgba(232, 93, 37, 0.08), rgba(212, 160, 18, 0.05));
}
.stat-value {
  display: block;
  font-family: 'Oswald', sans-serif;
  font-size: 1.3rem;
  font-weight: 700;
  color: var(--accent-orange);
  line-height: 1.1;
}
.stat-value small {
  font-size: 0.6rem;
  font-weight: 400;
  color: var(--text-muted);
  margin-left: 0.15rem;
}
.stat-label {
  display: block;
  font-size: 0.65rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--text-muted);
  margin-top: 0.2rem;
}

.trend-chart {
  display: flex;
  align-items: flex-end;
  gap: 0.3rem;
  height: 100px;
  padding: 0.5rem 0;
}
.trend-bar {
  flex: 1;
  min-height: 6px;
  background: linear-gradient(to top, var(--accent-orange), var(--accent-yellow));
  border-radius: 3px 3px 0 0;
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 2px;
}
.trend-value {
  font-size: 0.55rem;
  color: #fff;
  font-weight: 600;
  font-family: 'Oswald', sans-serif;
}
.trend-axis {
  display: flex;
  justify-content: space-between;
  font-size: 0.7rem;
  color: var(--text-muted);
  margin-top: 0.25rem;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}
.session {
  border-top: 1px solid var(--border-subtle);
  padding-top: 0.65rem;
}
.session:first-child {
  border-top: none;
  padding-top: 0;
}
.session-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 0.4rem;
}
.session-date {
  font-family: 'Oswald', sans-serif;
  font-weight: 600;
  font-size: 0.9rem;
  color: var(--text-primary);
}
.session-meta {
  font-size: 0.7rem;
  color: var(--text-muted);
}
.set-list {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}
.set-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.85rem;
}
.set-num {
  font-family: 'Oswald', sans-serif;
  font-size: 0.75rem;
  color: var(--text-muted);
  width: 28px;
}
.set-detail {
  flex: 1;
  color: var(--text-primary);
}
.set-rpe {
  font-size: 0.7rem;
  color: var(--accent-orange);
  font-weight: 600;
}
</style>
