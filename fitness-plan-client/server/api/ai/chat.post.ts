import { streamText, generateText } from 'ai'
import { resolveModelSet } from '../../utils/ai-model'
import { CONVERSATION_PROMPT } from '../../utils/ai-prompts'
import { buildProgramContext } from '../../utils/program-context'
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

function estimateTokens(text: string): number {
  return Math.ceil(text.length / 4)
}

const SUMMARIZER_PROMPT = `You compress a fitness coaching conversation into a concise summary. Preserve:
- User's goals, constraints, equipment, injuries, preferences
- Specific decisions made (program changes, schedule shifts, exercise swaps)
- Key feedback the user gave
- Any pending changes that haven't been confirmed

Return a single paragraph, 200-400 words. No bullets, no headers.`

async function summarizeOlderMessages(
  smallModel: any,
  messages: { role: string; content: string }[],
  keepRecent: number,
): Promise<{ role: string; content: string }[]> {
  if (messages.length <= keepRecent) return messages

  const toSummarize = messages.slice(0, messages.length - keepRecent)
  const recent = messages.slice(messages.length - keepRecent)
  const transcript = toSummarize.map(m => `${m.role.toUpperCase()}: ${m.content}`).join('\n\n')

  try {
    const { text } = await generateText({
      model: smallModel,
      system: SUMMARIZER_PROMPT,
      prompt: transcript,
    })
    return [
      { role: 'user', content: `[Summary of earlier conversation]\n${text.trim()}` },
      ...recent,
    ]
  } catch (err: any) {
    console.error('Summarization failed:', err.message)
    return recent
  }
}

export default defineEventHandler(async (event) => {
  const user = event.context.user
  if (!user) throw createError({ statusCode: 401, statusMessage: 'Unauthorized' })

  const body = await readBody(event)
  let messages = toCoreMsgs(body.messages || [])
  const userProgramId = body.userProgramId || null

  const lastUserMsg = getLastUserMessage(messages)
  if (lastUserMsg) {
    await db.insert(conversationMessages).values({
      userId: user.id,
      userProgramId,
      role: 'user',
      content: lastUserMsg,
    })
  }

  try {
    const models = await resolveModelSet(user.id)

    let systemPrompt = CONVERSATION_PROMPT
    if (userProgramId) {
      const context = await buildProgramContext(user.id, userProgramId)
      if (context) {
        systemPrompt = `${CONVERSATION_PROMPT}\n\n---\nThe user is asking about an existing program. You have full context of their training history below. Use specific numbers from this data when responding — reference exercises, weights, RPE, feedback patterns. Don't ask about things you can already see here.\n\n${context}`
      }
    }

    const contextWindow = models.descriptors.conversation.contextWindow
    const safetyBudget = Math.floor(contextWindow * 0.7)
    const totalTokens = estimateTokens(systemPrompt) + messages.reduce((s, m) => s + estimateTokens(m.content), 0)

    if (totalTokens > safetyBudget && messages.length > 4) {
      console.log(`[chat] Total ~${totalTokens} tokens exceeds budget ${safetyBudget} — summarizing older messages`)
      messages = await summarizeOlderMessages(models.small, messages, 4)
    }

    const result = streamText({ model: models.conversation, system: systemPrompt, messages })

    result.text.then(async (fullText) => {
      if (fullText.trim()) {
        await db.insert(conversationMessages).values({
          userId: user.id,
          userProgramId,
          role: 'assistant',
          content: fullText,
        })
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
