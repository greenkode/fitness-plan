<template>
  <div v-if="suggestions.length || canAnalyze" class="coaching-section">
    <div class="coaching-header">
      <span class="coaching-label">AI Coach</span>
      <button
        v-if="canAnalyze"
        class="analyze-btn"
        :disabled="analyzing"
        @click="analyze"
      >
        {{ analyzing ? 'Analyzing...' : suggestions.length ? 'Refresh' : 'Get Insights' }}
      </button>
    </div>

    <div v-if="suggestions.length" class="suggestions-list">
      <div
        v-for="s in suggestions"
        :key="s.id"
        class="suggestion-card"
        :class="'priority-' + (s.details?.priority || 'medium')"
      >
        <div class="suggestion-info">
          <span class="suggestion-type">{{ formatType(s.eventType) }}</span>
          <h4 class="suggestion-title">{{ s.details?.title }}</h4>
          <p class="suggestion-rationale">{{ s.details?.rationale }}</p>
        </div>
        <div class="suggestion-actions">
          <button class="accept-btn" @click="resolve(s.id, true)">Accept</button>
          <button class="dismiss-btn" @click="resolve(s.id, false)">Dismiss</button>
        </div>
      </div>
    </div>

    <p v-else class="empty-coaching">Click "Get Insights" to analyze your recent workouts and get personalized suggestions.</p>
  </div>
</template>

<script setup lang="ts">
interface Suggestion {
  id: string
  eventType: string
  details: { title?: string; rationale?: string; priority?: string }
  accepted: boolean | null
  suggestedAt: string
}

const suggestions = ref<Suggestion[]>([])
const analyzing = ref(false)
const canAnalyze = ref(true)
const toast = useToast()

async function load() {
  try {
    suggestions.value = await $fetch<Suggestion[]>('/api/fitness/coaching')
  } catch {
    suggestions.value = []
  }
}

async function analyze() {
  analyzing.value = true
  try {
    const result = await $fetch<{ suggestions: any[]; dataAnalyzed?: { workouts: number; feedback: number } }>('/api/fitness/coaching/analyze', {
      method: 'POST',
    })
    if (result.suggestions.length === 0) {
      toast.add({ title: 'No suggestions yet', description: 'Log a few more workouts to get personalized insights.', color: 'neutral' })
    } else {
      toast.add({ title: `${result.suggestions.length} new suggestions`, color: 'success' })
    }
    await load()
  } catch (e: any) {
    toast.add({ title: 'Analysis failed', description: e.data?.statusMessage || 'Try again later.', color: 'error' })
  } finally {
    analyzing.value = false
  }
}

async function resolve(id: string, accepted: boolean) {
  try {
    await $fetch(`/api/fitness/coaching/${id}/resolve`, {
      method: 'PATCH',
      body: { accepted },
    })
    suggestions.value = suggestions.value.filter(s => s.id !== id)
    toast.add({ title: accepted ? 'Suggestion accepted' : 'Dismissed', color: 'neutral' })
  } catch {
    toast.add({ title: 'Failed to update', color: 'error' })
  }
}

function formatType(t: string): string {
  return t.replace(/_/g, ' ')
}

onMounted(load)
</script>

<style scoped>
.coaching-section {
  background: linear-gradient(135deg, rgba(147, 51, 234, 0.06), rgba(37, 99, 235, 0.06));
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
  padding: 1.25rem;
  margin-bottom: 2rem;
}
:global(html.dark) .coaching-section {
  background: linear-gradient(135deg, rgba(168, 85, 247, 0.08), rgba(59, 130, 246, 0.08));
}
.coaching-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}
.coaching-label {
  font-family: 'Oswald', sans-serif;
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.15em;
  color: var(--accent-purple);
  font-weight: 600;
}
.analyze-btn {
  background: var(--accent-purple);
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 0.4rem 0.85rem;
  font-family: 'Oswald', sans-serif;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  cursor: pointer;
  transition: all 0.2s;
}
.analyze-btn:hover:not(:disabled) { filter: brightness(1.1); }
.analyze-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.suggestions-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.suggestion-card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: 8px;
  padding: 0.85rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.suggestion-card.priority-high {
  border-left: 3px solid var(--accent-orange);
}
.suggestion-card.priority-medium {
  border-left: 3px solid var(--accent-yellow);
}
.suggestion-card.priority-low {
  border-left: 3px solid var(--text-muted);
}
.suggestion-type {
  font-size: 0.6rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--accent-purple);
  font-weight: 600;
}
.suggestion-title {
  font-family: 'Oswald', sans-serif;
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0.2rem 0 0.4rem;
}
.suggestion-rationale {
  font-size: 0.85rem;
  color: var(--text-secondary);
  margin: 0;
  line-height: 1.5;
}
.suggestion-actions {
  display: flex;
  gap: 0.5rem;
}
.accept-btn, .dismiss-btn {
  font-family: 'Oswald', sans-serif;
  font-size: 0.7rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 0.35rem 0.85rem;
  border-radius: 6px;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}
.accept-btn {
  background: var(--accent-green);
  color: #000;
}
.accept-btn:hover { filter: brightness(1.1); }
.dismiss-btn {
  background: none;
  border: 1px solid var(--border-subtle);
  color: var(--text-muted);
}
.dismiss-btn:hover {
  border-color: var(--text-secondary);
  color: var(--text-secondary);
}
.empty-coaching {
  font-size: 0.85rem;
  color: var(--text-secondary);
  margin: 0;
  font-style: italic;
}
</style>
