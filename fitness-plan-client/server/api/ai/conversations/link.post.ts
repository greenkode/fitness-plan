import { eq, and, inArray } from 'drizzle-orm'
import { db } from '../../../utils/db'
import { conversationMessages } from '../../../../database/schema/conversations'

export default defineEventHandler(async (event) => {
  const user = event.context.user
  if (!user) throw createError({ statusCode: 401, statusMessage: 'Unauthorized' })

  const { messageIds, userProgramId } = await readBody(event)

  if (!messageIds?.length || !userProgramId) {
    throw createError({ statusCode: 400, statusMessage: 'messageIds and userProgramId required' })
  }

  await db.update(conversationMessages)
    .set({ userProgramId })
    .where(and(
      eq(conversationMessages.userId, user.id),
      inArray(conversationMessages.id, messageIds),
    ))

  return { linked: messageIds.length }
})
