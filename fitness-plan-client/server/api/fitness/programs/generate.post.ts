import { eq } from 'drizzle-orm'
import { db } from '../../../utils/db'
import { userAiSettings } from '../../../../database/schema/user-config'
import { programTemplates, templatePhases, templateWorkouts, templateWorkoutBlocks, templateExercises } from '../../../../database/schema/templates'
import { userPrograms, userSchedules } from '../../../../database/schema/user-config'
import { workoutAssignments } from '../../../../database/schema/assignments'
import { decrypt } from '../../../utils/crypto'

const OLLAMA_BASE = process.env.OLLAMA_BASE_URL || 'http://localhost:11434'
const OLLAMA_MODEL = process.env.OLLAMA_MODEL || 'qwen2.5:14b'
const CLAUDE_MODEL = 'claude-sonnet-4-5-20250514'

const CONVERSATION_PROMPT = `You are Krachtix, an expert fitness coach and program designer. Have a natural conversation to understand the user's needs.

ALWAYS respond with valid JSON:
{"phase": "question", "message": "your response here"}

When you have gathered ALL of these: goals, training days per week, experience level, available equipment, and any injuries — respond with:
{"phase": "ready", "message": "Great! Let me design your program now...", "requirements": {
  "goals": "what the user wants to achieve",
  "daysPerWeek": 4,
  "experienceLevel": "beginner|intermediate|advanced",
  "equipment": "what they have access to",
  "injuries": "any limitations or none",
  "preferences": "any specific preferences mentioned"
}}

RULES:
- Ask 1-2 questions at a time, be encouraging and conversational
- Don't propose a program yet — just gather information
- When you have everything, output phase "ready" with the requirements summary
- NEVER return anything except valid JSON`

const ARCHITECT_PROMPT = `You are a fitness program architect. Design a complete program structure based on the requirements.

Return ONLY valid JSON:
{
  "name": "Program Name",
  "description": "2-3 sentence description",
  "category": "strength|hypertrophy|fat_loss|general_fitness",
  "difficultyLevel": "beginner|intermediate|advanced",
  "totalWeeks": 12,
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
      "theme": "Training focus",
      "description": "What this phase achieves",
      "durationWeeks": 4,
      "workouts": [
        {"workoutType": "gym", "title": "Upper Body Push", "estimatedDuration": 50, "focus": "chest, shoulders, triceps"},
        {"workoutType": "gym", "title": "Lower Body Strength", "estimatedDuration": 55, "focus": "quads, hamstrings, glutes"},
        {"workoutType": "gym", "title": "Upper Body Pull", "estimatedDuration": 50, "focus": "back, biceps, rear delts"},
        {"workoutType": "cardio", "title": "HIIT Conditioning", "estimatedDuration": 30, "focus": "cardiovascular fitness"}
      ]
    }
  ]
}

RULES:
- Create 2-4 phases with progressive difficulty
- Each gym day needs its own unique workout
- Schedule must have all 7 days (dayOfWeek 0-6, 0=Sunday)
- Training types: "gym", "cardio", "recovery", "rest"
- Number of gym/cardio days must match the user's requested training days
- Each workout needs a descriptive title and focus area
- Be creative with workout titles — not just "Day 1", "Day 2"
- ONLY return JSON, nothing else`

const WORKOUT_DESIGNER_PROMPT = `You are an expert exercise programmer. Design the detailed exercises for ONE specific workout.

Return ONLY valid JSON:
{
  "blocks": [
    {
      "blockKey": "warmup",
      "title": "Warm-Up",
      "exercises": [
        {"name": "Exercise Name", "prescription": "2 x 10"},
        {"name": "Exercise Name", "prescription": "1 x 30s hold"}
      ]
    },
    {
      "blockKey": "main",
      "title": "Main Lifts",
      "exercises": [
        {"name": "Bench Press", "prescription": "4 x 6-8 @RPE8"},
        {"name": "Incline Dumbbell Press", "prescription": "3 x 10-12 @RPE7"},
        {"name": "Overhead Press", "prescription": "3 x 8-10 @RPE7"}
      ]
    },
    {
      "blockKey": "accessory",
      "title": "Accessories",
      "exercises": [
        {"name": "Lateral Raises", "prescription": "3 x 15"},
        {"name": "Tricep Pushdowns", "prescription": "3 x 12-15"},
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
- ALWAYS include warmup (2-4 exercises), main (3-5 exercises), accessory (2-4 exercises)
- Include cooldown when appropriate
- Warmup: dynamic stretches, activation drills, light sets
- Main: compound movements with specific rep/set schemes and RPE
- Accessory: isolation and supplementary work
- Prescriptions: "4 x 8-10 @RPE7" or "3 x 12" or "2 x 30s hold" or "5 min"
- Choose exercises appropriate for the equipment and experience level
- Be specific with exercise names (e.g., "Barbell Back Squat" not just "Squat")
- For cardio workouts: use intervals, circuits, or timed work
- For recovery: stretching, foam rolling, light movement
- ONLY return JSON`

const REVIEW_PROMPT = `You are a fitness program quality reviewer. Check this program for completeness and quality.

Review the program and return ONLY valid JSON:
{
  "approved": true,
  "issues": [],
  "suggestions": []
}

Or if there are problems:
{
  "approved": false,
  "issues": ["Missing warmup in Upper Body Push", "Too few exercises in Phase 2 Leg Day"],
  "fixes": {
    "Phase 1 - Upper Body Push - warmup": [
      {"name": "Arm Circles", "prescription": "2 x 15"},
      {"name": "Band Pull-Aparts", "prescription": "2 x 15"}
    ]
  }
}

Check for:
- Every gym workout has warmup + main + accessory blocks
- Main blocks have 3-5 exercises
- Accessory blocks have 2-4 exercises
- Exercise selection makes sense for the workout focus
- Progressive overload between phases
- Balanced muscle group coverage across the week
- ONLY return JSON`

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

interface ProgramProposal {
  name: string
  description: string
  category: string
  difficultyLevel: string
  schedule: { dayOfWeek: number; trainingType: string }[]
  phases: {
    phaseNumber: number
    name: string
    theme: string
    description: string
    progressionCriteria: Record<string, unknown>
    workouts: {
      workoutType: string
      title: string
      estimatedDuration: number
      sortOrder: number
      blocks: {
        blockKey: string
        title: string
        sortOrder: number
        exercises: { name: string; prescription: string; sortOrder: number }[]
      }[]
    }[]
  }[]
}

async function llmCall(systemPrompt: string, userContent: string, config: { anthropicKey?: string; ollamaBase?: string; ollamaModel?: string }): Promise<string> {
  if (config.anthropicKey) {
    const response = await $fetch<any>('https://api.anthropic.com/v1/messages', {
      method: 'POST',
      headers: {
        'x-api-key': config.anthropicKey,
        'anthropic-version': '2023-06-01',
        'Content-Type': 'application/json',
      },
      body: {
        model: CLAUDE_MODEL,
        max_tokens: 8192,
        system: systemPrompt,
        messages: [{ role: 'user', content: userContent }],
      },
      timeout: 120000,
    })
    return response.content?.[0]?.text || ''
  }

  const response = await $fetch<any>(`${config.ollamaBase || OLLAMA_BASE}/api/chat`, {
    method: 'POST',
    body: {
      model: config.ollamaModel || OLLAMA_MODEL,
      messages: [
        { role: 'system', content: systemPrompt },
        { role: 'user', content: userContent },
      ],
      format: 'json',
      stream: false,
    },
    timeout: 120000,
  })
  return response.message?.content || ''
}

async function llmChat(systemPrompt: string, messages: ChatMessage[], config: { anthropicKey?: string; ollamaBase?: string; ollamaModel?: string }): Promise<string> {
  if (config.anthropicKey) {
    const response = await $fetch<any>('https://api.anthropic.com/v1/messages', {
      method: 'POST',
      headers: {
        'x-api-key': config.anthropicKey,
        'anthropic-version': '2023-06-01',
        'Content-Type': 'application/json',
      },
      body: {
        model: CLAUDE_MODEL,
        max_tokens: 8192,
        system: systemPrompt,
        messages,
      },
      timeout: 120000,
    })
    return response.content?.[0]?.text || ''
  }

  const response = await $fetch<any>(`${config.ollamaBase || OLLAMA_BASE}/api/chat`, {
    method: 'POST',
    body: {
      model: config.ollamaModel || OLLAMA_MODEL,
      messages: [{ role: 'system', content: systemPrompt }, ...messages],
      format: 'json',
      stream: false,
    },
    timeout: 120000,
  })
  return response.message?.content || ''
}

function parseJson(text: string): any | null {
  try {
    const match = text.match(/\{[\s\S]*\}/)
    if (match) return JSON.parse(match[0])
  } catch {}
  return null
}

function getLlmConfig(anthropicKey: string, aiSettings: any) {
  return {
    anthropicKey: anthropicKey || (aiSettings?.provider === 'anthropic' && aiSettings?.apiKeyEnc ? decrypt(aiSettings.apiKeyEnc) : undefined),
    ollamaBase: aiSettings?.baseUrl || undefined,
    ollamaModel: aiSettings?.modelId || undefined,
  }
}

async function buildProgramWithPipeline(requirements: string, config: { anthropicKey?: string; ollamaBase?: string; ollamaModel?: string }): Promise<ProgramProposal> {
  const architectResponse = await llmCall(ARCHITECT_PROMPT, `User requirements:\n${requirements}`, config)
  const skeleton = parseJson(architectResponse)
  if (!skeleton || !skeleton.phases) {
    throw new Error('Architect failed to produce valid program structure')
  }

  const fullPhases = []
  for (const phase of skeleton.phases) {
    const fullWorkouts = []
    for (let wi = 0; wi < phase.workouts.length; wi++) {
      const workout = phase.workouts[wi]
      const designPrompt = `Design exercises for this workout:
Program: ${skeleton.name} (${skeleton.difficultyLevel})
Phase: ${phase.name} - ${phase.theme}
Workout: ${workout.title} (${workout.workoutType}, ${workout.estimatedDuration} min)
Focus: ${workout.focus || workout.title}
Equipment: from user requirements
Experience: ${skeleton.difficultyLevel}`

      const designResponse = await llmCall(WORKOUT_DESIGNER_PROMPT, designPrompt, config)
      const workoutDetail = parseJson(designResponse)

      const blocks = (workoutDetail?.blocks || []).map((b: any, bi: number) => ({
        blockKey: b.blockKey || 'main',
        title: b.title || 'Block',
        sortOrder: bi,
        exercises: (b.exercises || []).map((e: any, ei: number) => ({
          name: e.name,
          prescription: e.prescription || '3 x 10',
          sortOrder: ei,
        })),
      }))

      if (blocks.length === 0) {
        blocks.push({
          blockKey: 'main',
          title: 'Main',
          sortOrder: 0,
          exercises: [{ name: workout.title, prescription: '3 x 10', sortOrder: 0 }],
        })
      }

      fullWorkouts.push({
        workoutType: workout.workoutType,
        title: workout.title,
        estimatedDuration: workout.estimatedDuration || 45,
        sortOrder: wi,
        blocks,
      })
    }

    fullPhases.push({
      phaseNumber: phase.phaseNumber,
      name: phase.name,
      theme: phase.theme || '',
      description: phase.description || '',
      progressionCriteria: { weeks: phase.durationWeeks || 4, rule: 'Progress when difficulty consistently below 7' },
      workouts: fullWorkouts,
    })
  }

  const program: ProgramProposal = {
    name: skeleton.name,
    description: skeleton.description || '',
    category: skeleton.category || 'general_fitness',
    difficultyLevel: skeleton.difficultyLevel || 'intermediate',
    schedule: skeleton.schedule || [],
    phases: fullPhases,
  }

  try {
    const reviewInput = JSON.stringify(program, null, 2)
    const reviewResponse = await llmCall(REVIEW_PROMPT, reviewInput, config)
    const review = parseJson(reviewResponse)

    if (review && !review.approved && review.fixes) {
      for (const [key, exercises] of Object.entries(review.fixes)) {
        const parts = key.split(' - ')
        if (parts.length >= 3) {
          const phaseName = parts[0].replace(/^Phase \d+ /, '')
          const workoutTitle = parts[1]
          const blockKey = parts[2]

          for (const phase of program.phases) {
            if (phase.name.includes(phaseName)) {
              for (const workout of phase.workouts) {
                if (workout.title.includes(workoutTitle)) {
                  let block = workout.blocks.find(b => b.blockKey === blockKey)
                  if (!block) {
                    block = { blockKey, title: blockKey.charAt(0).toUpperCase() + blockKey.slice(1), sortOrder: workout.blocks.length, exercises: [] }
                    workout.blocks.push(block)
                  }
                  for (const ex of exercises as any[]) {
                    block.exercises.push({ name: ex.name, prescription: ex.prescription, sortOrder: block.exercises.length })
                  }
                }
              }
            }
          }
        }
      }
    }
  } catch {}

  return program
}

async function handleChat(user: { id: string; email: string; name: string }, messages: ChatMessage[]) {
  const config = useRuntimeConfig()
  const anthropicKey = config.anthropicApiKey as string

  const aiSettings = await db.query.userAiSettings.findFirst({
    where: eq(userAiSettings.userId, user.id),
  })

  const llmConfig = getLlmConfig(anthropicKey, aiSettings)
  const responseText = await llmChat(CONVERSATION_PROMPT, messages, llmConfig)
  const parsed = parseJson(responseText)

  if (!parsed) {
    return { type: 'question' as const, text: responseText || 'Could you tell me more about your fitness goals?' }
  }

  if (parsed.phase === 'ready' && parsed.requirements) {
    const requirements = typeof parsed.requirements === 'string' ? parsed.requirements : JSON.stringify(parsed.requirements)

    const program = await buildProgramWithPipeline(requirements, llmConfig)

    return {
      type: 'program' as const,
      text: parsed.message || 'Here is your program!',
      program,
    }
  }

  return { type: 'question' as const, text: parsed.message || responseText }
}

async function handleSave(user: { id: string; email: string; name: string }, program: ProgramProposal) {
  if (!program.phases || program.phases.length === 0) {
    throw createError({ statusCode: 400, statusMessage: 'Program must have at least one phase' })
  }

  for (const phase of program.phases) {
    if (!phase.workouts || phase.workouts.length === 0) {
      throw createError({ statusCode: 400, statusMessage: `Phase "${phase.name}" must have at least one workout` })
    }
    for (const workout of phase.workouts) {
      if (!workout.blocks || workout.blocks.length === 0) {
        throw createError({ statusCode: 400, statusMessage: `Workout "${workout.title}" must have at least one block` })
      }
    }
  }

  const result = await db.transaction(async (tx) => {
    const [template] = await tx.insert(programTemplates).values({
      name: program.name,
      description: program.description,
      category: program.category,
      difficultyLevel: program.difficultyLevel,
      isSystem: false,
      createdBy: user.id,
    }).returning()

    let firstPhaseId: string | null = null
    const phaseWorkoutMap: Map<string, { workoutType: string; templateWorkoutId: string }[]> = new Map()

    for (const phase of program.phases) {
      const [insertedPhase] = await tx.insert(templatePhases).values({
        templateId: template.id,
        phaseNumber: phase.phaseNumber,
        name: phase.name,
        theme: phase.theme,
        description: phase.description,
        progressionCriteria: phase.progressionCriteria,
      }).returning()

      if (!firstPhaseId) firstPhaseId = insertedPhase.id

      const workoutEntries: { workoutType: string; templateWorkoutId: string }[] = []

      for (const workout of phase.workouts) {
        const [insertedWorkout] = await tx.insert(templateWorkouts).values({
          phaseId: insertedPhase.id,
          workoutType: workout.workoutType,
          title: workout.title,
          estimatedDuration: workout.estimatedDuration,
          sortOrder: workout.sortOrder,
        }).returning()

        workoutEntries.push({ workoutType: workout.workoutType, templateWorkoutId: insertedWorkout.id })

        for (const block of workout.blocks) {
          const [insertedBlock] = await tx.insert(templateWorkoutBlocks).values({
            templateWorkoutId: insertedWorkout.id,
            blockKey: block.blockKey,
            title: block.title,
            sortOrder: block.sortOrder,
          }).returning()

          if (block.exercises.length > 0) {
            await tx.insert(templateExercises).values(
              block.exercises.map(ex => ({
                blockId: insertedBlock.id,
                name: ex.name,
                prescription: ex.prescription,
                sortOrder: ex.sortOrder,
              }))
            )
          }
        }
      }

      phaseWorkoutMap.set(insertedPhase.id, workoutEntries)
    }

    await tx.delete(userSchedules).where(eq(userSchedules.userId, user.id))

    if (program.schedule) {
      for (const entry of program.schedule) {
        await tx.insert(userSchedules).values({
          userId: user.id,
          dayOfWeek: entry.dayOfWeek,
          trainingType: entry.trainingType,
        })
      }
    }

    const [userProgram] = await tx.insert(userPrograms).values({
      userId: user.id,
      templateId: template.id,
      currentPhaseId: firstPhaseId,
      status: 'active',
    }).returning()

    const firstPhaseWorkouts = phaseWorkoutMap.get(firstPhaseId!) || []
    const today = new Date()
    const workoutCountsForType = new Map<string, number>()

    for (let day = 0; day < 28; day++) {
      const date = new Date(today)
      date.setDate(today.getDate() + day)
      const dayOfWeek = date.getDay()

      const scheduleEntry = (program.schedule || []).find(s => s.dayOfWeek === dayOfWeek)
      if (!scheduleEntry || scheduleEntry.trainingType === 'rest') continue

      const matchingWorkouts = firstPhaseWorkouts.filter(w => w.workoutType === scheduleEntry.trainingType)
      if (matchingWorkouts.length === 0) continue

      const typeKey = scheduleEntry.trainingType
      const currentCount = workoutCountsForType.get(typeKey) || 0
      const workoutIndex = currentCount % matchingWorkouts.length
      workoutCountsForType.set(typeKey, currentCount + 1)

      await tx.insert(workoutAssignments).values({
        userProgramId: userProgram.id,
        assignedDate: date.toISOString().split('T')[0],
        templateWorkoutId: matchingWorkouts[workoutIndex].templateWorkoutId,
        status: 'pending',
      })
    }

    return { templateId: template.id, userProgramId: userProgram.id }
  })

  return result
}

export default defineEventHandler(async (event) => {
  const user = event.context.user
  if (!user) {
    throw createError({ statusCode: 401, statusMessage: 'Unauthorized' })
  }

  const body = await readBody(event)

  if (body.action === 'chat') {
    try {
      return await handleChat(user, body.messages || [])
    } catch (err: unknown) {
      console.error('Program generation error:', err instanceof Error ? err.message : err)
      throw createError({ statusCode: 502, statusMessage: 'AI service error. Please try again.' })
    }
  }

  if (body.action === 'save') {
    try {
      return await handleSave(user, body.program)
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'statusCode' in err) throw err
      console.error('Program save error:', err instanceof Error ? err.message : err)
      throw createError({ statusCode: 500, statusMessage: 'Failed to save program' })
    }
  }

  throw createError({ statusCode: 400, statusMessage: 'Invalid action' })
})
