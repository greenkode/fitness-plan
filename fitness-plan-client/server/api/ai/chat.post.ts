import { streamText } from 'ai'
import { resolveModelSet } from '../../utils/ai-model'
import { CONVERSATION_PROMPT } from '../../utils/ai-prompts'
import { db } from '../../utils/db'
import { conversationMessages } from '../../../database/schema/conversations'

function toCoreMsgs(uiMessages: any[]): { role: string; content: string }[] {
  return uiMessages.map((m: any) => {
    let content = ''
    if (typeof m.content === 'string' && m.content.trim()) {
      content = m.content
    } else if (m.parts && Array.isArray(m.parts)) {
      content = m.parts
        .filter((p: any) => p.type === 'text')
        .map((p: any) => p.text)
        .join('')
    }
    return { role: m.role, content }
  }).filter(m => m.content.trim())
}

function getLastUserMessage(messages: { role: string; content: string }[]): string {
  for (let i = messages.length - 1; i >= 0; i--) {
    if (messages[i].role === 'user') return messages[i].content
  }
  return ''
}

export default defineEventHandler(async (event) => {
  const user = event.context.user
  if (!user) throw createError({ statusCode: 401, statusMessage: 'Unauthorized' })

  const body = await readBody(event)
  const messages = toCoreMsgs(body.messages || [])
  const userProgramId = body.userProgramId || null

  const lastUserMsg = getLastUserMessage(messages)
  let userMsgId: string | null = null
  if (lastUserMsg) {
    const [saved] = await db.insert(conversationMessages).values({
      userId: user.id,
      userProgramId,
      role: 'user',
      content: lastUserMsg,
    }).returning({ id: conversationMessages.id })
    userMsgId = saved.id
  }

  try {
    const models = await resolveModelSet(user.id)
    const result = streamText({ model: models.conversation, system: CONVERSATION_PROMPT, messages })

    result.text.then(async (fullText) => {
      if (fullText.trim()) {
        const [saved] = await db.insert(conversationMessages).values({
          userId: user.id,
          userProgramId,
          role: 'assistant',
          content: fullText,
        }).returning({ id: conversationMessages.id })

        console.log(`[chat] Saved conversation: user=${userMsgId}, assistant=${saved.id}`)
      }
    }).catch((err) => {
      console.error('[chat] Failed to save assistant message:', err.message)
    })

    const response = result.toUIMessageStreamResponse()

    setResponseStatus(event, response.status)
    response.headers.forEach((value, key) => {
      setResponseHeader(event, key, value)
    })

    if (response.body) {
      return sendStream(event, response.body)
    }

    return ''
  } catch (err: any) {
    console.error('AI chat error:', err.message)
    throw createError({ statusCode: 502, statusMessage: 'AI service unavailable' })
  }
})
