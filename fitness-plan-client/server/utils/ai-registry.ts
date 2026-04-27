import { createAnthropic } from '@ai-sdk/anthropic'
import { createOpenAICompatible } from '@ai-sdk/openai-compatible'
import type { LanguageModel } from 'ai'

export type ProviderKind = 'anthropic' | 'ollama'

export interface ModelDescriptor {
  id: string
  provider: ProviderKind
  contextWindow: number
  maxOutputTokens: number
}

const REGISTRY: Record<string, ModelDescriptor> = {
  'claude-sonnet-4-6': { id: 'claude-sonnet-4-6', provider: 'anthropic', contextWindow: 200_000, maxOutputTokens: 8192 },
  'claude-opus-4-7': { id: 'claude-opus-4-7', provider: 'anthropic', contextWindow: 200_000, maxOutputTokens: 8192 },
  'claude-haiku-4-5-20251001': { id: 'claude-haiku-4-5-20251001', provider: 'anthropic', contextWindow: 200_000, maxOutputTokens: 8192 },
  'qwen2.5:14b': { id: 'qwen2.5:14b', provider: 'ollama', contextWindow: 32_768, maxOutputTokens: 4096 },
  'qwen2.5-coder:7b': { id: 'qwen2.5-coder:7b', provider: 'ollama', contextWindow: 32_768, maxOutputTokens: 4096 },
  'qwen2.5:3b': { id: 'qwen2.5:3b', provider: 'ollama', contextWindow: 32_768, maxOutputTokens: 2048 },
}

const FALLBACK: Record<ProviderKind, Omit<ModelDescriptor, 'id'>> = {
  anthropic: { provider: 'anthropic', contextWindow: 100_000, maxOutputTokens: 4096 },
  ollama: { provider: 'ollama', contextWindow: 8_192, maxOutputTokens: 2048 },
}

export function getDescriptor(id: string, provider: ProviderKind): ModelDescriptor {
  return REGISTRY[id] ?? { id, ...FALLBACK[provider] }
}

export interface BuildModelOptions {
  apiKey?: string
  baseUrl?: string
}

export function buildModel(descriptor: ModelDescriptor, opts: BuildModelOptions): LanguageModel {
  if (descriptor.provider === 'anthropic') {
    if (!opts.apiKey) throw new Error(`Anthropic model ${descriptor.id} requires an apiKey`)
    return createAnthropic({ apiKey: opts.apiKey })(descriptor.id)
  }
  const base = opts.baseUrl || process.env.OLLAMA_BASE_URL || 'http://localhost:11434'
  const provider = createOpenAICompatible({ baseURL: base + '/v1', name: 'ollama' })
  return provider(descriptor.id)
}
