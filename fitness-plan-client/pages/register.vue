<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2 class="auth-title">Get Started</h2>
      <p class="auth-subtitle">Create your Krachtix account</p>

      <form class="auth-form" @submit.prevent="register">
        <div class="form-group">
          <label class="form-label">Name</label>
          <input v-model="name" type="text" class="form-input" placeholder="Your name" />
        </div>
        <div class="form-group">
          <label class="form-label">Email</label>
          <input v-model="email" type="email" class="form-input" placeholder="you@example.com" required />
        </div>
        <div class="form-group">
          <label class="form-label">Password</label>
          <input v-model="password" type="password" class="form-input" placeholder="Min 8 characters" required minlength="8" />
        </div>

        <p v-if="error" class="form-error">{{ error }}</p>

        <button type="submit" class="submit-btn" :disabled="loading">
          {{ loading ? 'Creating account...' : 'Create Account' }}
        </button>
      </form>

      <p class="auth-footer">
        Already have an account? <NuxtLink to="/login" class="auth-link">Sign in</NuxtLink>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
const { fetch: refreshSession } = useUserSession()

const name = ref('')
const email = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function register() {
  error.value = ''
  loading.value = true
  try {
    await $fetch('/api/auth/register', {
      method: 'POST',
      body: { name: name.value || undefined, email: email.value, password: password.value },
    })
    await refreshSession()
    navigateTo('/')
  } catch (e: any) {
    error.value = e.data?.statusMessage || 'Registration failed'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60vh;
}
.auth-card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: 16px;
  padding: 2.5rem;
  width: 100%;
  max-width: 420px;
  box-shadow: 0 4px 24px var(--shadow-color);
}
.auth-title {
  font-family: 'Oswald', sans-serif;
  font-size: 1.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  background: linear-gradient(135deg, var(--accent-orange), var(--accent-yellow));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 0.25rem;
}
.auth-subtitle {
  color: var(--text-secondary);
  font-size: 0.95rem;
  margin-bottom: 2rem;
}
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}
.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}
.form-label {
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--text-muted);
  font-weight: 600;
}
.form-input {
  background: var(--section-bg);
  border: 1px solid var(--border-subtle);
  border-radius: 8px;
  padding: 0.75rem 1rem;
  font-size: 0.95rem;
  color: var(--text-primary);
  font-family: 'Source Sans 3', sans-serif;
  transition: border-color 0.2s ease;
  outline: none;
}
.form-input:focus {
  border-color: var(--accent-orange);
}
.form-input::placeholder {
  color: var(--text-muted);
}
.form-error {
  color: #ef4444;
  font-size: 0.85rem;
  margin: 0;
}
.submit-btn {
  background: var(--accent-orange);
  color: #fff;
  font-family: 'Oswald', sans-serif;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  border: none;
  border-radius: 8px;
  padding: 0.85rem 1.5rem;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-top: 0.5rem;
}
.submit-btn:hover:not(:disabled) {
  filter: brightness(1.1);
  transform: translateY(-1px);
}
.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.auth-footer {
  text-align: center;
  margin-top: 1.5rem;
  font-size: 0.9rem;
  color: var(--text-secondary);
}
.auth-link {
  color: var(--accent-orange);
  text-decoration: none;
  font-weight: 600;
}
.auth-link:hover {
  text-decoration: underline;
}
</style>
