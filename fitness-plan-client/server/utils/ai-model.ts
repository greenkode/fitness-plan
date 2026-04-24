import { createAnthropic } from '@ai-sdk/anthropic'
import { createOpenAICompatible } from '@ai-sdk/openai-compatible'
import { eq } from 'drizzle-orm'
import { db } from './db'
import { userAiSettings } from '../../database/schema/user-config'
import { decrypt } from './crypto'
import type { LanguageModelV1 } from 'ai'

const CLAUDE_MODEL = 'claude-sonnet-4-6'
const OLLAMA_BASE = process.env.OLLAMA_BASE_URL || 'http://localhost:11434'
const OLLAMA_MODEL = process.env.OLLAMA_MODEL || 'qwen2.5:14b'

function createOllamaModel(baseUrl: string, modelId: string): LanguageModelV1 {
  const provider = createOpenAICompatible({
    baseURL: baseUrl + '/v1',
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
      const apiKey = decrypt(settings.apiKeyEnc)
      return createAnthropic({ apiKey })(settings.modelId || CLAUDE_MODEL)
    }

    if (settings?.provider === 'ollama') {
      return createOllamaModel(settings.baseUrl || OLLAMA_BASE, settings.modelId || OLLAMA_MODEL)
    }
  } catch (err: any) {
    console.error('Failed to load user AI settings:', err.message)
  }

  return createOllamaModel(OLLAMA_BASE, OLLAMA_MODEL)
}
