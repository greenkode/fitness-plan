<template>
  <div v-if="isResting" class="rest-timer">
    <span class="rest-label">Rest</span>
    <span class="rest-time">{{ formatted }}</span>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{ isResting: boolean; lastSetAt: string | null }>()

const seconds = ref(0)
let interval: ReturnType<typeof setInterval> | null = null

function update() {
  if (!props.lastSetAt || !props.isResting) { seconds.value = 0; return }
  seconds.value = Math.floor((Date.now() - new Date(props.lastSetAt).getTime()) / 1000)
}

watch(() => props.isResting, (val) => {
  if (val) {
    update()
    interval = setInterval(update, 1000)
  } else if (interval) {
    clearInterval(interval)
    interval = null
  }
}, { immediate: true })

onUnmounted(() => { if (interval) clearInterval(interval) })

const formatted = computed(() => {
  const m = Math.floor(seconds.value / 60)
  const s = seconds.value % 60
  return `${m}:${String(s).padStart(2, '0')}`
})
</script>

<style scoped>
.rest-timer {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 1rem;
  background: var(--section-bg);
  border-radius: 8px;
  border-left: 3px solid var(--accent-yellow);
  margin: 0.5rem 0;
}
.rest-label {
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--accent-yellow);
  font-weight: 600;
}
.rest-time {
  font-family: 'Oswald', sans-serif;
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-primary);
}
</style>
