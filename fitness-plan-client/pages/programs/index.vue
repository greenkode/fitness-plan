<template>
  <div class="programs-page">
    <div class="page-header">
      <h2 class="page-title">My Programs</h2>
      <NuxtLink to="/programs/new" class="new-btn">
        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" /></svg>
        New Program
      </NuxtLink>
    </div>

    <div v-if="loading" class="loading-state">Loading...</div>
    <div v-else-if="!programs.length" class="empty-state">
      <p>No programs yet.</p>
      <NuxtLink to="/programs/new" class="create-link">Create your first program with AI</NuxtLink>
    </div>

    <div v-else class="program-list">
      <div v-for="p in programs" :key="p.id" class="program-card" :class="'status-' + p.status">
        <div class="card-top">
          <div class="card-info">
            <h3 class="program-name">{{ p.templateName }}</h3>
            <p v-if="p.templateDescription" class="program-desc">{{ p.templateDescription }}</p>
            <div class="program-meta">
              <span v-if="p.category" class="meta-badge badge-cat">{{ p.category }}</span>
              <span v-if="p.difficulty" class="meta-badge badge-diff">{{ p.difficulty }}</span>
              <span class="meta-text">Started {{ formatDate(p.startedAt) }}</span>
            </div>
          </div>
          <div class="card-status">
            <span class="status-label" :class="'label-' + p.status">{{ p.status }}</span>
          </div>
        </div>

        <div v-if="p.totalPhases > 0" class="phase-progress">
          <div class="phase-bar">
            <div
              class="phase-fill"
              :style="{ width: `${((p.currentPhaseNumber || 1) / p.totalPhases) * 100}%` }"
            />
          </div>
          <span class="phase-label">
            {{ p.currentPhase || 'Phase 1' }} ({{ p.currentPhaseNumber || 1 }}/{{ p.totalPhases }})
          </span>
        </div>

        <div class="card-actions">
          <button
            v-if="p.status === 'active'"
            class="action-btn pause-btn"
            @click.stop="updateStatus(p, 'paused')"
          >Pause</button>
          <button
            v-if="p.status === 'paused'"
            class="action-btn resume-btn"
            @click.stop="updateStatus(p, 'active')"
          >Resume</button>
          <button
            v-if="p.status !== 'completed'"
            class="action-btn complete-btn"
            @click.stop="updateStatus(p, 'completed')"
          >Complete</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ middleware: 'auth' })

const toast = useToast()

interface ProgramData {
  id: string
  templateName: string
  templateDescription: string | null
  category: string | null
  difficulty: string | null
  status: string
  startedAt: string
  pausedAt: string | null
  completedAt: string | null
  currentPhase: string | null
  currentPhaseNumber: number | null
  totalPhases: number
}

const programs = ref<ProgramData[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    programs.value = await $fetch<ProgramData[]>('/api/fitness/programs')
  } catch {
    programs.value = []
  } finally {
    loading.value = false
  }
})

async function updateStatus(p: ProgramData, status: string) {
  try {
    await $fetch(`/api/fitness/programs/${p.id}`, {
      method: 'PATCH',
      body: { status },
    })
    p.status = status
    if (status === 'completed') p.completedAt = new Date().toISOString()
    if (status === 'paused') p.pausedAt = new Date().toISOString()
    if (status === 'active') p.pausedAt = null
    toast.add({ title: `Program ${status}`, color: 'success' })
  } catch {
    toast.add({ title: 'Failed to update', color: 'error' })
  }
}

function formatDate(dateStr: string): string {
  const d = new Date(dateStr)
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}
</script>

<style scoped>
.programs-page {
  max-width: 700px;
  margin: 0 auto;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
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
  margin: 0;
}
.new-btn {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  background: var(--accent-orange);
  color: #fff;
  font-family: 'Oswald', sans-serif;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-size: 0.85rem;
  padding: 0.6rem 1.1rem;
  border-radius: 8px;
  text-decoration: none;
  transition: all 0.2s;
}
.new-btn:hover {
  filter: brightness(1.1);
  transform: translateY(-1px);
}

.loading-state, .empty-state {
  text-align: center;
  padding: 3rem;
  color: var(--text-muted);
}
.create-link {
  display: inline-block;
  margin-top: 1rem;
  color: var(--accent-orange);
  text-decoration: none;
  font-weight: 600;
}
.create-link:hover { text-decoration: underline; }

.program-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.program-card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
  padding: 1.25rem;
  transition: border-color 0.2s;
}
.program-card.status-active { border-left: 3px solid var(--accent-orange); }
.program-card.status-paused { border-left: 3px solid var(--accent-yellow); }
.program-card.status-completed { border-left: 3px solid var(--accent-green); opacity: 0.7; }

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
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
.program-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.meta-badge {
  font-size: 0.6rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  padding: 0.15rem 0.5rem;
  border-radius: 4px;
  font-weight: 600;
}
.badge-cat { background: var(--accent-orange); color: #000; }
.badge-diff { background: var(--accent-purple); color: #fff; }
.meta-text {
  font-size: 0.75rem;
  color: var(--text-muted);
}
.status-label {
  font-size: 0.65rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  font-weight: 600;
  padding: 0.25rem 0.6rem;
  border-radius: 4px;
}
.label-active { background: rgba(232, 93, 37, 0.15); color: var(--accent-orange); }
.label-paused { background: rgba(212, 160, 18, 0.15); color: var(--accent-yellow); }
.label-completed { background: rgba(13, 148, 136, 0.15); color: var(--accent-green); }

.phase-progress {
  margin-top: 1rem;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.phase-bar {
  flex: 1;
  height: 6px;
  background: var(--border-subtle);
  border-radius: 3px;
  overflow: hidden;
}
.phase-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--accent-orange), var(--accent-yellow));
  border-radius: 3px;
  transition: width 0.3s;
}
.phase-label {
  font-size: 0.75rem;
  color: var(--text-muted);
  white-space: nowrap;
}

.card-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 1rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--border-subtle);
}
.action-btn {
  font-size: 0.7rem;
  font-family: 'Oswald', sans-serif;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  padding: 0.4rem 0.85rem;
  border-radius: 6px;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}
.pause-btn {
  background: none;
  border: 1px solid var(--accent-yellow);
  color: var(--accent-yellow);
}
.pause-btn:hover { background: rgba(212, 160, 18, 0.1); }
.resume-btn {
  background: var(--accent-orange);
  color: #fff;
}
.resume-btn:hover { filter: brightness(1.1); }
.complete-btn {
  background: none;
  border: 1px solid var(--accent-green);
  color: var(--accent-green);
}
.complete-btn:hover { background: rgba(13, 148, 136, 0.1); }
</style>
