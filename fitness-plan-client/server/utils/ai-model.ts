import { eq } from 'drizzle-orm'
import type { LanguageModel } from 'ai'
import { db } from './db'
import { userAiSettings } from '../../database/schema/user-config'
import { decrypt } from './crypto'
import { buildModel, getDescriptor, type ModelDescriptor } from './ai-registry'

const CLAUDE_MODEL = 'claude-sonnet-4-6'

const OLLAMA_DEFAULTS = {
  conversation: process.env.OLLAMA_CONVERSATION_MODEL || 'qwen2.5:14b',
  structured: process.env.OLLAMA_STRUCTURED_MODEL || 'qwen2.5-coder:7b',
  small: process.env.OLLAMA_SMALL_MODEL || 'qwen2.5:3b',
}

type ResolvedConfig =
  | { kind: 'anthropic'; apiKey: string; modelId: string }
  | { kind: 'ollama'; baseUrl?: string; modelIds: { conversation: string; structured: string; small: string } }

async function resolveUserConfig(userId: string): Promise<ResolvedConfig> {
  const config = useRuntimeConfig()
  const globalKey = config.anthropicApiKey as string
  if (globalKey) {
    return { kind: 'anthropic', apiKey: globalKey, modelId: CLAUDE_MODEL }
  }

  try {
    const settings = await db.query.userAiSettings.findFirst({
      where: eq(userAiSettings.userId, userId),
    })

    if (settings?.provider === 'anthropic' && settings.apiKeyEnc) {
      return { kind: 'anthropic', apiKey: decrypt(settings.apiKeyEnc), modelId: settings.modelId || CLAUDE_MODEL }
    }

    if (settings?.provider === 'ollama') {
      const modelId = settings.modelId || OLLAMA_DEFAULTS.conversation
      return {
        kind: 'ollama',
        baseUrl: settings.baseUrl || undefined,
        modelIds: { conversation: modelId, structured: modelId, small: modelId },
      }
    }
  } catch (err) {
    console.warn('[ai-model] Failed to load user AI settings:', (err as Error).message)
  }

  return { kind: 'ollama', modelIds: OLLAMA_DEFAULTS }
}

function materialize(descriptor: ModelDescriptor, cfg: ResolvedConfig): LanguageModel {
  if (cfg.kind === 'anthropic') {
    return buildModel(descriptor, { apiKey: cfg.apiKey })
  }
  return buildModel(descriptor, { baseUrl: cfg.baseUrl })
}

export async function resolveModel(userId: string): Promise<LanguageModel> {
  const cfg = await resolveUserConfig(userId)
  const id = cfg.kind === 'anthropic' ? cfg.modelId : cfg.modelIds.conversation
  return materialize(getDescriptor(id, cfg.kind), cfg)
}

export interface ResolvedModelSet {
  conversation: LanguageModel
  structured: LanguageModel
  small: LanguageModel
  descriptors: {
    conversation: ModelDescriptor
    structured: ModelDescriptor
    small: ModelDescriptor
  }
}

export async function resolveModelSet(userId: string): Promise<ResolvedModelSet> {
  const cfg = await resolveUserConfig(userId)

  const ids = cfg.kind === 'anthropic'
    ? { conversation: cfg.modelId, structured: cfg.modelId, small: cfg.modelId }
    : cfg.modelIds

  const descriptors = {
    conversation: getDescriptor(ids.conversation, cfg.kind),
    structured: getDescriptor(ids.structured, cfg.kind),
    small: getDescriptor(ids.small, cfg.kind),
  }

  return {
    conversation: materialize(descriptors.conversation, cfg),
    structured: materialize(descriptors.structured, cfg),
    small: materialize(descriptors.small, cfg),
    descriptors,
  }
}
