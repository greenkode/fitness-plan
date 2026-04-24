import { pgTable, uuid, text, integer, timestamp, jsonb, real, uniqueIndex } from 'drizzle-orm/pg-core'
import { users } from './users'
import { programTemplates, templatePhases } from './templates'

export const userProfiles = pgTable('user_profiles', {
  id: uuid('id').defaultRandom().primaryKey(),
  userId: uuid('user_id').notNull().references(() => users.id, { onDelete: 'cascade' }),
  experienceLevel: text('experience_level').notNull().default('beginner'),
  goals: jsonb('goals'),
  availableEquipment: jsonb('available_equipment'),
  bodyWeightKg: real('body_weight_kg'),
  createdAt: timestamp('created_at', { withTimezone: true }).defaultNow().notNull(),
  updatedAt: timestamp('updated_at', { withTimezone: true }).defaultNow().notNull(),
}, (table) => [
  uniqueIndex('user_profiles_user_id_idx').on(table.userId),
])

export const userSchedules = pgTable('user_schedules', {
  id: uuid('id').defaultRandom().primaryKey(),
  userId: uuid('user_id').notNull().references(() => users.id, { onDelete: 'cascade' }),
  dayOfWeek: integer('day_of_week').notNull(),
  trainingType: text('training_type').notNull(),
}, (table) => [
  uniqueIndex('user_schedules_user_day_idx').on(table.userId, table.dayOfWeek),
])

export const userPrograms = pgTable('user_programs', {
  id: uuid('id').defaultRandom().primaryKey(),
  userId: uuid('user_id').notNull().references(() => users.id, { onDelete: 'cascade' }),
  templateId: uuid('template_id').notNull().references(() => programTemplates.id),
  startedAt: timestamp('started_at', { withTimezone: true }).defaultNow().notNull(),
  currentPhaseId: uuid('current_phase_id').references(() => templatePhases.id),
  status: text('status').notNull().default('active'),
  pausedAt: timestamp('paused_at', { withTimezone: true }),
  completedAt: timestamp('completed_at', { withTimezone: true }),
})

export const userAiSettings = pgTable('user_ai_settings', {
  id: uuid('id').defaultRandom().primaryKey(),
  userId: uuid('user_id').notNull().references(() => users.id, { onDelete: 'cascade' }),
  provider: text('provider').notNull().default('ollama'),
  modelId: text('model_id'),
  apiKeyEnc: text('api_key_enc'),
  baseUrl: text('base_url'),
  createdAt: timestamp('created_at', { withTimezone: true }).defaultNow().notNull(),
  updatedAt: timestamp('updated_at', { withTimezone: true }).defaultNow().notNull(),
}, (table) => [
  uniqueIndex('user_ai_settings_user_id_idx').on(table.userId),
])
