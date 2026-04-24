import { pgTable, uuid, text, integer, real, boolean, date, timestamp } from 'drizzle-orm/pg-core'
import { users } from './users'
import { workoutAssignments } from './assignments'
import { templateExercises } from './templates'

export const workoutLogs = pgTable('workout_logs', {
  id: uuid('id').defaultRandom().primaryKey(),
  userId: uuid('user_id').notNull().references(() => users.id, { onDelete: 'cascade' }),
  assignmentId: uuid('assignment_id').references(() => workoutAssignments.id),
  workoutDate: date('workout_date').notNull(),
  startedAt: timestamp('started_at', { withTimezone: true }),
  completedAt: timestamp('completed_at', { withTimezone: true }),
  notes: text('notes'),
  overallRpe: real('overall_rpe'),
})

export const exerciseLogs = pgTable('exercise_logs', {
  id: uuid('id').defaultRandom().primaryKey(),
  workoutLogId: uuid('workout_log_id').notNull().references(() => workoutLogs.id, { onDelete: 'cascade' }),
  templateExerciseId: uuid('template_exercise_id').references(() => templateExercises.id),
  exerciseName: text('exercise_name').notNull(),
  sortOrder: integer('sort_order').notNull().default(0),
})

export const setLogs = pgTable('set_logs', {
  id: uuid('id').defaultRandom().primaryKey(),
  exerciseLogId: uuid('exercise_log_id').notNull().references(() => exerciseLogs.id, { onDelete: 'cascade' }),
  setNumber: integer('set_number').notNull(),
  weightKg: real('weight_kg'),
  reps: integer('reps'),
  durationSeconds: integer('duration_seconds'),
  rpe: real('rpe'),
  completed: boolean('completed').notNull().default(true),
  notes: text('notes'),
})
