<template>
  <div class="feedback-card">
    <h2 class="feedback-title">How Did It Feel?</h2>
    <p class="feedback-subtitle">Rate your session to help the AI coach adjust your program</p>

    <FeedbackSlider v-model="fatigue" label="Fatigue" :min="1" :max="10" min-label="Fresh" max-label="Exhausted" />
    <FeedbackSlider v-model="soreness" label="Soreness" :min="1" :max="10" min-label="None" max-label="Very sore" />
    <FeedbackSlider v-model="motivation" label="Motivation" :min="1" :max="10" min-label="Low" max-label="Fired up" />
    <FeedbackSlider v-model="sleepQuality" label="Sleep Quality" :min="1" :max="10" min-label="Terrible" max-label="Perfect" />
    <FeedbackSlider v-model="stress" label="Stress Level" :min="1" :max="10" min-label="Calm" max-label="Very stressed" />

    <div class="freetext-group">
      <label class="freetext-label">Notes (optional)</label>
      <textarea v-model="freeText" class="freetext-input" rows="3" placeholder="Left shoulder felt tight, energy was low..." />
    </div>

    <button class="submit-btn" :disabled="loading" @click="submit">
      {{ loading ? 'Submitting...' : 'Submit Feedback' }}
    </button>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{ workoutLogId: string; workoutDate: string }>()
const emit = defineEmits<{ submitted: [] }>()

const fatigue = ref<number | null>(5)
const soreness = ref<number | null>(5)
const motivation = ref<number | null>(5)
const sleepQuality = ref<number | null>(5)
const stress = ref<number | null>(5)
const freeText = ref('')
const loading = ref(false)
const toast = useToast()

async function submit() {
  loading.value = true
  try {
    await $fetch('/api/fitness/feedback', {
      method: 'POST',
      body: {
        workoutLogId: props.workoutLogId,
        feedbackDate: props.workoutDate,
        fatigueLevel: fatigue.value,
        sorenessLevel: soreness.value,
        motivationLevel: motivation.value,
        sleepQuality: sleepQuality.value,
        stressLevel: stress.value,
        freeText: freeText.value || undefined,
      },
    })
    emit('submitted')
  } catch {
    toast.add({ title: 'Failed to submit feedback', description: 'Please try again.', color: 'error' })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.feedback-card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: 16px;
  padding: 2rem;
  max-width: 520px;
  margin: 0 auto;
  box-shadow: 0 4px 24px var(--shadow-color);
}
.feedback-title {
  font-family: 'Oswald', sans-serif;
  font-size: 1.5rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-primary);
  margin-bottom: 0.25rem;
}
.feedback-subtitle {
  color: var(--text-secondary);
  font-size: 0.9rem;
  margin-bottom: 2rem;
}
.freetext-group {
  margin-top: 0.5rem;
  margin-bottom: 1.5rem;
}
.freetext-label {
  display: block;
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 0.5rem;
}
.freetext-input {
  width: 100%;
  background: var(--section-bg);
  border: 1px solid var(--border-subtle);
  border-radius: 8px;
  padding: 0.75rem;
  font-size: 0.9rem;
  color: var(--text-primary);
  font-family: 'Source Sans 3', sans-serif;
  resize: vertical;
  outline: none;
}
.freetext-input:focus {
  border-color: var(--accent-orange);
}
.freetext-input::placeholder {
  color: var(--text-muted);
}
.submit-btn {
  width: 100%;
  background: linear-gradient(135deg, var(--accent-orange), #ff8c5a);
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
.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(232, 93, 37, 0.4);
}
.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
