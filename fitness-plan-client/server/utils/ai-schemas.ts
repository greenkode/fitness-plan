import { z } from 'zod'

export const ExerciseSchema = z.object({
  name: z.string(),
  prescription: z.string(),
  description: z.string().optional(),
  sortOrder: z.number(),
})

export const BlockSchema = z.object({
  blockKey: z.string(),
  title: z.string(),
  sortOrder: z.number(),
  exercises: z.array(ExerciseSchema),
})

export const WorkoutSchema = z.object({
  workoutType: z.string(),
  title: z.string(),
  estimatedDuration: z.number(),
  sortOrder: z.number(),
  blocks: z.array(BlockSchema),
})

export const PhaseSchema = z.object({
  phaseNumber: z.number(),
  name: z.string(),
  theme: z.string(),
  description: z.string(),
  progressionCriteria: z.record(z.unknown()),
  workouts: z.array(WorkoutSchema),
})

export const ScheduleEntrySchema = z.object({
  dayOfWeek: z.number().describe('0=Sunday, 1=Monday, ..., 6=Saturday'),
  trainingType: z.string().describe('gym, cardio, recovery, or rest'),
})

export const ProgramProposalSchema = z.object({
  name: z.string(),
  description: z.string(),
  category: z.string(),
  difficultyLevel: z.string(),
  schedule: z.array(ScheduleEntrySchema),
  phases: z.array(PhaseSchema),
})

export const SkeletonWorkoutSchema = z.object({
  workoutType: z.string(),
  title: z.string(),
  estimatedDuration: z.number(),
  focus: z.string(),
})

export const SkeletonPhaseSchema = z.object({
  phaseNumber: z.number(),
  name: z.string(),
  theme: z.string(),
  description: z.string(),
  durationWeeks: z.number(),
  workouts: z.array(SkeletonWorkoutSchema),
})

export const ProgramSkeletonSchema = z.object({
  name: z.string(),
  description: z.string(),
  category: z.string(),
  difficultyLevel: z.string(),
  schedule: z.array(ScheduleEntrySchema),
  phases: z.array(SkeletonPhaseSchema),
})

export const WorkoutDetailSchema = z.object({
  blocks: z.array(BlockSchema),
})

export const ParsedSetSchema = z.object({
  weightKg: z.number().nullable(),
  reps: z.number().nullable(),
  durationSeconds: z.number().nullable().optional(),
  difficulty: z.number().nullable(),
})

export type ProgramProposal = z.infer<typeof ProgramProposalSchema>
