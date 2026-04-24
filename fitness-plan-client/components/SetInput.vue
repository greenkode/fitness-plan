<template>
  <div class="set-row" :class="{ active: isActive, completed: completed }">
    <span class="set-num">{{ setNumber }}</span>
    <input
      v-model.number="weight"
      type="number"
      step="0.5"
      class="set-input weight-input"
      placeholder="kg"
      :disabled="completed && !editing"
    />
    <input
      v-model.number="reps"
      type="number"
      class="set-input reps-input"
      placeholder="reps"
      :disabled="completed && !editing"
    />
    <input
      v-model.number="rpe"
      type="number"
      min="1"
      max="10"
      step="0.5"
      class="set-input rpe-input"
      placeholder="RPE"
      :disabled="completed && !editing"
    />
    <button
      v-if="!completed"
      class="check-btn"
      :disabled="!weight || !reps"
      @click="complete"
    >
      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="20 6 9 17 4 12" />
      </svg>
    </button>
    <span v-else class="done-mark">
      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="20 6 9 17 4 12" />
      </svg>
    </span>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  setNumber: number
  isActive: boolean
  initialWeight?: number | null
  initialReps?: number | null
  initialRpe?: number | null
  completed: boolean
}>()

const emit = defineEmits<{
  complete: [{ weightKg: number; reps: number; rpe: number }]
}>()

const weight = ref(props.initialWeight || null)
const reps = ref(props.initialReps || null)
const rpe = ref(props.initialRpe || null)
const editing = ref(false)

function complete() {
  if (!weight.value || !reps.value) return
  emit('complete', {
    weightKg: weight.value,
    reps: reps.value,
    rpe: rpe.value || 7,
  })
}
</script>

<style scoped>
.set-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem;
  border-radius: 8px;
  transition: all 0.2s ease;
}
.set-row.active {
  background: var(--section-bg);
  border: 1px solid var(--accent-orange);
}
.set-row.completed {
  opacity: 0.6;
}
.set-num {
  font-family: 'Oswald', sans-serif;
  font-weight: 600;
  font-size: 0.9rem;
  color: var(--text-muted);
  width: 24px;
  text-align: center;
  flex-shrink: 0;
}
.set-input {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: 6px;
  padding: 0.5rem;
  font-size: 0.9rem;
  color: var(--text-primary);
  font-family: 'Source Sans 3', sans-serif;
  text-align: center;
  outline: none;
  transition: border-color 0.2s;
  -moz-appearance: textfield;
}
.set-input::-webkit-outer-spin-button,
.set-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
.set-input:focus {
  border-color: var(--accent-orange);
}
.weight-input { width: 65px; }
.reps-input { width: 55px; }
.rpe-input { width: 55px; }
.check-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--accent-orange);
  color: #fff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
  padding: 0;
}
.check-btn:hover:not(:disabled) {
  filter: brightness(1.1);
  transform: scale(1.05);
}
.check-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}
.done-mark {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent-green);
  flex-shrink: 0;
}
@media (max-width: 480px) {
  .weight-input { width: 55px; }
  .reps-input { width: 45px; }
  .rpe-input { width: 45px; }
  .set-input { padding: 0.4rem; font-size: 0.85rem; }
}
</style>
