<template>
  <article class="rounded-2xl border border-white/10 bg-white/5 p-5 shadow-lg shadow-teal-500/10">
    <p class="text-sm text-slate-400">{{ day.label }}</p>
    <h2 class="mt-2 text-xl font-semibold text-white">{{ day.focus }} · {{ day.movement }}</h2>
    <p class="text-xs uppercase tracking-widest text-teal-300">{{ formattedDate }}</p>
    <dl class="mt-4 grid gap-3">
      <template v-for="(set, idx) in day.sets" :key="idx">
        <div class="flex items-center justify-between text-sm">
          <span>Set {{ idx + 1 }}</span>
          <span>{{ set.weight }} kg · {{ set.reps }} @ RPE {{ set.rpe }}</span>
        </div>
      </template>
    </dl>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { WorkoutDay } from '~/data/program'
import { useCalendar } from '~/composables/useCalendar'

const props = defineProps<{ day: WorkoutDay }>()
const { formatDay } = useCalendar()
const formattedDate = computed(() => formatDay(props.day.date))
</script>
