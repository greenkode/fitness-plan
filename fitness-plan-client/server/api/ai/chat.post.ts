import { streamText } from 'ai'
import { resolveModelSet } from '../../utils/ai-model'
import { CONVERSATION_PROMPT } from '../../utils/ai-prompts'

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

export default defineEventHandler(async (event) => {
  const user = event.context.user
  if (!user) throw createError({ statusCode: 401, statusMessage: 'Unauthorized' })

  const body = await readBody(event)
  const messages = toCoreMsgs(body.messages || [])

  try {
    const models = await resolveModelSet(user.id)
    const result = streamText({ model: models.conversation, system: CONVERSATION_PROMPT, messages })
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
