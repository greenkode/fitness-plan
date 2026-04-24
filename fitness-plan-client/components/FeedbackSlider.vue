<template>
  <div class="slider-group">
    <div class="slider-header">
      <label class="slider-label">{{ label }}</label>
      <span class="slider-value">{{ modelValue || '—' }}</span>
    </div>
    <input
      type="range"
      :min="min"
      :max="max"
      :value="modelValue || min"
      class="slider-input"
      @input="$emit('update:modelValue', Number(($event.target as HTMLInputElement).value))"
    />
    <div class="slider-range">
      <span>{{ minLabel || min }}</span>
      <span>{{ maxLabel || max }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  label: string
  modelValue: number | null
  min?: number
  max?: number
  minLabel?: string
  maxLabel?: string
}>()

defineEmits<{
  'update:modelValue': [value: number]
}>()
</script>

<style scoped>
.slider-group {
  margin-bottom: 1.25rem;
}
.slider-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}
.slider-label {
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--text-primary);
}
.slider-value {
  font-family: 'Oswald', sans-serif;
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--accent-orange);
  min-width: 28px;
  text-align: right;
}
.slider-input {
  width: 100%;
  height: 6px;
  -webkit-appearance: none;
  appearance: none;
  background: var(--border-subtle);
  border-radius: 3px;
  outline: none;
}
.slider-input::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--accent-orange);
  cursor: pointer;
  border: 2px solid var(--bg-card);
  box-shadow: 0 2px 6px var(--shadow-color);
}
.slider-input::-moz-range-thumb {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--accent-orange);
  cursor: pointer;
  border: 2px solid var(--bg-card);
}
.slider-range {
  display: flex;
  justify-content: space-between;
  font-size: 0.7rem;
  color: var(--text-muted);
  margin-top: 0.25rem;
}
</style>
