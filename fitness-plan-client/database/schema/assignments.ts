import { pgTable, uuid, text, date, uniqueIndex } from 'drizzle-orm/pg-core'
import { userPrograms } from './user-config'
import { templateWorkouts } from './templates'

export const workoutAssignments = pgTable('workout_assignments', {
  id: uuid('id').defaultRandom().primaryKey(),
  userProgramId: uuid('user_program_id').notNull().references(() => userPrograms.id, { onDelete: 'cascade' }),
  assignedDate: date('assigned_date').notNull(),
  templateWorkoutId: uuid('template_workout_id').notNull().references(() => templateWorkouts.id),
  status: text('status').notNull().default('pending'),
  notes: text('notes'),
}, (table) => [
  uniqueIndex('workout_assignments_program_date_idx').on(table.userProgramId, table.assignedDate),
])
