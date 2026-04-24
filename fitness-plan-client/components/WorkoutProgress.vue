<template>
  <div class="progress-bar">
    <div class="progress-track">
      <div class="progress-fill" :style="{ width: `${percentage}%` }" />
    </div>
    <span class="progress-text">{{ completed }}/{{ total }} sets</span>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{ completed: number; total: number }>()
const percentage = computed(() => props.total > 0 ? Math.round((props.completed / props.total) * 100) : 0)
</script>

<style scoped>
.progress-bar {
  display: flex;
  align-items: center;
  gap: 1rem;
}
.progress-track {
  flex: 1;
  height: 8px;
  background: var(--border-subtle);
  border-radius: 4px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--accent-orange), var(--accent-yellow));
  border-radius: 4px;
  transition: width 0.3s ease;
}
.progress-text {
  font-family: 'Oswald', sans-serif;
  font-size: 0.85rem;
  color: var(--text-secondary);
  white-space: nowrap;
}
</style>
