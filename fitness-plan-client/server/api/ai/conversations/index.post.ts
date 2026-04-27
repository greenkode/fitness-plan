import { db } from '../../../utils/db'
import { conversationMessages } from '../../../../database/schema/conversations'

export default defineEventHandler(async (event) => {
  const user = event.context.user
  if (!user) throw createError({ statusCode: 401, statusMessage: 'Unauthorized' })

  const { userProgramId, role, content } = await readBody(event)

  if (!role || !content) {
    throw createError({ statusCode: 400, statusMessage: 'role and content are required' })
  }

  const [msg] = await db.insert(conversationMessages).values({
    userId: user.id,
    userProgramId: userProgramId || null,
    role,
    content,
  }).returning({ id: conversationMessages.id })

  return { id: msg.id }
})
