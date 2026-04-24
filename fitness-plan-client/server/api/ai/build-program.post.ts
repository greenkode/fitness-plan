import { generateObject, generateText } from 'ai'
import { resolveModel } from '../../utils/ai-model'
import { ProgramSkeletonSchema, WorkoutDetailSchema } from '../../utils/ai-schemas'
import { ARCHITECT_PROMPT, WORKOUT_DESIGNER_PROMPT } from '../../utils/ai-prompts'
import type { ProgramProposal } from '../../utils/ai-schemas'

async function generateStructured(model: any, system: string, prompt: string, schema: any): Promise<any> {
  try {
    const { object } = await generateObject({ model, schema, system, prompt })
    return object
  } catch {
    const { text } = await generateText({
      model,
      system: system + '\n\nRespond with ONLY valid JSON, no explanation.',
      prompt,
    })
    const match = text.match(/\{[\s\S]*\}/)
    if (match) return JSON.parse(match[0])
    throw new Error('Failed to generate valid JSON')
  }
}

export default defineEventHandler(async (event) => {
  const user = event.context.user
  if (!user) throw createError({ statusCode: 401, statusMessage: 'Unauthorized' })

  const body = await readBody(event)
  const { goals, daysPerWeek, experienceLevel, equipment, injuries, preferences } = body

  if (!goals || !daysPerWeek || !experienceLevel || !equipment) {
    throw createError({ statusCode: 400, statusMessage: 'Missing required fields' })
  }

  try {
    const model = await resolveModel(user.id)
    const reqText = `Goals: ${goals}\nDays per week: ${daysPerWeek}\nExperience: ${experienceLevel}\nEquipment: ${equipment}\nInjuries: ${injuries || 'none'}\nPreferences: ${preferences || 'none'}`

    const skeleton = await generateStructured(model, ARCHITECT_PROMPT, reqText, ProgramSkeletonSchema)

    if (!skeleton?.phases?.length) {
      throw new Error('No phases generated')
    }

    const allWorkoutPromises: { phase: any; workout: any; index: number; promise: Promise<any> }[] = []

    for (const phase of skeleton.phases) {
      for (let wi = 0; wi < (phase.workouts || []).length; wi++) {
        const workout = phase.workouts[wi]
        const promise = generateStructured(
          model,
          WORKOUT_DESIGNER_PROMPT,
          `Program: ${skeleton.name} (${skeleton.difficultyLevel})\nPhase: ${phase.name} - ${phase.theme}\nWorkout: ${workout.title} (${workout.workoutType}, ${workout.estimatedDuration}min)\nFocus: ${workout.focus}\nEquipment: ${equipment}\nExperience: ${experienceLevel}\nInjuries: ${injuries || 'none'}`,
          WorkoutDetailSchema,
        ).catch(() => ({
          blocks: [{ blockKey: 'main', title: 'Main', sortOrder: 0, exercises: [{ name: workout.title, prescription: '3 x 10', sortOrder: 0 }] }],
        }))
        allWorkoutPromises.push({ phase, workout, index: wi, promise })
      }
    }

    const results = await Promise.all(allWorkoutPromises.map(e => e.promise))

    const phaseMap = new Map<number, any[]>()
    allWorkoutPromises.forEach((entry, i) => {
      const detail = results[i]
      const blocks = (detail.blocks || []).map((b: any, bi: number) => ({
        blockKey: b.blockKey || 'main',
        title: b.title || 'Block',
        sortOrder: bi,
        exercises: (b.exercises || []).map((e: any, ei: number) => ({
          name: e.name || 'Exercise',
          prescription: e.prescription || '3 x 10',
          sortOrder: ei,
        })),
      }))
      const phaseNum = entry.phase.phaseNumber
      if (!phaseMap.has(phaseNum)) phaseMap.set(phaseNum, [])
      phaseMap.get(phaseNum)!.push({
        workoutType: entry.workout.workoutType,
        title: entry.workout.title,
        estimatedDuration: entry.workout.estimatedDuration || 45,
        sortOrder: entry.index,
        blocks,
      })
    })

    const fullPhases = skeleton.phases.map((phase: any) => ({
      phaseNumber: phase.phaseNumber,
      name: phase.name,
      theme: phase.theme || '',
      description: phase.description || '',
      progressionCriteria: { weeks: phase.durationWeeks || 4, rule: 'Progress when difficulty consistently below 7' },
      workouts: phaseMap.get(phase.phaseNumber) || [],
    }))

    const program: ProgramProposal = {
      name: skeleton.name || 'Custom Program',
      description: skeleton.description || '',
      category: skeleton.category || 'general_fitness',
      difficultyLevel: skeleton.difficultyLevel || experienceLevel,
      schedule: skeleton.schedule || [],
      phases: fullPhases,
    }

    return { program }
  } catch (err: any) {
    console.error('Build program error:', err.message)
    throw createError({ statusCode: 502, statusMessage: 'Failed to generate program: ' + (err.message || 'unknown') })
  }
})
