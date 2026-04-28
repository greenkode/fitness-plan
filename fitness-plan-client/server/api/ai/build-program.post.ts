import { z } from 'zod'
import { resolveModelSet } from '../../utils/ai-model'
import { generateStructured } from '../../utils/ai-json'
import { ScheduleEntrySchema, type ProgramProposal } from '../../utils/ai-schemas'

const RawSkeletonWorkoutSchema = z.object({
  workoutType: z.string(),
  title: z.string(),
  estimatedDuration: z.number().optional(),
  focus: z.string().optional(),
})

const RawSkeletonPhaseSchema = z.object({
  phaseNumber: z.number(),
  name: z.string().optional(),
  theme: z.string().optional(),
  description: z.string().optional(),
  durationWeeks: z.number().optional(),
  workouts: z.array(RawSkeletonWorkoutSchema),
})

const RawSkeletonSchema = z.object({
  name: z.string().optional(),
  description: z.string().optional(),
  category: z.string().optional(),
  difficultyLevel: z.string().optional(),
  schedule: z.array(ScheduleEntrySchema).optional(),
  phases: z.array(RawSkeletonPhaseSchema).min(1),
})

const RawWorkoutDetailSchema = z.object({
  blocks: z.array(z.object({
    blockKey: z.string().optional(),
    title: z.string().optional(),
    exercises: z.array(z.object({
      name: z.string(),
      prescription: z.string(),
      description: z.string().optional(),
    })),
  })),
})

type RawWorkoutDetail = z.infer<typeof RawWorkoutDetailSchema>

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
        {"name": "Band Pull-Aparts", "prescription": "2 x 15", "description": "Hold a light resistance band at shoulder width and pull apart, squeezing shoulder blades. Activates rear delts and upper back."},
        {"name": "Arm Circles", "prescription": "1 x 20", "description": "Extend arms to sides and make controlled circles forward then backward. Warms up shoulder joints and rotator cuff."}
      ]
    },
    {
      "blockKey": "main",
      "title": "Main Lifts",
      "exercises": [
        {"name": "Barbell Bench Press", "prescription": "4 x 6-8", "description": "Lie flat, grip just outside shoulder width, lower bar to mid-chest with control, press up. Targets chest, front delts, triceps."},
        {"name": "Incline Dumbbell Press", "prescription": "3 x 10-12", "description": "On a 30-45° incline bench, press dumbbells from shoulder level to lockout. Emphasizes upper chest and front delts."},
        {"name": "Standing Overhead Press", "prescription": "3 x 8-10", "description": "Press a barbell from shoulders to overhead, keeping core braced and ribs down. Builds shoulder and tricep strength."}
      ]
    },
    {
      "blockKey": "accessory",
      "title": "Accessories",
      "exercises": [
        {"name": "Cable Lateral Raises", "prescription": "3 x 15", "description": "Raise cable handle out to the side to shoulder height with a slight bend in the elbow. Isolates the medial deltoid."},
        {"name": "Tricep Rope Pushdowns", "prescription": "3 x 12-15", "description": "Push rope down and apart at the bottom while keeping elbows pinned at sides. Targets all three tricep heads."},
        {"name": "Face Pulls", "prescription": "3 x 15", "description": "Pull a rope to your forehead with elbows high and externally rotate at the end range. Strengthens rear delts and upper back."}
      ]
    },
    {
      "blockKey": "cooldown",
      "title": "Cool-Down",
      "exercises": [
        {"name": "Static Stretching", "prescription": "5 min", "description": "Hold gentle stretches for trained muscle groups for 20-30 seconds each. Helps recovery and mobility."}
      ]
    }
  ]
}

RULES:
- Include warmup (2-3 exercises), main (3-5 exercises), accessory (2-4 exercises), cooldown (1-2)
- Use specific exercise names like "Barbell Back Squat" not just "Squat"
- Prescriptions: "4 x 8-10" or "3 x 12" or "2 x 30s hold"
- Every exercise MUST include a "description" field: 1-2 sentences covering execution cues and target muscles
- Match exercises to the workout focus and available equipment
- ONLY output JSON`

function estimateTokens(text: string): number {
  return Math.ceil(text.length / 4)
}

const PLACEHOLDER_DETAIL: RawWorkoutDetail = {
  blocks: [{ blockKey: 'main', title: 'Main', exercises: [] }],
}

export default defineEventHandler(async (event) => {
  const user = event.context.user
  if (!user) throw createError({ statusCode: 401, statusMessage: 'Unauthorized' })

  const body = await readBody(event)
  const { goals, daysPerWeek, experienceLevel, equipment, injuries, preferences } = body

  if (!goals || !daysPerWeek || !experienceLevel || !equipment) {
    throw createError({ statusCode: 400, statusMessage: 'Missing required fields' })
  }

  const models = await resolveModelSet(user.id)
  const reqText = `Goals: ${goals}\nDays per week: ${daysPerWeek}\nExperience: ${experienceLevel}\nEquipment: ${equipment}\nInjuries: ${injuries || 'none'}\nPreferences: ${preferences || 'none'}`

  const architectEstimate = estimateTokens(ARCHITECT_SYSTEM) + estimateTokens(reqText)
  const architectLimit = models.descriptors.structured.contextWindow
  if (architectEstimate > architectLimit * 0.8) {
    throw createError({
      statusCode: 400,
      statusMessage: `Prompt ~${architectEstimate} tokens exceeds 80% of model ${models.descriptors.structured.id} context (${architectLimit}). Choose a larger model or shorten your request.`,
    })
  }

  try {
    console.log(`[build] Step 1: Architect (${models.descriptors.structured.id})`)
    const skeleton = await generateStructured({
      model: models.structured,
      system: ARCHITECT_SYSTEM,
      prompt: reqText,
      schema: RawSkeletonSchema,
      fixModel: models.small,
    })

    const workoutCount = skeleton.phases.reduce((s, p) => s + p.workouts.length, 0)
    console.log(`[build] Step 2: Designing ${workoutCount} workouts in parallel`)

    const workoutPromises: { phaseNum: number; workout: z.infer<typeof RawSkeletonWorkoutSchema>; index: number; promise: Promise<RawWorkoutDetail> }[] = []

    for (const phase of skeleton.phases) {
      for (const [wi, workout] of phase.workouts.entries()) {
        const prompt = `Workout: ${workout.title}\nType: ${workout.workoutType}\nDuration: ${workout.estimatedDuration || 45}min\nFocus: ${workout.focus || ''}\nEquipment: ${equipment}\nExperience: ${experienceLevel}\nInjuries: ${injuries || 'none'}`

        const promise = generateStructured({
          model: models.structured,
          system: WORKOUT_SYSTEM,
          prompt,
          schema: RawWorkoutDetailSchema,
          fixModel: models.small,
        }).catch(() => PLACEHOLDER_DETAIL)

        workoutPromises.push({ phaseNum: phase.phaseNumber, workout, index: wi, promise })
      }
    }

    const results = await Promise.all(workoutPromises.map(e => e.promise))

    console.log('[build] Step 3: Assembling program')

    const phaseMap = new Map<number, ProgramProposal['phases'][number]['workouts']>()
    workoutPromises.forEach((entry, i) => {
      const detail = results[i] ?? PLACEHOLDER_DETAIL
      const blocks = detail.blocks.map((b, bi) => ({
        blockKey: b.blockKey || 'main',
        title: b.title || 'Block',
        sortOrder: bi,
        exercises: b.exercises.map((e, ei) => ({
          name: e.name,
          prescription: e.prescription,
          description: e.description,
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
      schedule: (skeleton.schedule || []).map(s => ({
        dayOfWeek: s.dayOfWeek,
        trainingType: s.trainingType,
      })),
      phases: skeleton.phases.map(phase => ({
        phaseNumber: phase.phaseNumber,
        name: phase.name || `Phase ${phase.phaseNumber}`,
        theme: phase.theme || '',
        description: phase.description || '',
        progressionCriteria: { weeks: phase.durationWeeks || 4, rule: 'Progress when difficulty consistently below 7' },
        workouts: phaseMap.get(phase.phaseNumber) || [],
      })),
    }

    console.log(`[build] Done: "${program.name}" - ${program.phases.length} phases, ${workoutPromises.length} workouts`)
    return { program }
  } catch (err) {
    const message = (err as Error).message || 'unknown'
    console.error('[build] Failed:', message)
    throw createError({ statusCode: 502, statusMessage: 'Failed to generate program: ' + message })
  }
})
