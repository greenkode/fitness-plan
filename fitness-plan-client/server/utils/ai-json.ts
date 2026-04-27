import { generateText, type LanguageModel } from 'ai'
import type { ZodSchema } from 'zod'

export class StructuredGenerationError extends Error {
  constructor(
    message: string,
    readonly rawText: string,
    readonly cause?: unknown,
  ) {
    super(message)
    this.name = 'StructuredGenerationError'
  }
}

export function extractJson(text: string): unknown {
  const match = text.match(/\{[\s\S]*\}/)
  if (!match) throw new Error('No JSON object found in response')
  return JSON.parse(match[0])
}

const JSON_FIX_SYSTEM = `You fix malformed JSON. The input may have trailing text, missing brackets, or formatting issues.
Extract the JSON object and return ONLY the corrected valid JSON. No explanation.`

export interface GenerateStructuredOptions<T> {
  model: LanguageModel
  system: string
  prompt: string
  schema: ZodSchema<T>
  fixModel?: LanguageModel
}

export async function generateStructured<T>(opts: GenerateStructuredOptions<T>): Promise<T> {
  const { text } = await generateText({ model: opts.model, system: opts.system, prompt: opts.prompt })

  try {
    return opts.schema.parse(extractJson(text))
  } catch (firstErr) {
    if (!opts.fixModel) {
      throw new StructuredGenerationError('Failed to parse structured response', text, firstErr)
    }
    try {
      const { text: fixed } = await generateText({
        model: opts.fixModel,
        system: JSON_FIX_SYSTEM,
        prompt: text,
      })
      return opts.schema.parse(extractJson(fixed))
    } catch (secondErr) {
      throw new StructuredGenerationError('Failed to parse structured response after fix retry', text, secondErr)
    }
  }
}
