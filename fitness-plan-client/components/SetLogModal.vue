<template>
  <div v-if="open" class="modal-overlay" @click.self="$emit('close')">
    <div class="modal">
      <div class="modal-header">
        <div class="header-info">
          <h2 class="exercise-name">{{ exerciseName }}</h2>
          <span class="exercise-rx">{{ prescription }}</span>
        </div>
        <div class="header-actions">
          <NuxtLink v-if="exerciseId" :to="`/exercises/${exerciseId}`" class="details-link">
            Details
            <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6" /></svg>
          </NuxtLink>
          <button class="close-btn" @click="$emit('close')">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>
      </div>

      <div class="modal-body">

        <div class="sets-log">
          <div v-if="sets.length === 0" class="empty-state">No sets logged yet</div>
          <div v-for="(set, i) in sets" :key="i" class="logged-set">
            <span class="set-number">{{ i + 1 }}</span>
            <span class="set-detail">{{ set.weightKg }}kg x {{ set.reps }}</span>
            <span class="set-diff">{{ set.rpe }}/10</span>
          </div>
        </div>

        <form class="chat-form" @submit.prevent="parseWithAi">
          <div class="chat-row">
            <input
              ref="chatRef"
              v-model="chatInput"
              type="text"
              class="chat-field"
              placeholder="100kg x 5, held 60s, bodyweight x 12..."
              :disabled="parsing"
            />
            <button type="submit" class="log-btn-inline" :disabled="!chatInput.trim() || parsing">
              <svg v-if="!parsing" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="22" y1="2" x2="11" y2="13" /><polygon points="22 2 15 22 11 13 2 9 22 2" /></svg>
              <span v-else class="spinner"></span>
            </button>
          </div>
          <p v-if="parsing" class="parse-status">AI is parsing...</p>
          <p v-else-if="parseError" class="parse-error">{{ parseError }}</p>
          <p v-else class="parse-hint">Describe what you did — weight, reps, time, difficulty</p>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface LoggedSet {
  weightKg: number
  reps: number
  rpe: number
}

const props = defineProps<{
  open: boolean
  exerciseId?: string
  exerciseName: string
  prescription: string
  existingSets: LoggedSet[]
}>()

const emit = defineEmits<{
  close: []
  'set-logged': [set: LoggedSet]
}>()

const chatInput = ref('')
const chatRef = ref<HTMLInputElement>()
const parseError = ref('')
const parsing = ref(false)

const sets = computed(() => props.existingSets)

const lastWeight = computed(() => {
  if (sets.value.length > 0) return sets.value[sets.value.length - 1].weightKg
  return null
})

watch(() => props.open, (val) => {
  if (val) {
    chatInput.value = ''
    parseError.value = ''
    nextTick(() => chatRef.value?.focus())
  }
})

async function parseWithAi() {
  const text = chatInput.value.trim()
  if (!text || parsing.value) return
  parseError.value = ''
  parsing.value = true

  try {
    const result = await $fetch<{
      source: string
      sets: { weightKg: number | null; reps: number | null; difficulty: number | null }[]
    }>('/api/ai/parse', {
      method: 'POST',
      body: { message: text, exerciseName: props.exerciseName },
    })

    if (result.sets && result.sets.length > 0) {
      for (const set of result.sets) {
        const w = set.weightKg ?? lastWeight.value ?? 0
        const r = set.reps
        if (r && r > 0) {
          emit('set-logged', {
            weightKg: w,
            reps: r,
            rpe: set.difficulty ?? 7,
          })
        }
      }
      chatInput.value = ''
    } else {
      parseError.value = 'Could not parse. Try: 100 5 7 or 80kg for 8'
    }
  } catch {
    parseError.value = 'AI unavailable. Use the fields above.'
  } finally {
    parsing.value = false
  }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 2rem 1rem;
  overflow-y: auto;
  z-index: 100;
  animation: fadeIn 0.15s ease;
}
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
.modal {
  background: var(--bg-card);
  border-radius: 16px;
  width: 100%;
  max-width: 480px;
  border: 1px solid var(--border-subtle);
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  animation: slideUp 0.2s ease;
}
@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
.modal-header {
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--border-subtle);
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.75rem;
  background: linear-gradient(135deg, rgba(232, 93, 37, 0.08) 0%, transparent 100%);
  border-radius: 16px 16px 0 0;
}
.header-info {
  flex: 1;
  min-width: 0;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}
.details-link {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  font-size: 0.7rem;
  font-family: 'Oswald', sans-serif;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-muted);
  text-decoration: none;
  padding: 0.35rem 0.55rem;
  border: 1px solid var(--border-subtle);
  border-radius: 6px;
  transition: all 0.2s;
}
.details-link:hover {
  border-color: var(--accent-orange);
  color: var(--accent-orange);
}
:global(html.dark) .modal-header {
  background: linear-gradient(135deg, rgba(255, 107, 53, 0.1) 0%, transparent 100%);
}
.exercise-name {
  font-family: 'Oswald', sans-serif;
  font-size: 1.4rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-primary);
  margin: 0;
}
.exercise-rx {
  font-size: 0.85rem;
  color: var(--accent-orange);
}
.close-btn {
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  padding: 0.5rem;
  border-radius: 8px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}
.close-btn:hover {
  color: var(--accent-orange);
  background: var(--section-bg);
}
.modal-body {
  padding: 1.25rem 1.5rem;
}
.sets-log {
  margin-bottom: 1.25rem;
  max-height: 200px;
  overflow-y: auto;
}
.empty-state {
  text-align: center;
  padding: 1rem;
  color: var(--text-muted);
  font-size: 0.85rem;
  font-style: italic;
}
.logged-set {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--border-subtle);
}
.logged-set:last-child { border-bottom: none; }
.set-number {
  font-family: 'Oswald', sans-serif;
  font-weight: 600;
  font-size: 0.85rem;
  color: var(--text-muted);
  width: 20px;
  text-align: center;
}
.set-detail {
  flex: 1;
  font-size: 0.95rem;
  color: var(--text-primary);
  font-weight: 500;
}
.set-diff {
  font-size: 0.8rem;
  color: var(--accent-orange);
  font-weight: 600;
}

.chat-form {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.chat-row {
  display: flex;
  gap: 0.5rem;
}
.chat-field {
  flex: 1;
  background: var(--section-bg);
  border: 1px solid var(--border-subtle);
  border-radius: 8px;
  padding: 0.6rem 0.75rem;
  font-size: 0.95rem;
  color: var(--text-primary);
  font-family: 'Source Sans 3', sans-serif;
  outline: none;
  transition: border-color 0.2s;
}
.chat-field:focus { border-color: var(--accent-orange); }
.chat-field::placeholder { color: var(--text-muted); }
.chat-field:disabled { opacity: 0.5; }

.log-btn-inline {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: var(--accent-orange);
  color: #fff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s;
  padding: 0;
}
.log-btn-inline:hover:not(:disabled) {
  filter: brightness(1.1);
  transform: scale(1.05);
}
.log-btn-inline:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.parse-error {
  margin: 0;
  font-size: 0.75rem;
  color: #ef4444;
}
.parse-status {
  margin: 0;
  font-size: 0.75rem;
  color: var(--accent-orange);
  font-style: italic;
}
.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 768px) {
  .modal-overlay { padding: 0; align-items: flex-end; }
  .modal {
    border-radius: 16px 16px 0 0;
    max-width: 100%;
    max-height: 90dvh;
    overflow-y: auto;
    animation: slideUpMobile 0.25s ease;
  }
  @keyframes slideUpMobile {
    from { transform: translateY(100%); }
    to { transform: translateY(0); }
  }
  .modal-header { padding: 1rem 1.25rem; }
  .exercise-name { font-size: 1.15rem; }
  .modal-body { padding: 1rem 1.25rem; }
  .chat-field {
    font-size: 1rem;
    padding: 0.75rem 1rem;
  }
  .log-btn-inline {
    width: 48px;
    height: 48px;
  }
  .close-btn {
    min-width: 44px;
    min-height: 44px;
  }
}
</style>
