import { pgTable, uuid, text, integer, jsonb, index } from 'drizzle-orm/pg-core'
import { users } from './users'
import { sql } from 'drizzle-orm'

export const ragEmbeddings = pgTable('rag_embeddings', {
  id: uuid('id').defaultRandom().primaryKey(),
  userId: uuid('user_id').references(() => users.id, { onDelete: 'cascade' }),
  content: text('content').notNull(),
  embedding: text('embedding'),
  metadata: jsonb('metadata'),
  chunkIndex: integer('chunk_index'),
  sourceType: text('source_type').notNull(),
  sourceId: text('source_id'),
}, (table) => [
  index('rag_embeddings_source_type_idx').on(table.sourceType),
])
