<template>
  <section class="rounded-2xl border border-slate-700 bg-slate-900/60 p-5 shadow-lg shadow-cyan-500/20">
    <header class="flex items-center justify-between">
      <div>
        <p class="text-sm uppercase tracking-[0.3em] text-slate-400">AI Journal</p>
        <h3 class="text-2xl font-semibold text-white">Session Feedback</h3>
      </div>
      <span class="text-sm text-teal-300">live</span>
    </header>
    <form class="mt-4 space-y-3">
      <textarea
        v-model="feedback"
        rows="3"
        placeholder="How did today feel?"
        class="w-full rounded-xl border border-slate-800 bg-slate-950/80 px-3 py-2 text-sm text-white placeholder:text-slate-500"
      ></textarea>
      <div class="flex gap-2">
        <button
          type="button"
          class="flex-1 rounded-xl bg-teal-500/90 px-4 py-2 text-sm font-semibold text-white transition hover:bg-teal-400"
        >
          Submit
        </button>
        <button
          type="button"
          class="rounded-xl border border-teal-500/80 px-4 py-2 text-sm font-semibold text-teal-200 hover:bg-teal-500/5"
        >
          Ask AI
        </button>
      </div>
    </form>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
const feedback = ref('')
</script>
