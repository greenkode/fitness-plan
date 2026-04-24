import { tool, generateObject, jsonSchema } from 'ai'
import { z } from 'zod'
import { resolveModel } from './ai-model'
import { ProgramSkeletonSchema, WorkoutDetailSchema } from './ai-schemas'
import { ARCHITECT_PROMPT, WORKOUT_DESIGNER_PROMPT, REVIEW_PROMPT } from './ai-prompts'
import type { ProgramProposal } from './ai-schemas'

export function createBuildProgramTool(userId: string) {
  return tool({
    description: 'Design a complete fitness program based on gathered requirements. Call this when you have collected all necessary information about the user\'s goals, schedule, experience, equipment, and limitations.',
    parameters: jsonSchema<{
      goals: string
      daysPerWeek: number
      experienceLevel: string
      equipment: string
      injuries: string
      preferences: string
    }>({
      type: 'object',
      properties: {
        goals: { type: 'string', description: 'What the user wants to achieve' },
        daysPerWeek: { type: 'number', description: 'Training days per week, between 1 and 7' },
        experienceLevel: { type: 'string', description: 'beginner, intermediate, or advanced' },
        equipment: { type: 'string', description: 'Available equipment' },
        injuries: { type: 'string', description: 'Injuries or limitations, or none' },
        preferences: { type: 'string', description: 'Any specific preferences, or none' },
      },
      required: ['goals', 'daysPerWeek', 'experienceLevel', 'equipment', 'injuries', 'preferences'],
    }),
    execute: async (input) => {
      const model = await resolveModel(userId)
      return await buildProgramPipeline(model, input)
    },
  })
}

async function buildProgramPipeline(model: any, requirements: {
  goals: string
  daysPerWeek: number
  experienceLevel: string
  equipment: string
  injuries: string
  preferences?: string
}): Promise<ProgramProposal> {
  const requirementsText = `Goals: ${requirements.goals}
Days per week: ${requirements.daysPerWeek}
Experience: ${requirements.experienceLevel}
Equipment: ${requirements.equipment}
Injuries: ${requirements.injuries}
Preferences: ${requirements.preferences || 'none'}`

  const { object: skeleton } = await generateObject({
    model,
    schema: ProgramSkeletonSchema,
    system: ARCHITECT_PROMPT,
    prompt: requirementsText,
  })

  const fullPhases = []

  for (const phase of skeleton.phases) {
    const fullWorkouts = []

    for (let wi = 0; wi < phase.workouts.length; wi++) {
      const workout = phase.workouts[wi]

      let blocks
      try {
        const { object: detail } = await generateObject({
          model,
          schema: WorkoutDetailSchema,
          system: WORKOUT_DESIGNER_PROMPT,
          prompt: `Program: ${skeleton.name} (${skeleton.difficultyLevel})
Phase: ${phase.name} - ${phase.theme}
Workout: ${workout.title} (${workout.workoutType}, ${workout.estimatedDuration} min)
Focus: ${workout.focus}
Equipment: ${requirements.equipment}
Experience: ${requirements.experienceLevel}
Injuries: ${requirements.injuries}`,
        })
        blocks = detail.blocks.map((b, bi) => ({
          ...b,
          sortOrder: bi,
          exercises: b.exercises.map((e, ei) => ({ ...e, sortOrder: ei })),
        }))
      } catch {
        blocks = [{
          blockKey: 'main',
          title: 'Main',
          sortOrder: 0,
          exercises: [{ name: workout.title, prescription: '3 x 10', sortOrder: 0 }],
        }]
      }

      fullWorkouts.push({
        workoutType: workout.workoutType,
        title: workout.title,
        estimatedDuration: workout.estimatedDuration,
        sortOrder: wi,
        blocks,
      })
    }

    fullPhases.push({
      phaseNumber: phase.phaseNumber,
      name: phase.name,
      theme: phase.theme,
      description: phase.description,
      progressionCriteria: { weeks: phase.durationWeeks, rule: 'Progress when difficulty consistently below 7' },
      workouts: fullWorkouts,
    })
  }

  const program: ProgramProposal = {
    name: skeleton.name,
    description: skeleton.description,
    category: skeleton.category,
    difficultyLevel: skeleton.difficultyLevel,
    schedule: skeleton.schedule,
    phases: fullPhases,
  }

  try {
    const { object: review } = await generateObject({
      model,
      schema: z.object({
        approved: z.boolean(),
        issues: z.array(z.string()).optional(),
      }),
      system: REVIEW_PROMPT,
      prompt: JSON.stringify(program, null, 2),
    })

    if (!review.approved && review.issues) {
      console.log('Program review issues:', review.issues)
    }
  } catch {}

  return program
}
