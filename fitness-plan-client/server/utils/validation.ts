import { z } from 'zod'

export const registerSchema = z.object({
  email: z.string().email(),
  password: z.string().min(8).max(128),
  name: z.string().min(1).max(255).optional(),
})

export const loginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(1),
})

export const profileSchema = z.object({
  experienceLevel: z.enum(['beginner', 'intermediate', 'advanced']),
  goals: z.record(z.unknown()).optional(),
  availableEquipment: z.record(z.unknown()).optional(),
  bodyWeightKg: z.number().positive().optional(),
})

export const scheduleSchema = z.object({
  schedule: z.array(z.object({
    dayOfWeek: z.number().int().min(0).max(6),
    trainingType: z.enum(['gym', 'cardio', 'recovery', 'rest']),
  })).min(1).max(7),
})

export const feedbackSchema = z.object({
  workoutLogId: z.string().uuid().optional(),
  feedbackDate: z.string(),
  fatigueLevel: z.number().int().min(1).max(10).optional(),
  sorenessLevel: z.number().int().min(1).max(10).optional(),
  motivationLevel: z.number().int().min(1).max(10).optional(),
  sleepQuality: z.number().int().min(1).max(10).optional(),
  stressLevel: z.number().int().min(1).max(10).optional(),
  freeText: z.string().max(5000).optional(),
})

export const aiSettingsSchema = z.object({
  provider: z.enum(['ollama', 'openai', 'anthropic', 'google', 'mistral', 'groq']),
  modelId: z.string().optional(),
  apiKey: z.string().optional(),
  baseUrl: z.string().url().optional(),
})

export const workoutLogSchema = z.object({
  assignmentId: z.string().optional().nullable(),
  workoutDate: z.string(),
  notes: z.string().max(5000).optional(),
  overallRpe: z.number().min(1).max(10).optional(),
  exercises: z.array(z.object({
    exerciseName: z.string(),
    templateExerciseId: z.string().uuid().optional().nullable(),
    sets: z.array(z.object({
      setNumber: z.number().int().positive(),
      weightKg: z.number().optional().nullable(),
      reps: z.number().int().optional().nullable(),
      durationSeconds: z.number().int().optional().nullable(),
      rpe: z.number().min(1).max(10).optional().nullable(),
      completed: z.boolean().default(false),
      notes: z.string().optional().nullable(),
    })),
  })),
})
