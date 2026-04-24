import { db } from '../../../utils/db'
import { userAiSettings } from '../../../../database/schema'
import { eq } from 'drizzle-orm'
import { decrypt } from '../../../utils/crypto'

interface ParsedSet {
  weightKg: number | null
  reps: number | null
  durationSeconds: number | null
  difficulty: number | null
}

interface ParsedExercise {
  name: string
  sets: ParsedSet[]
}

const SYSTEM_PROMPT = `You are a fitness workout set parser. Extract EXACTLY the numbers the user provides — NEVER change, convert, or modify any values.

Return ONLY valid JSON: {"sets": [{"weightKg": 100, "reps": 5, "durationSeconds": null, "difficulty": 7}]}

CRITICAL RULES:
- NEVER modify the weight value. If user says "100kg", weightKg is 100. If user says "100", weightKg is 100.
- Only convert if user explicitly says "lbs" or "pounds" (divide by 2.205)
- "difficulty" is how hard it felt (1-10). Also called "RPE". "pretty hard" = 8, "easy" = 5, "moderate" = 6, "tough" = 9, "max effort" = 10
- If difficulty is not mentioned, set it to null
- For time-based exercises (planks, holds, carries): use "durationSeconds" instead of "reps". "held 60 seconds" = durationSeconds: 60, reps: null
- "30s" or "30 seconds" = durationSeconds: 30
- "1 min" or "1 minute" = durationSeconds: 60
- An exercise can have BOTH reps and duration (e.g. "10 reps in 30 seconds")
- "100 5 7" = weightKg: 100, reps: 5, difficulty: 7
- "100 x 5" = weightKg: 100, reps: 5
- "bodyweight 12" or "bw 12" = weightKg: 0, reps: 12
- If only one number given, it's reps, set weightKg to null
- "8, 8, 6" = 3 separate sets with those rep counts
- Return ONLY JSON, no explanation`

const OLLAMA_BASE = process.env.OLLAMA_BASE_URL || 'http://localhost:11434'
const OLLAMA_MODEL = process.env.OLLAMA_MODEL || 'qwen2.5:14b'

async function parseWithOllama(message: string, baseUrl: string, model: string): Promise<ParsedSet[] | null> {
  try {
    const response = await $fetch<any>(`${baseUrl}/api/chat`, {
      method: 'POST',
      body: {
        model,
        messages: [
          { role: 'system', content: SYSTEM_PROMPT },
          { role: 'user', content: message },
        ],
        format: 'json',
        stream: false,
        options: { temperature: 0.1 },
      },
      timeout: 30000,
    })

    const text = response.message?.content || ''
    const jsonMatch = text.match(/\{[\s\S]*\}/)
    if (jsonMatch) {
      const parsed = JSON.parse(jsonMatch[0])
      if (parsed.sets && Array.isArray(parsed.sets)) {
        return parsed.sets
      }
    }
  } catch (err: any) {
    console.error('Ollama parse error:', err.message)
  }
  return null
}

async function parseWithProvider(message: string, provider: string, apiKey: string, modelId: string): Promise<ParsedSet[] | null> {
  try {
    let apiUrl = ''
    let headers: Record<string, string> = {}
    let requestBody: Record<string, unknown> = {}

    if (provider === 'openai') {
      apiUrl = 'https://api.openai.com/v1/chat/completions'
      headers = { 'Authorization': `Bearer ${apiKey}`, 'Content-Type': 'application/json' }
      requestBody = {
        model: modelId || 'gpt-4o-mini',
        messages: [{ role: 'system', content: SYSTEM_PROMPT }, { role: 'user', content: message }],
        response_format: { type: 'json_object' },
        temperature: 0.1,
      }
    } else if (provider === 'anthropic') {
      apiUrl = 'https://api.anthropic.com/v1/messages'
      headers = { 'x-api-key': apiKey, 'anthropic-version': '2023-06-01', 'Content-Type': 'application/json' }
      requestBody = {
        model: modelId || 'claude-sonnet-4-5-20250514',
        max_tokens: 512,
        system: SYSTEM_PROMPT,
        messages: [{ role: 'user', content: message }],
      }
    } else if (provider === 'google') {
      apiUrl = `https://generativelanguage.googleapis.com/v1beta/models/${modelId || 'gemini-2.0-flash'}:generateContent?key=${apiKey}`
      headers = { 'Content-Type': 'application/json' }
      requestBody = {
        contents: [{ parts: [{ text: `${SYSTEM_PROMPT}\n\nUser: ${message}` }] }],
        generationConfig: { responseMimeType: 'application/json', temperature: 0.1 },
      }
    }

    if (!apiUrl) return null

    const response = await $fetch<any>(apiUrl, { method: 'POST', headers, body: requestBody, timeout: 15000 })

    let jsonText = ''
    if (provider === 'openai') jsonText = response.choices?.[0]?.message?.content || ''
    else if (provider === 'anthropic') jsonText = response.content?.[0]?.text || ''
    else if (provider === 'google') jsonText = response.candidates?.[0]?.content?.parts?.[0]?.text || ''

    const jsonMatch = jsonText.match(/\{[\s\S]*\}/)
    if (jsonMatch) {
      const parsed = JSON.parse(jsonMatch[0])
      if (parsed.sets && Array.isArray(parsed.sets)) return parsed.sets
    }
  } catch (err: any) {
    console.error(`${provider} parse error:`, err.message)
  }
  return null
}

function regexParse(input: string): ParsedSet[] {
  const isBw = /\b(bw|bodyweight)\b/i.test(input)
  const diffMatch = input.match(/@\s*(?:rpe\s*|difficulty\s*)?(\d+(?:\.\d+)?)/i)
    || input.match(/(?:rpe|difficulty)\s*(\d+(?:\.\d+)?)/i)
  const difficulty = diffMatch ? parseFloat(diffMatch[1]) : null

  const cleaned = input
    .replace(/@\s*(?:rpe\s*|difficulty\s*)?\d+(?:\.\d+)?/i, '')
    .replace(/(?:rpe|difficulty)\s*\d+(?:\.\d+)?/i, '')
    .replace(/\b(bw|bodyweight)\b/i, '')
    .replace(/kg/gi, '')
    .replace(/reps?/gi, '')
    .replace(/[x×]/gi, ' ')
    .replace(/for|at|with/gi, ' ')
    .trim()

  const numbers = cleaned.match(/\d+(?:\.\d+)?/g)?.map(Number) || []

  if (numbers.length === 0) return []

  const sets: ParsedSet[] = []

  if (isBw && numbers.length >= 1) {
    sets.push({ weightKg: 0, reps: numbers[0], difficulty: numbers[1] ?? difficulty })
  } else if (numbers.length >= 3) {
    sets.push({ weightKg: numbers[0], reps: numbers[1], difficulty: numbers[2] ?? difficulty })
  } else if (numbers.length === 2) {
    sets.push({ weightKg: numbers[0], reps: numbers[1], difficulty })
  } else if (numbers.length === 1) {
    sets.push({ weightKg: null, reps: numbers[0], difficulty })
  }

  return sets
}

export default defineEventHandler(async (event) => {
  const user = event.context.user
  const body = await readBody(event)
  const message: string = body.message || ''
  const exerciseName: string = body.exerciseName || ''

  if (!message.trim()) {
    throw createError({ statusCode: 400, statusMessage: 'Message is required' })
  }

  const aiSettings = await db.query.userAiSettings.findFirst({
    where: eq(userAiSettings.userId, user.id),
  })

  const provider = aiSettings?.provider || 'ollama'
  const contextMessage = exerciseName ? `Exercise: ${exerciseName}\nInput: ${message}` : message

  let aiSets: ParsedSet[] | null = null

  if (provider === 'ollama') {
    const baseUrl = aiSettings?.baseUrl || OLLAMA_BASE
    const model = aiSettings?.modelId || OLLAMA_MODEL
    aiSets = await parseWithOllama(contextMessage, baseUrl, model)
  } else if (aiSettings?.apiKeyEnc) {
    const apiKey = decrypt(aiSettings.apiKeyEnc)
    aiSets = await parseWithProvider(contextMessage, provider, apiKey, aiSettings.modelId || '')
  }

  if (aiSets && aiSets.length > 0) {
    return {
      source: 'ai' as const,
      provider,
      sets: aiSets,
      originalMessage: message,
    }
  }

  const regexSets = regexParse(message)

  return {
    source: 'parser' as const,
    sets: regexSets,
    originalMessage: message,
  }
})
