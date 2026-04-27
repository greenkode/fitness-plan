import { eq, and, asc } from 'drizzle-orm'
import { db } from '../../../utils/db'
import { conversationMessages } from '../../../../database/schema/conversations'

export default defineEventHandler(async (event) => {
  const user = event.context.user
  if (!user) throw createError({ statusCode: 401, statusMessage: 'Unauthorized' })

  const programId = getRouterParam(event, 'programId')
  if (!programId) throw createError({ statusCode: 400, statusMessage: 'Program ID required' })

  const messages = await db.select({
    id: conversationMessages.id,
    role: conversationMessages.role,
    content: conversationMessages.content,
    metadata: conversationMessages.metadata,
    createdAt: conversationMessages.createdAt,
  }).from(conversationMessages)
    .where(and(
      eq(conversationMessages.userId, user.id),
      eq(conversationMessages.userProgramId, programId),
    ))
    .orderBy(asc(conversationMessages.createdAt))

  return messages
})
