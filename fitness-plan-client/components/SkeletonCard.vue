<template>
  <div class="skeleton-card" :class="size">
    <div class="skeleton-line title" />
    <div class="skeleton-line subtitle" />
    <div v-if="size !== 'sm'" class="skeleton-line body" />
  </div>
</template>

<script setup lang="ts">
defineProps<{ size?: 'sm' | 'md' | 'lg' }>()
</script>

<style scoped>
.skeleton-card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: 10px;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.skeleton-card.sm { padding: 0.65rem; gap: 0.4rem; }
.skeleton-line {
  height: 12px;
  background: linear-gradient(90deg, var(--section-bg) 25%, var(--border-subtle) 50%, var(--section-bg) 75%);
  background-size: 200% 100%;
  border-radius: 4px;
  animation: shimmer 1.5s infinite;
}
.skeleton-line.title { width: 60%; height: 16px; }
.skeleton-line.subtitle { width: 40%; height: 10px; }
.skeleton-line.body { width: 80%; height: 10px; }
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
