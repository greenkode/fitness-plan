<template>
  <div class="timer-container">
    <span class="timer-label">Elapsed</span>
    <span class="timer-display">{{ formatted }}</span>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{ startedAt: string | null }>()

const elapsed = ref(0)
let interval: ReturnType<typeof setInterval> | null = null

function update() {
  if (!props.startedAt) { elapsed.value = 0; return }
  elapsed.value = Math.floor((Date.now() - new Date(props.startedAt).getTime()) / 1000)
}

const formatted = computed(() => {
  const h = Math.floor(elapsed.value / 3600)
  const m = Math.floor((elapsed.value % 3600) / 60)
  const s = elapsed.value % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

onMounted(() => {
  update()
  interval = setInterval(update, 1000)
})

onUnmounted(() => {
  if (interval) clearInterval(interval)
})
</script>

<style scoped>
.timer-container {
  text-align: center;
  padding: 1rem;
}
.timer-label {
  display: block;
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.15em;
  color: var(--text-muted);
  margin-bottom: 0.25rem;
}
.timer-display {
  font-family: 'Oswald', sans-serif;
  font-size: 3rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--accent-orange);
  line-height: 1;
}
@media (max-width: 480px) {
  .timer-display { font-size: 2.25rem; }
}
</style>
