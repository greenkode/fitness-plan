<template>
  <div class="calendar">
    <div class="calendar-toolbar">
      <div class="view-toggle">
        <button class="toggle-btn" :class="{ active: view === 'week' }" @click="view = 'week'">Week</button>
        <button class="toggle-btn" :class="{ active: view === 'month' }" @click="view = 'month'">Month</button>
      </div>

      <div class="nav-controls">
        <button class="nav-btn" @click="navigate(-1)" aria-label="Previous">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6" /></svg>
        </button>
        <button class="today-btn" @click="goToday">Today</button>
        <button class="nav-btn" @click="navigate(1)" aria-label="Next">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6" /></svg>
        </button>
      </div>
    </div>

    <div class="period-label">{{ periodLabel }}</div>

    <div class="day-headers">
      <div v-for="d in dayHeaders" :key="d" class="day-header">{{ d }}</div>
    </div>

    <div class="calendar-grid" :class="view">
      <div
        v-for="cell in cells"
        :key="cell.dateKey"
        class="cell"
        :class="{
          'in-month': cell.inMonth,
          'is-today': cell.isToday,
          'has-workout': cell.assignment,
          'is-rest': cell.assignment?.workoutType === 'rest',
        }"
        @click="onClickCell(cell)"
      >
        <span class="day-num">{{ cell.dayNum }}</span>
        <div v-if="cell.assignment && cell.assignment.workoutType !== 'rest'" class="cell-workout">
          <span class="workout-pill" :class="'wt-' + cell.assignment.workoutType">{{ cell.assignment.workoutType }}</span>
          <span v-if="view === 'week'" class="workout-title">{{ cell.assignment.title }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Assignment {
  date: string
  title: string
  workoutType: string
}

const props = defineProps<{ assignments: Assignment[] }>()

const view = ref<'week' | 'month'>('week')
const cursor = ref(new Date())

const dayHeaders = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']

const todayStr = computed(() => new Date().toISOString().split('T')[0])

const assignmentMap = computed(() => {
  const map = new Map<string, Assignment>()
  for (const a of props.assignments) map.set(a.date, a)
  return map
})

interface Cell {
  dateKey: string
  dayNum: number
  inMonth: boolean
  isToday: boolean
  date: Date
  assignment: Assignment | null
}

const cells = computed<Cell[]>(() => {
  const cur = new Date(cursor.value)
  const result: Cell[] = []

  if (view.value === 'week') {
    const start = new Date(cur)
    start.setDate(cur.getDate() - cur.getDay())

    for (let i = 0; i < 7; i++) {
      const d = new Date(start)
      d.setDate(start.getDate() + i)
      const key = d.toISOString().split('T')[0]
      result.push({
        dateKey: key,
        dayNum: d.getDate(),
        inMonth: true,
        isToday: key === todayStr.value,
        date: d,
        assignment: assignmentMap.value.get(key) || null,
      })
    }
  } else {
    const monthStart = new Date(cur.getFullYear(), cur.getMonth(), 1)
    const gridStart = new Date(monthStart)
    gridStart.setDate(1 - monthStart.getDay())

    for (let i = 0; i < 42; i++) {
      const d = new Date(gridStart)
      d.setDate(gridStart.getDate() + i)
      const key = d.toISOString().split('T')[0]
      result.push({
        dateKey: key,
        dayNum: d.getDate(),
        inMonth: d.getMonth() === cur.getMonth(),
        isToday: key === todayStr.value,
        date: d,
        assignment: assignmentMap.value.get(key) || null,
      })
    }
  }

  return result
})

const periodLabel = computed(() => {
  const cur = cursor.value
  if (view.value === 'week') {
    const start = new Date(cur)
    start.setDate(cur.getDate() - cur.getDay())
    const end = new Date(start)
    end.setDate(start.getDate() + 6)
    const sameMonth = start.getMonth() === end.getMonth()
    if (sameMonth) {
      return `${start.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })} – ${end.getDate()}, ${end.getFullYear()}`
    }
    return `${start.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })} – ${end.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}, ${end.getFullYear()}`
  }
  return cur.toLocaleDateString('en-US', { month: 'long', year: 'numeric' })
})

function navigate(direction: number) {
  const d = new Date(cursor.value)
  if (view.value === 'week') d.setDate(d.getDate() + 7 * direction)
  else d.setMonth(d.getMonth() + direction)
  cursor.value = d
}

function goToday() {
  cursor.value = new Date()
}

function onClickCell(cell: Cell) {
  if (!cell.inMonth && view.value === 'month') {
    cursor.value = cell.date
    return
  }
  navigateTo(`/workout/${cell.dateKey}`)
}
</script>

<style scoped>
.calendar {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
  padding: 1rem;
  margin-bottom: 1.5rem;
}
.calendar-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
  gap: 0.5rem;
}
.view-toggle {
  display: flex;
  background: var(--section-bg);
  border-radius: 8px;
  padding: 2px;
}
.toggle-btn {
  background: none;
  border: none;
  padding: 0.4rem 0.85rem;
  font-family: 'Oswald', sans-serif;
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-muted);
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
}
.toggle-btn.active {
  background: var(--accent-orange);
  color: #fff;
}
.nav-controls {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}
.nav-btn {
  width: 32px;
  height: 32px;
  background: var(--section-bg);
  border: 1px solid var(--border-subtle);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--text-secondary);
  transition: all 0.2s;
}
.nav-btn:hover {
  border-color: var(--accent-orange);
  color: var(--accent-orange);
}
.today-btn {
  background: none;
  border: 1px solid var(--border-subtle);
  border-radius: 6px;
  padding: 0.35rem 0.75rem;
  font-family: 'Oswald', sans-serif;
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}
.today-btn:hover {
  border-color: var(--accent-orange);
  color: var(--accent-orange);
}

.period-label {
  font-family: 'Oswald', sans-serif;
  font-size: 1rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-primary);
  text-align: center;
  margin-bottom: 0.75rem;
}

.day-headers {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
  margin-bottom: 4px;
}
.day-header {
  font-family: 'Oswald', sans-serif;
  font-size: 0.65rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--text-muted);
  text-align: center;
  padding: 0.4rem 0;
  font-weight: 600;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}
.cell {
  aspect-ratio: 1;
  background: var(--section-bg);
  border: 1px solid var(--border-subtle);
  border-radius: 6px;
  padding: 0.4rem;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  transition: all 0.2s;
  min-height: 50px;
}
.calendar-grid.week .cell {
  aspect-ratio: auto;
  min-height: 100px;
}
.cell:hover {
  border-color: var(--accent-orange);
  transform: translateY(-1px);
}
.cell:not(.in-month) {
  opacity: 0.35;
}
.cell.is-today {
  border-color: var(--accent-orange);
  background: rgba(232, 93, 37, 0.08);
}
:global(html.dark) .cell.is-today {
  background: rgba(255, 107, 53, 0.12);
}
.cell.is-rest {
  opacity: 0.6;
}

.day-num {
  font-family: 'Oswald', sans-serif;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--text-primary);
}
.is-today .day-num {
  color: var(--accent-orange);
}

.cell-workout {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  flex: 1;
  min-height: 0;
}
.workout-pill {
  display: inline-block;
  align-self: flex-start;
  font-size: 0.55rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  padding: 0.1rem 0.35rem;
  border-radius: 3px;
  font-weight: 600;
  font-family: 'Oswald', sans-serif;
}
.wt-gym { background: var(--accent-orange); color: #000; }
.wt-cardio { background: var(--accent-green); color: #000; }
.wt-recovery { background: var(--accent-purple); color: #fff; }
.workout-title {
  font-size: 0.7rem;
  color: var(--text-secondary);
  line-height: 1.2;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

@media (max-width: 600px) {
  .calendar { padding: 0.75rem; }
  .calendar-grid.week .cell { min-height: 70px; }
  .cell { padding: 0.3rem; min-height: 44px; }
  .day-num { font-size: 0.75rem; }
  .workout-pill { font-size: 0.5rem; padding: 0.08rem 0.3rem; }
  .day-header { font-size: 0.55rem; }
}
</style>
