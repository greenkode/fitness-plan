import { createAnthropic } from '@ai-sdk/anthropic'
import { createOpenAICompatible } from '@ai-sdk/openai-compatible'
import { eq } from 'drizzle-orm'
import { db } from './db'
import { userAiSettings } from '../../database/schema/user-config'
import { decrypt } from './crypto'
import type { LanguageModelV1 } from 'ai'

const OLLAMA_BASE = process.env.OLLAMA_BASE_URL || 'http://localhost:11434'
const CLAUDE_MODEL = 'claude-sonnet-4-6'

const MODELS = {
  conversation: process.env.OLLAMA_CONVERSATION_MODEL || 'qwen2.5:14b',
  structured: process.env.OLLAMA_STRUCTURED_MODEL || 'qwen2.5-coder:7b',
  small: process.env.OLLAMA_SMALL_MODEL || 'qwen2.5:3b',
}

function ollamaModel(modelId: string, baseUrl?: string): LanguageModelV1 {
  const provider = createOpenAICompatible({
    baseURL: (baseUrl || OLLAMA_BASE) + '/v1',
    name: 'ollama',
  })
  return provider(modelId)
}

export async function resolveModel(userId: string): Promise<LanguageModelV1> {
  const config = useRuntimeConfig()
  const anthropicKey = config.anthropicApiKey as string

  if (anthropicKey) {
    return createAnthropic({ apiKey: anthropicKey })(CLAUDE_MODEL)
  }

  try {
    const settings = await db.query.userAiSettings.findFirst({
      where: eq(userAiSettings.userId, userId),
    })

    if (settings?.provider === 'anthropic' && settings.apiKeyEnc) {
      return createAnthropic({ apiKey: decrypt(settings.apiKeyEnc) })(settings.modelId || CLAUDE_MODEL)
    }

    if (settings?.provider === 'ollama') {
      return ollamaModel(settings.modelId || MODELS.conversation, settings.baseUrl || undefined)
    }
  } catch (err: any) {
    console.error('Failed to load user AI settings:', err.message)
  }

  return ollamaModel(MODELS.conversation)
}

export function getConversationModel(): LanguageModelV1 {
  return ollamaModel(MODELS.conversation)
}

export function getStructuredModel(): LanguageModelV1 {
  return ollamaModel(MODELS.structured)
}

export function getSmallModel(): LanguageModelV1 {
  return ollamaModel(MODELS.small)
}

export async function resolveModelSet(userId: string) {
  const config = useRuntimeConfig()
  const anthropicKey = config.anthropicApiKey as string

  if (anthropicKey) {
    const claude = createAnthropic({ apiKey: anthropicKey })(CLAUDE_MODEL)
    return { conversation: claude, structured: claude, small: claude }
  }

  try {
    const settings = await db.query.userAiSettings.findFirst({
      where: eq(userAiSettings.userId, userId),
    })

    if (settings?.provider === 'anthropic' && settings.apiKeyEnc) {
      const claude = createAnthropic({ apiKey: decrypt(settings.apiKeyEnc) })(settings.modelId || CLAUDE_MODEL)
      return { conversation: claude, structured: claude, small: claude }
    }
  } catch {}

  return {
    conversation: ollamaModel(MODELS.conversation),
    structured: ollamaModel(MODELS.structured),
    small: ollamaModel(MODELS.small),
  }
}
