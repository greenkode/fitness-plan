<template>
  <div class="mx-auto flex max-w-6xl flex-col gap-8 px-4 py-8 sm:px-6 lg:px-0">
    <header class="space-y-3 rounded-3xl border border-white/10 bg-slate-900/70 p-6 shadow-2xl shadow-cyan-500/10">
      <p class="text-xs uppercase tracking-[0.4em] text-teal-300">krachtix</p>
      <h1 class="text-4xl font-semibold text-white">Adaptive fitness programming with AI insight</h1>
      <p class="text-base text-slate-300">
        Log sets, capture feedback, and let the coach recommend adjustments without leaving the chat.
      </p>
    </header>

    <FeedbackCard />

    <section class="space-y-5">
      <div class="flex items-center justify-between">
        <div>
          <p class="text-sm uppercase tracking-[0.4em] text-slate-500">Weekly focus</p>
          <h2 class="text-xl font-semibold text-white">Strength + Recovery</h2>
        </div>
        <span class="text-sm text-teal-300">4 sessions · dynamic RPE</span>
      </div>
      <div class="grid gap-5 md:grid-cols-2">
        <CalendarCard v-for="day in program" :key="day.date" :day="day" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import FeedbackCard from '~/components/FeedbackCard.vue'
import CalendarCard from '~/components/CalendarCard.vue'
import { program } from '~/data/program'
</script>
