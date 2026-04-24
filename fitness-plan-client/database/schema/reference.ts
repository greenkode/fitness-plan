import { pgTable, uuid, integer, text, jsonb, uniqueIndex } from 'drizzle-orm/pg-core'

export const goldenRules = pgTable('golden_rules', {
  id: uuid('id').defaultRandom().primaryKey(),
  ruleNumber: integer('rule_number').notNull(),
  title: text('title').notNull(),
  subtitle: text('subtitle'),
  content: jsonb('content'),
}, (table) => [
  uniqueIndex('golden_rules_rule_number_idx').on(table.ruleNumber),
])
