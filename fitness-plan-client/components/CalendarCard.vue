<template>
  <div class="day-card" @click="$emit('click')">
    <div class="day-header">
      <span class="day-name">{{ dayName }}</span>
      <span v-if="type !== 'rest'" class="day-type" :class="'type-' + type">{{ type }}</span>
    </div>
    <div class="day-content">
      <div class="workout-title">{{ title }}</div>
      <div v-if="duration" class="workout-duration">
        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" />
        </svg>
        {{ duration }}
      </div>
    </div>
    <div class="click-hint">Tap to view</div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  dayName: string
  type: 'gym' | 'cardio' | 'recovery' | 'rest'
  title: string
  duration?: string
}>()

defineEmits<{
  click: []
}>()
</script>

<style scoped>
.day-card {
  background: var(--bg-card);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid var(--border-subtle);
  box-shadow: 0 2px 8px var(--shadow-color);
}
.day-card:hover {
  transform: translateY(-4px);
  background: var(--bg-card-hover);
  border-color: var(--accent-orange);
  box-shadow: 0 8px 32px var(--shadow-color);
}
.day-header {
  padding: 1rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border-subtle);
}
.day-name {
  font-family: 'Oswald', sans-serif;
  font-size: 1.25rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.day-type {
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-weight: 600;
}
.type-gym { background: var(--accent-orange); color: #000; }
.type-cardio { background: var(--accent-green); color: #000; }
.type-recovery { background: var(--accent-purple); color: #fff; }
.day-content {
  padding: 1rem;
}
.workout-title {
  font-family: 'Oswald', sans-serif;
  font-size: 1rem;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 0.5rem;
  line-height: 1.3;
}
.workout-duration {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  color: var(--text-secondary);
}
.workout-duration svg { color: var(--accent-orange); }
.click-hint {
  text-align: center;
  padding: 0.75rem;
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--text-muted);
  border-top: 1px solid var(--border-subtle);
  transition: color 0.3s ease;
}
.day-card:hover .click-hint { color: var(--accent-orange); }
</style>
