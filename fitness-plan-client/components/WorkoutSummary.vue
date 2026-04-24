<template>
  <div class="summary-card">
    <h2 class="summary-title">Workout Complete</h2>

    <div class="stats-grid">
      <div class="summary-stat">
        <span class="summary-stat-value">{{ formatDuration(durationMinutes) }}</span>
        <span class="summary-stat-label">Duration</span>
      </div>
      <div class="summary-stat">
        <span class="summary-stat-value">{{ setsCompleted }}/{{ totalSets }}</span>
        <span class="summary-stat-label">Sets</span>
      </div>
      <div class="summary-stat">
        <span class="summary-stat-value">{{ totalVolume.toLocaleString() }}<small>kg</small></span>
        <span class="summary-stat-label">Volume</span>
      </div>
      <div class="summary-stat highlight">
        <span class="summary-stat-value">{{ estimatedCalories }}</span>
        <span class="summary-stat-label">Est. Calories</span>
      </div>
    </div>

    <div v-if="averageRpe" class="rpe-row">
      <span class="rpe-label">Average Difficulty</span>
      <span class="rpe-value">{{ averageRpe }}</span>
    </div>

    <div v-if="avgRestSeconds > 0" class="rpe-row">
      <span class="rpe-label">Avg Rest Time</span>
      <span class="rpe-value">{{ formatRestTime(avgRestSeconds) }}</span>
    </div>

    <button class="back-btn" @click="$emit('back')">Back to Calendar</button>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  durationMinutes: number
  setsCompleted: number
  totalSets: number
  totalVolume: number
  estimatedCalories: number
  averageRpe: number | null
  avgRestSeconds: number
}>()

defineEmits<{ back: [] }>()

function formatDuration(min: number): string {
  const h = Math.floor(min / 60)
  const m = Math.round(min % 60)
  return h > 0 ? `${h}h ${m}m` : `${m}m`
}

function formatRestTime(sec: number): string {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return m > 0 ? `${m}m ${s}s` : `${s}s`
}
</script>

<style scoped>
.summary-card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: 16px;
  padding: 2rem;
  max-width: 520px;
  margin: 0 auto;
  box-shadow: 0 4px 24px var(--shadow-color);
}
.summary-title {
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
  text-align: center;
}
.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  margin-bottom: 1.5rem;
}
.summary-stat {
  text-align: center;
  padding: 1rem;
  background: var(--section-bg);
  border-radius: 10px;
  border: 1px solid var(--border-subtle);
}
.summary-stat.highlight {
  border-color: var(--accent-orange);
  background: linear-gradient(135deg, rgba(232, 93, 37, 0.08), rgba(212, 160, 18, 0.08));
}
.summary-stat-value {
  display: block;
  font-family: 'Oswald', sans-serif;
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--accent-orange);
  line-height: 1.2;
}
.summary-stat-value small {
  font-size: 0.75rem;
  font-weight: 400;
  color: var(--text-muted);
}
.summary-stat-label {
  display: block;
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--text-muted);
  margin-top: 0.25rem;
}
.rpe-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 0;
  border-top: 1px solid var(--border-subtle);
}
.rpe-label {
  font-size: 0.85rem;
  color: var(--text-secondary);
}
.rpe-value {
  font-family: 'Oswald', sans-serif;
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-primary);
}
.back-btn {
  width: 100%;
  margin-top: 1.5rem;
  background: var(--accent-orange);
  color: #fff;
  font-family: 'Oswald', sans-serif;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  border: none;
  border-radius: 8px;
  padding: 0.85rem;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s ease;
}
.back-btn:hover {
  filter: brightness(1.1);
  transform: translateY(-1px);
}
</style>
