import { pgTable, uuid, text, integer, date, timestamp } from 'drizzle-orm/pg-core'
import { users } from './users'
import { workoutLogs } from './tracking'

export const sessionFeedback = pgTable('session_feedback', {
  id: uuid('id').defaultRandom().primaryKey(),
  userId: uuid('user_id').notNull().references(() => users.id, { onDelete: 'cascade' }),
  workoutLogId: uuid('workout_log_id').references(() => workoutLogs.id),
  feedbackDate: date('feedback_date').notNull(),
  fatigueLevel: integer('fatigue_level'),
  sorenessLevel: integer('soreness_level'),
  motivationLevel: integer('motivation_level'),
  sleepQuality: integer('sleep_quality'),
  stressLevel: integer('stress_level'),
  freeText: text('free_text'),
  createdAt: timestamp('created_at', { withTimezone: true }).defaultNow().notNull(),
})
