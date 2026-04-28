<template>
  <div class="krachtix-shell">
    <AppHeader />
    <main class="krachtix-main">
      <slot />
    </main>
  </div>
</template>

<style scoped>
.krachtix-shell {
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  background: var(--bg-app);
  color: var(--text-primary);
}
.krachtix-main {
  flex: 1;
  max-width: 800px;
  width: 100%;
  margin: 0 auto;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
}
@media (max-width: 768px) {
  .krachtix-main {
    padding: 1rem;
  }
}
@media (max-width: 480px) {
  .krachtix-main {
    padding: 0.75rem;
  }
}
</style>
