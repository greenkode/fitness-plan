<template>
  <header class="page-header">
    <div class="title-block">
      <h1 class="title" :class="{ gradient }">{{ title }}</h1>
      <p v-if="subtitle" class="subtitle">{{ subtitle }}</p>
    </div>
    <div v-if="$slots.actions" class="actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  title: string
  subtitle?: string
  gradient?: boolean
}>(), {
  gradient: true,
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 1rem;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--border-subtle);
}
.title-block {
  flex: 1;
  min-width: 0;
}
.title {
  font-family: 'Oswald', sans-serif;
  font-size: 1.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-primary);
  margin: 0;
  line-height: 1.1;
}
.title.gradient {
  background: linear-gradient(135deg, var(--accent-orange), var(--accent-yellow));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.subtitle {
  font-size: 0.85rem;
  color: var(--text-secondary);
  margin: 0.25rem 0 0;
}
.actions {
  display: flex;
  gap: 0.5rem;
  flex-shrink: 0;
}
@media (max-width: 600px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: 0.75rem;
  }
  .title { font-size: 1.5rem; }
  .actions { width: 100%; }
}
</style>
