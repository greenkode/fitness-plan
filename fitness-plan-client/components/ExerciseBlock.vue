<template>
  <div class="exercise-block">
    <div class="exercise-header">
      <h3 class="exercise-name">{{ exercise.exerciseName }}</h3>
      <span v-if="prescription" class="exercise-rx">{{ prescription }}</span>
    </div>
    <div class="sets-header">
      <span class="col-num">#</span>
      <span class="col-weight">KG</span>
      <span class="col-reps">Reps</span>
      <span class="col-rpe">RPE</span>
      <span class="col-action"></span>
    </div>
    <div class="sets-list">
      <SetInput
        v-for="set in exercise.sets"
        :key="set.setNumber"
        :set-number="set.setNumber"
        :is-active="!set.completed && isNextSet(set.setNumber)"
        :initial-weight="set.weightKg || previousWeight"
        :initial-reps="set.reps"
        :initial-rpe="set.rpe"
        :completed="set.completed"
        @complete="(data) => $emit('set-completed', { setNumber: set.setNumber, ...data })"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ExerciseLog } from '~/types/workout'

const props = defineProps<{
  exercise: ExerciseLog
  prescription?: string | null
}>()

defineEmits<{
  'set-completed': [{ setNumber: number; weightKg: number; reps: number; rpe: number }]
}>()

const previousWeight = computed(() => {
  const completed = props.exercise.sets.filter(s => s.completed && s.weightKg)
  return completed.length > 0 ? completed[completed.length - 1].weightKg : null
})

function isNextSet(setNumber: number): boolean {
  const firstIncomplete = props.exercise.sets.find(s => !s.completed)
  return firstIncomplete?.setNumber === setNumber
}
</script>

<style scoped>
.exercise-block {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
  padding: 1.25rem;
  margin-bottom: 1rem;
  box-shadow: 0 2px 8px var(--shadow-color);
}
.exercise-header {
  margin-bottom: 1rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid var(--border-subtle);
}
.exercise-name {
  font-family: 'Oswald', sans-serif;
  font-size: 1.1rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-primary);
  margin: 0;
}
.exercise-rx {
  font-size: 0.8rem;
  color: var(--accent-orange);
  margin-top: 0.25rem;
  display: block;
}
.sets-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0 0.5rem 0.5rem;
  font-size: 0.65rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--text-muted);
  font-weight: 600;
}
.col-num { width: 24px; text-align: center; }
.col-weight { width: 65px; text-align: center; }
.col-reps { width: 55px; text-align: center; }
.col-rpe { width: 55px; text-align: center; }
.col-action { width: 36px; }
.sets-list {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
@media (max-width: 480px) {
  .col-weight { width: 55px; }
  .col-reps { width: 45px; }
  .col-rpe { width: 45px; }
}
</style>
