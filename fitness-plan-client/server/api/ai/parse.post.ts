import { generateObject } from 'ai'
import { z } from 'zod'
import { resolveModel } from '../../utils/ai-model'
import { ParsedSetSchema } from '../../utils/ai-schemas'
import { SET_PARSER_PROMPT } from '../../utils/ai-prompts'

function regexParse(input: string): z.infer<typeof ParsedSetSchema>[] {
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

  if (isBw && numbers.length >= 1) {
    return [{ weightKg: 0, reps: numbers[0], durationSeconds: null, difficulty: numbers[1] ?? difficulty }]
  }
  if (numbers.length >= 3) {
    return [{ weightKg: numbers[0], reps: numbers[1], durationSeconds: null, difficulty: numbers[2] ?? difficulty }]
  }
  if (numbers.length === 2) {
    return [{ weightKg: numbers[0], reps: numbers[1], durationSeconds: null, difficulty }]
  }
  return [{ weightKg: null, reps: numbers[0], durationSeconds: null, difficulty }]
}

export default defineEventHandler(async (event) => {
  const user = event.context.user
  if (!user) {
    throw createError({ statusCode: 401, statusMessage: 'Unauthorized' })
  }

  const { message, exerciseName } = await readBody(event)

  if (!message?.trim()) {
    throw createError({ statusCode: 400, statusMessage: 'Message required' })
  }

  try {
    const model = await resolveModel(user.id)
    const prompt = exerciseName ? `Exercise: ${exerciseName}\nInput: ${message}` : message

    const { object } = await generateObject({
      model,
      schema: z.object({ sets: z.array(ParsedSetSchema) }),
      system: SET_PARSER_PROMPT,
      prompt,
    })

    return { source: 'ai', sets: object.sets, originalMessage: message }
  } catch {
    const sets = regexParse(message)
    return { source: 'parser', sets, originalMessage: message }
  }
})
