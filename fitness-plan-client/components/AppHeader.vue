<template>
  <header class="app-header">
    <div class="header-actions">
      <ClientOnly><ThemeToggle /></ClientOnly>
      <UDropdownMenu v-if="loggedIn" :items="userMenuItems">
        <button class="avatar-btn">
          {{ userInitial }}
        </button>
      </UDropdownMenu>
      <NuxtLink v-else to="/login" class="login-btn">Login</NuxtLink>
    </div>
    <NuxtLink to="/" class="brand-link"><h1 class="brand">Krachtix</h1></NuxtLink>
    <p class="tagline">Adaptive fitness programming with AI insight</p>
    <nav v-if="loggedIn" class="nav-bar">
      <NuxtLink to="/" class="nav-link" :class="{ active: route.path === '/' }">Calendar</NuxtLink>
      <NuxtLink to="/history" class="nav-link" :class="{ active: route.path === '/history' }">History</NuxtLink>
      <NuxtLink to="/programs" class="nav-link" :class="{ active: route.path.startsWith('/programs') }">Programs</NuxtLink>
    </nav>
  </header>
</template>

<script setup lang="ts">
import { useWorkoutStore } from '~/stores/workout'

const { loggedIn, user, clear } = useUserSession()
const route = useRoute()

const userInitial = computed(() => {
  const name = user.value?.name || user.value?.email || '?'
  return name.charAt(0).toUpperCase()
})

const userMenuItems = computed(() => [
  [{ label: user.value?.name || user.value?.email || 'User', disabled: true }],
  [{ label: 'History', icon: 'i-lucide-history', onSelect: () => navigateTo('/history') }],
  [{ label: 'Settings', icon: 'i-lucide-settings', onSelect: () => navigateTo('/settings') }],
  [{
    label: 'Logout',
    icon: 'i-lucide-log-out',
    onSelect: async () => {
      const workoutStore = useWorkoutStore()
      await $fetch('/api/auth/logout', { method: 'POST' })
      await clear()
      workoutStore.resetWorkout()
      navigateTo('/login')
    },
  }],
])
</script>

<style scoped>
.app-header {
  position: relative;
  padding: 2rem;
  text-align: center;
  background: linear-gradient(180deg, rgba(232, 93, 37, 0.08) 0%, transparent 100%);
  border-bottom: 1px solid var(--border-subtle);
}
:global(html.dark) .app-header {
  background: linear-gradient(180deg, rgba(255, 107, 53, 0.1) 0%, transparent 100%);
}
.header-actions {
  position: absolute;
  top: 1.5rem;
  right: 1.5rem;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.brand-link {
  text-decoration: none;
}
.brand {
  font-family: 'Oswald', sans-serif;
  font-size: 2.5rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  background: linear-gradient(135deg, var(--accent-orange), var(--accent-yellow));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 0.5rem;
  line-height: 1.2;
}
.tagline {
  color: var(--text-secondary);
  font-size: 1.1rem;
  margin-bottom: 0;
}
.nav-bar {
  display: flex;
  justify-content: center;
  gap: 0.25rem;
  margin-top: 1.25rem;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: 8px;
  padding: 0.25rem;
  display: inline-flex;
}
.nav-link {
  font-family: 'Oswald', sans-serif;
  font-size: 0.85rem;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  text-decoration: none;
  color: var(--text-muted);
  padding: 0.5rem 1.25rem;
  border-radius: 6px;
  transition: all 0.2s ease;
}
.nav-link:hover {
  color: var(--text-primary);
}
.nav-link.active {
  background: var(--accent-orange);
  color: #fff;
}
.stats-bar {
  display: flex;
  justify-content: center;
  gap: 2rem;
  margin-top: 1.5rem;
  flex-wrap: wrap;
}
.stat-box {
  text-align: center;
  padding: 0.75rem 1.5rem;
  background: var(--bg-card);
  border-radius: 8px;
  border: 1px solid var(--border-subtle);
  box-shadow: 0 2px 8px var(--shadow-color);
}
.stat-value {
  display: block;
  font-family: 'Oswald', sans-serif;
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--accent-orange);
}
.stat-label {
  display: block;
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--text-muted);
}
.avatar-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--accent-orange);
  color: #fff;
  border: none;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s ease;
}
.avatar-btn:hover {
  transform: scale(1.05);
}
.login-btn {
  background: var(--accent-orange);
  color: #fff;
  font-family: 'Oswald', sans-serif;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  border: none;
  border-radius: 8px;
  padding: 0.6rem 1.2rem;
  font-size: 0.85rem;
  cursor: pointer;
  text-decoration: none;
  transition: all 0.2s ease;
}
.login-btn:hover {
  filter: brightness(1.1);
  transform: translateY(-1px);
}
@media (max-width: 768px) {
  .app-header {
    padding: 1.5rem 1rem;
  }
  .brand {
    font-size: 1.75rem;
    padding-right: 3rem;
  }
  .tagline {
    font-size: 0.9rem;
  }
  .header-actions {
    top: 1rem;
    right: 1rem;
  }
  .stats-bar {
    gap: 0.75rem;
  }
  .stat-box {
    padding: 0.5rem 1rem;
  }
  .stat-value {
    font-size: 1.1rem;
  }
  .stat-label {
    font-size: 0.65rem;
  }
}
@media (max-width: 480px) {
  .stats-bar {
    flex-direction: column;
    align-items: stretch;
  }
  .stat-box {
    display: flex;
    justify-content: space-between;
    align-items: center;
    text-align: left;
  }
}
</style>
