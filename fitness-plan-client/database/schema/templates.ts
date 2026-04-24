import { pgTable, uuid, text, integer, boolean, jsonb, uniqueIndex } from 'drizzle-orm/pg-core'
import { users } from './users'

export const programTemplates = pgTable('program_templates', {
  id: uuid('id').defaultRandom().primaryKey(),
  name: text('name').notNull(),
  description: text('description'),
  category: text('category'),
  difficultyLevel: text('difficulty_level'),
  isSystem: boolean('is_system').notNull().default(false),
  createdBy: uuid('created_by').references(() => users.id),
})

export const templatePhases = pgTable('template_phases', {
  id: uuid('id').defaultRandom().primaryKey(),
  templateId: uuid('template_id').notNull().references(() => programTemplates.id, { onDelete: 'cascade' }),
  phaseNumber: integer('phase_number').notNull(),
  name: text('name').notNull(),
  theme: text('theme'),
  description: text('description'),
  progressionCriteria: jsonb('progression_criteria'),
}, (table) => [
  uniqueIndex('template_phases_template_phase_idx').on(table.templateId, table.phaseNumber),
])

export const templateWorkouts = pgTable('template_workouts', {
  id: uuid('id').defaultRandom().primaryKey(),
  phaseId: uuid('phase_id').notNull().references(() => templatePhases.id, { onDelete: 'cascade' }),
  workoutType: text('workout_type').notNull(),
  title: text('title').notNull(),
  estimatedDuration: integer('estimated_duration'),
  sortOrder: integer('sort_order').notNull().default(0),
})

export const templateWorkoutBlocks = pgTable('template_workout_blocks', {
  id: uuid('id').defaultRandom().primaryKey(),
  templateWorkoutId: uuid('template_workout_id').notNull().references(() => templateWorkouts.id, { onDelete: 'cascade' }),
  blockKey: text('block_key').notNull(),
  title: text('title').notNull(),
  sortOrder: integer('sort_order').notNull().default(0),
})

export const templateExercises = pgTable('template_exercises', {
  id: uuid('id').defaultRandom().primaryKey(),
  blockId: uuid('block_id').notNull().references(() => templateWorkoutBlocks.id, { onDelete: 'cascade' }),
  name: text('name').notNull(),
  prescription: text('prescription'),
  sortOrder: integer('sort_order').notNull().default(0),
})

export const templateWorkoutTips = pgTable('template_workout_tips', {
  id: uuid('id').defaultRandom().primaryKey(),
  templateWorkoutId: uuid('template_workout_id').notNull().references(() => templateWorkouts.id, { onDelete: 'cascade' }),
  tipText: text('tip_text').notNull(),
  sortOrder: integer('sort_order').notNull().default(0),
})
