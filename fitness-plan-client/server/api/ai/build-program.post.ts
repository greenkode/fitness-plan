import { generateText } from 'ai'
import { resolveModelSet } from '../../utils/ai-model'
import type { ProgramProposal } from '../../utils/ai-schemas'

function extractJson(text: string): any {
  const match = text.match(/\{[\s\S]*\}/)
  if (match) return JSON.parse(match[0])
  throw new Error('No JSON found in response')
}

const ARCHITECT_SYSTEM = `You are a fitness program architect. Design a program structure.
Return ONLY valid JSON matching this exact structure:
{
  "name": "Program Name",
  "description": "Brief description",
  "category": "strength|hypertrophy|fat_loss|general_fitness",
  "difficultyLevel": "beginner|intermediate|advanced",
  "schedule": [
    {"dayOfWeek": 0, "trainingType": "rest"},
    {"dayOfWeek": 1, "trainingType": "gym"},
    {"dayOfWeek": 2, "trainingType": "rest"},
    {"dayOfWeek": 3, "trainingType": "gym"},
    {"dayOfWeek": 4, "trainingType": "cardio"},
    {"dayOfWeek": 5, "trainingType": "gym"},
    {"dayOfWeek": 6, "trainingType": "recovery"}
  ],
  "phases": [
    {
      "phaseNumber": 1,
      "name": "Phase Name",
      "theme": "Focus area",
      "description": "What this phase does",
      "durationWeeks": 4,
      "workouts": [
        {"workoutType": "gym", "title": "Upper Body Push", "estimatedDuration": 50, "focus": "chest, shoulders, triceps"}
      ]
    }
  ]
}

RULES:
- dayOfWeek: 0=Sunday through 6=Saturday
- trainingType: gym, cardio, recovery, rest
- Create 2-4 phases with progressive difficulty
- Each gym/cardio day needs a unique workout
- Schedule must have all 7 days
- ONLY output JSON, nothing else`

const WORKOUT_SYSTEM = `You are an exercise programmer. Design exercises for ONE workout.
Return ONLY valid JSON:
{
  "blocks": [
    {
      "blockKey": "warmup",
      "title": "Warm-Up",
      "exercises": [
        {"name": "Band Pull-Aparts", "prescription": "2 x 15"},
        {"name": "Arm Circles", "prescription": "1 x 20"}
      ]
    },
    {
      "blockKey": "main",
      "title": "Main Lifts",
      "exercises": [
        {"name": "Barbell Bench Press", "prescription": "4 x 6-8"},
        {"name": "Incline Dumbbell Press", "prescription": "3 x 10-12"},
        {"name": "Standing Overhead Press", "prescription": "3 x 8-10"}
      ]
    },
    {
      "blockKey": "accessory",
      "title": "Accessories",
      "exercises": [
        {"name": "Cable Lateral Raises", "prescription": "3 x 15"},
        {"name": "Tricep Rope Pushdowns", "prescription": "3 x 12-15"},
        {"name": "Face Pulls", "prescription": "3 x 15"}
      ]
    },
    {
      "blockKey": "cooldown",
      "title": "Cool-Down",
      "exercises": [
        {"name": "Static Stretching", "prescription": "5 min"}
      ]
    }
  ]
}

RULES:
- Include warmup (2-3 exercises), main (3-5 exercises), accessory (2-4 exercises), cooldown (1-2)
- Use specific exercise names like "Barbell Back Squat" not just "Squat"
- Prescriptions: "4 x 8-10" or "3 x 12" or "2 x 30s hold"
- Match exercises to the workout focus and available equipment
- ONLY output JSON`

const JSON_FIX_SYSTEM = `You fix malformed JSON. The input may have trailing text, missing brackets, or formatting issues.
Extract the JSON object and return ONLY the corrected valid JSON. No explanation.`

async function generateJson(model: any, system: string, prompt: string, fixModel?: any): Promise<any> {
  const { text } = await generateText({ model, system, prompt })

  try {
    return extractJson(text)
  } catch {
    if (fixModel) {
      const { text: fixed } = await generateText({
        model: fixModel,
        system: JSON_FIX_SYSTEM,
        prompt: text,
      })
      return extractJson(fixed)
    }
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
    const models = await resolveModelSet(user.id)
    const reqText = `Goals: ${goals}\nDays per week: ${daysPerWeek}\nExperience: ${experienceLevel}\nEquipment: ${equipment}\nInjuries: ${injuries || 'none'}\nPreferences: ${preferences || 'none'}`

    console.log('[build] Step 1: Architect (structured model)')
    const skeleton = await generateJson(models.structured, ARCHITECT_SYSTEM, reqText, models.small)

    if (!skeleton?.phases?.length) {
      throw new Error('Architect produced no phases')
    }

    console.log(`[build] Step 2: Designing ${skeleton.phases.reduce((s: number, p: any) => s + (p.workouts?.length || 0), 0)} workouts in parallel (structured model)`)

    const workoutPromises: { phaseNum: number; workout: any; index: number; promise: Promise<any> }[] = []

    for (const phase of skeleton.phases) {
      for (let wi = 0; wi < (phase.workouts || []).length; wi++) {
        const workout = phase.workouts[wi]
        const prompt = `Workout: ${workout.title}\nType: ${workout.workoutType}\nDuration: ${workout.estimatedDuration}min\nFocus: ${workout.focus}\nEquipment: ${equipment}\nExperience: ${experienceLevel}\nInjuries: ${injuries || 'none'}`

        const promise = generateJson(models.structured, WORKOUT_SYSTEM, prompt, models.small).catch(() => ({
          blocks: [{
            blockKey: 'main', title: 'Main', exercises: [
              { name: workout.title, prescription: '3 x 10' },
            ],
          }],
        }))

        workoutPromises.push({ phaseNum: phase.phaseNumber, workout, index: wi, promise })
      }
    }

    const results = await Promise.all(workoutPromises.map(e => e.promise))

    console.log('[build] Step 3: Assembling program')

    const phaseMap = new Map<number, any[]>()
    workoutPromises.forEach((entry, i) => {
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

      if (!phaseMap.has(entry.phaseNum)) phaseMap.set(entry.phaseNum, [])
      phaseMap.get(entry.phaseNum)!.push({
        workoutType: entry.workout.workoutType,
        title: entry.workout.title,
        estimatedDuration: entry.workout.estimatedDuration || 45,
        sortOrder: entry.index,
        blocks,
      })
    })

    const program: ProgramProposal = {
      name: skeleton.name || 'Custom Program',
      description: skeleton.description || '',
      category: skeleton.category || 'general_fitness',
      difficultyLevel: skeleton.difficultyLevel || experienceLevel,
      schedule: (skeleton.schedule || []).map((s: any) => ({
        dayOfWeek: s.dayOfWeek,
        trainingType: s.trainingType,
      })),
      phases: skeleton.phases.map((phase: any) => ({
        phaseNumber: phase.phaseNumber,
        name: phase.name,
        theme: phase.theme || '',
        description: phase.description || '',
        progressionCriteria: { weeks: phase.durationWeeks || 4, rule: 'Progress when difficulty consistently below 7' },
        workouts: phaseMap.get(phase.phaseNumber) || [],
      })),
    }

    console.log(`[build] Done: "${program.name}" - ${program.phases.length} phases, ${workoutPromises.length} workouts`)
    return { program }
  } catch (err: any) {
    console.error('Build program error:', err.message)
    throw createError({ statusCode: 502, statusMessage: 'Failed to generate program: ' + (err.message || 'unknown') })
  }
})
