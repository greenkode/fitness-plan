import { pgTable, uuid, text, boolean, jsonb, timestamp } from 'drizzle-orm/pg-core'
import { users } from './users'

export const coachingEvents = pgTable('coaching_events', {
  id: uuid('id').defaultRandom().primaryKey(),
  userId: uuid('user_id').notNull().references(() => users.id, { onDelete: 'cascade' }),
  eventType: text('event_type').notNull(),
  details: jsonb('details'),
  suggestedAt: timestamp('suggested_at', { withTimezone: true }).defaultNow().notNull(),
  accepted: boolean('accepted'),
  resolvedAt: timestamp('resolved_at', { withTimezone: true }),
})
