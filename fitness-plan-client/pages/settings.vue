<template>
  <div class="settings-page">
    <h2 class="page-title">Settings</h2>

    <section class="card">
      <h3 class="section-title">Profile</h3>
      <p class="section-desc">Used for accurate calorie estimates and program tailoring.</p>

      <div class="form-row">
        <label class="form-label">Body Weight (kg)</label>
        <input
          v-model.number="bodyWeightKg"
          type="number"
          step="0.1"
          class="form-input"
          placeholder="e.g. 75"
        />
      </div>

      <div class="form-row">
        <label class="form-label">Experience Level</label>
        <select v-model="experienceLevel" class="form-input">
          <option value="beginner">Beginner</option>
          <option value="intermediate">Intermediate</option>
          <option value="advanced">Advanced</option>
        </select>
      </div>

      <button class="save-btn" :disabled="savingProfile" @click="saveProfile">
        {{ savingProfile ? 'Saving...' : 'Save Profile' }}
      </button>
    </section>

    <section class="card">
      <h3 class="section-title">AI Provider</h3>
      <p class="section-desc">Choose which AI handles your program creation and coaching.</p>

      <div class="form-row">
        <label class="form-label">Provider</label>
        <select v-model="aiProvider" class="form-input">
          <option value="ollama">Ollama (Local, Free)</option>
          <option value="anthropic">Anthropic (Claude)</option>
          <option value="openai">OpenAI</option>
          <option value="google">Google (Gemini)</option>
        </select>
      </div>

      <div class="form-row">
        <label class="form-label">Model</label>
        <input
          v-model="aiModel"
          type="text"
          class="form-input"
          :placeholder="modelPlaceholder"
        />
      </div>

      <div v-if="aiProvider !== 'ollama'" class="form-row">
        <label class="form-label">API Key {{ hasApiKey ? '(saved — leave blank to keep)' : '' }}</label>
        <input
          v-model="aiApiKey"
          type="password"
          class="form-input"
          :placeholder="hasApiKey ? '••••••••' : 'sk-...'"
        />
      </div>

      <div v-if="aiProvider === 'ollama'" class="form-row">
        <label class="form-label">Base URL (optional)</label>
        <input
          v-model="aiBaseUrl"
          type="text"
          class="form-input"
          placeholder="http://localhost:11434"
        />
      </div>

      <div class="actions-row">
        <button class="save-btn" :disabled="savingAi" @click="saveAi">
          {{ savingAi ? 'Saving...' : 'Save AI Settings' }}
        </button>
        <button v-if="hasApiKey" class="delete-btn" @click="clearApiKey">Clear API Key</button>
      </div>
    </section>

    <section class="card">
      <h3 class="section-title">Appearance</h3>
      <p class="section-desc">Light or dark mode (also available in the top-right toggle).</p>

      <div class="theme-buttons">
        <button
          class="theme-btn"
          :class="{ active: colorMode.preference === 'light' }"
          @click="colorMode.preference = 'light'"
        >Light</button>
        <button
          class="theme-btn"
          :class="{ active: colorMode.preference === 'dark' }"
          @click="colorMode.preference = 'dark'"
        >Dark</button>
        <button
          class="theme-btn"
          :class="{ active: colorMode.preference === 'system' }"
          @click="colorMode.preference = 'system'"
        >System</button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ middleware: 'auth' })

const toast = useToast()
const colorMode = useColorMode()

const bodyWeightKg = ref<number | null>(null)
const experienceLevel = ref('intermediate')
const savingProfile = ref(false)

const aiProvider = ref<'ollama' | 'anthropic' | 'openai' | 'google'>('ollama')
const aiModel = ref('')
const aiApiKey = ref('')
const aiBaseUrl = ref('')
const hasApiKey = ref(false)
const savingAi = ref(false)

const modelPlaceholder = computed(() => {
  if (aiProvider.value === 'ollama') return 'qwen2.5:14b'
  if (aiProvider.value === 'anthropic') return 'claude-sonnet-4-6'
  if (aiProvider.value === 'openai') return 'gpt-4o-mini'
  if (aiProvider.value === 'google') return 'gemini-2.0-flash'
  return ''
})

onMounted(async () => {
  try {
    const profile = await $fetch<any>('/api/fitness/profile')
    if (profile) {
      bodyWeightKg.value = profile.bodyWeightKg
      experienceLevel.value = profile.experienceLevel || 'intermediate'
    }
  } catch {}

  try {
    const ai = await $fetch<any>('/api/settings/ai')
    if (ai) {
      aiProvider.value = ai.provider
      aiModel.value = ai.modelId || ''
      aiBaseUrl.value = ai.baseUrl || ''
      hasApiKey.value = ai.hasApiKey
    }
  } catch {}
})

async function saveProfile() {
  savingProfile.value = true
  try {
    await $fetch('/api/fitness/profile', {
      method: 'PUT',
      body: { experienceLevel: experienceLevel.value, bodyWeightKg: bodyWeightKg.value || undefined },
    })
    toast.add({ title: 'Profile saved', color: 'success' })
  } catch (e: any) {
    toast.add({ title: 'Save failed', description: e.data?.statusMessage || 'Try again', color: 'error' })
  } finally {
    savingProfile.value = false
  }
}

async function saveAi() {
  savingAi.value = true
  try {
    await $fetch('/api/settings/ai', {
      method: 'PUT',
      body: {
        provider: aiProvider.value,
        modelId: aiModel.value || undefined,
        apiKey: aiApiKey.value || undefined,
        baseUrl: aiBaseUrl.value || undefined,
      },
    })
    toast.add({ title: 'AI settings saved', color: 'success' })
    aiApiKey.value = ''
    if (aiProvider.value !== 'ollama') hasApiKey.value = true
  } catch (e: any) {
    toast.add({ title: 'Save failed', description: e.data?.statusMessage || 'Try again', color: 'error' })
  } finally {
    savingAi.value = false
  }
}

async function clearApiKey() {
  try {
    await $fetch('/api/settings/ai', { method: 'DELETE' })
    hasApiKey.value = false
    toast.add({ title: 'API key cleared', color: 'neutral' })
  } catch {
    toast.add({ title: 'Failed to clear key', color: 'error' })
  }
}
</script>

<style scoped>
.settings-page {
  max-width: 600px;
  margin: 0 auto;
}
.page-title {
  font-family: 'Oswald', sans-serif;
  font-size: 1.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  background: linear-gradient(135deg, var(--accent-orange), var(--accent-yellow));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 1.5rem;
}
.card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
  padding: 1.5rem;
  margin-bottom: 1rem;
}
.section-title {
  font-family: 'Oswald', sans-serif;
  font-size: 1.1rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-primary);
  margin: 0 0 0.25rem;
}
.section-desc {
  font-size: 0.85rem;
  color: var(--text-secondary);
  margin: 0 0 1.25rem;
}
.form-row {
  margin-bottom: 1rem;
}
.form-label {
  display: block;
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--text-muted);
  font-weight: 600;
  margin-bottom: 0.4rem;
}
.form-input {
  width: 100%;
  background: var(--section-bg);
  border: 1px solid var(--border-subtle);
  border-radius: 8px;
  padding: 0.65rem 0.85rem;
  font-size: 0.95rem;
  color: var(--text-primary);
  font-family: 'Source Sans 3', sans-serif;
  outline: none;
  transition: border-color 0.2s;
  -moz-appearance: textfield;
}
.form-input::-webkit-outer-spin-button,
.form-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
.form-input:focus { border-color: var(--accent-orange); }

.actions-row {
  display: flex;
  gap: 0.5rem;
}
.save-btn {
  background: var(--accent-orange);
  color: #fff;
  font-family: 'Oswald', sans-serif;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-size: 0.85rem;
  border: none;
  border-radius: 8px;
  padding: 0.65rem 1.25rem;
  cursor: pointer;
  transition: all 0.2s;
}
.save-btn:hover:not(:disabled) { filter: brightness(1.1); }
.save-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.delete-btn {
  background: none;
  border: 1px solid #ef4444;
  color: #ef4444;
  border-radius: 8px;
  padding: 0.65rem 1rem;
  font-family: 'Oswald', sans-serif;
  font-weight: 600;
  text-transform: uppercase;
  font-size: 0.8rem;
  cursor: pointer;
  transition: all 0.2s;
}
.delete-btn:hover { background: rgba(239, 68, 68, 0.1); }

.theme-buttons {
  display: flex;
  gap: 0.5rem;
}
.theme-btn {
  flex: 1;
  background: var(--section-bg);
  border: 1px solid var(--border-subtle);
  color: var(--text-secondary);
  font-family: 'Oswald', sans-serif;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-size: 0.85rem;
  padding: 0.6rem;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.theme-btn.active {
  background: var(--accent-orange);
  color: #fff;
  border-color: var(--accent-orange);
}
.theme-btn:hover:not(.active) {
  border-color: var(--text-secondary);
  color: var(--text-primary);
}
</style>
