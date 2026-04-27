import { eq, and, desc, gte } from 'drizzle-orm'
import { z } from 'zod'
import { db } from '../../../utils/db'
import { workoutLogs, exerciseLogs, setLogs } from '../../../../database/schema/tracking'
import { sessionFeedback } from '../../../../database/schema/feedback'
import { coachingEvents } from '../../../../database/schema/coaching'
import { resolveModelSet } from '../../../utils/ai-model'
import { generateStructured } from '../../../utils/ai-json'

const SuggestionSchema = z.object({
  suggestions: z.array(z.object({
    type: z.string().describe('phase_advance | weight_increase | volume_adjust | deload | milestone | rest_day | exercise_swap'),
    title: z.string().describe('Short headline'),
    rationale: z.string().describe('Why this is suggested based on the data'),
    actionable: z.boolean().describe('Whether the user can act on this'),
    priority: z.string().describe('high | medium | low'),
  })),
})

const COACHING_PROMPT = `You are an expert fitness coach analyzing a user's recent workout data and feedback. Generate 1-3 actionable suggestions to help them progress.

Look for patterns:
- High RPE consistently → suggest deload
- Low RPE for 3+ sessions → suggest weight increase
- High fatigue/soreness → suggest extra rest day
- Low motivation → suggest variety or exercise swap
- Steady performance → suggest phase advancement
- New PRs → celebrate milestone

Be specific and reference actual numbers. Only suggest things that are clearly supported by the data. If the data is insufficient, return an empty suggestions array.

Return JSON only.`

export default defineEventHandler(async (event) => {
  const user = event.context.user
  if (!user) throw createError({ statusCode: 401, statusMessage: 'Unauthorized' })

  try {
    const fourteenDaysAgo = new Date()
    fourteenDaysAgo.setDate(fourteenDaysAgo.getDate() - 14)
    const fromDate = fourteenDaysAgo.toISOString().split('T')[0]

    const recentWorkouts = await db.select({
      id: workoutLogs.id,
      workoutDate: workoutLogs.workoutDate,
      startedAt: workoutLogs.startedAt,
      completedAt: workoutLogs.completedAt,
      overallRpe: workoutLogs.overallRpe,
    })
      .from(workoutLogs)
      .where(and(
        eq(workoutLogs.userId, user.id),
        gte(workoutLogs.workoutDate, fromDate),
      ))
      .orderBy(desc(workoutLogs.workoutDate))
      .limit(10)

    if (recentWorkouts.length === 0) {
      return { suggestions: [], message: 'No recent workouts to analyze' }
    }

    const workoutSummaries = await Promise.all(recentWorkouts.map(async (w) => {
      const exercises = await db.select({
        name: exerciseLogs.exerciseName,
        id: exerciseLogs.id,
      }).from(exerciseLogs).where(eq(exerciseLogs.workoutLogId, w.id))

      const exerciseSummaries = await Promise.all(exercises.map(async (ex) => {
        const sets = await db.select({
          weight: setLogs.weightKg,
          reps: setLogs.reps,
          rpe: setLogs.rpe,
        }).from(setLogs).where(eq(setLogs.exerciseLogId, ex.id))
        return { name: ex.name, sets }
      }))

      const totalVolume = exerciseSummaries.reduce((sum, ex) =>
        sum + ex.sets.reduce((s, set) => s + ((set.weight || 0) * (set.reps || 0)), 0), 0)
      const avgRpe = exerciseSummaries.flatMap(e => e.sets.map(s => s.rpe)).filter(r => r !== null)
      const avgRpeValue = avgRpe.length ? avgRpe.reduce((a, b) => a + (b || 0), 0) / avgRpe.length : null

      return {
        date: w.workoutDate,
        totalVolume,
        avgRpe: avgRpeValue,
        exerciseCount: exercises.length,
        exercises: exerciseSummaries,
      }
    }))

    const recentFeedback = await db.select({
      date: sessionFeedback.feedbackDate,
      fatigue: sessionFeedback.fatigueLevel,
      soreness: sessionFeedback.sorenessLevel,
      motivation: sessionFeedback.motivationLevel,
      sleep: sessionFeedback.sleepQuality,
      stress: sessionFeedback.stressLevel,
      notes: sessionFeedback.freeText,
    })
      .from(sessionFeedback)
      .where(and(
        eq(sessionFeedback.userId, user.id),
        gte(sessionFeedback.feedbackDate, fromDate),
      ))
      .orderBy(desc(sessionFeedback.feedbackDate))
      .limit(10)

    const dataSummary = `Recent workouts (last 14 days):
${workoutSummaries.map(w => `- ${w.date}: ${w.exerciseCount} exercises, ${w.totalVolume}kg volume, avg RPE ${w.avgRpe?.toFixed(1) || 'N/A'}`).join('\n')}

Recent feedback:
${recentFeedback.map(f => `- ${f.date}: fatigue=${f.fatigue}/10, soreness=${f.soreness}/10, motivation=${f.motivation}/10, sleep=${f.sleep}/10, stress=${f.stress}/10${f.notes ? ` — "${f.notes}"` : ''}`).join('\n') || 'No feedback recorded'}

Top exercises by frequency:
${Object.entries(workoutSummaries.flatMap(w => w.exercises).reduce<Record<string, number>>((acc, ex) => { acc[ex.name] = (acc[ex.name] || 0) + 1; return acc }, {})).sort(([, a], [, b]) => b - a).slice(0, 5).map(([name, count]) => `- ${name}: ${count} sessions`).join('\n')}`

    const models = await resolveModelSet(user.id)
    const result = await generateStructured({
      model: models.structured,
      system: COACHING_PROMPT,
      prompt: dataSummary,
      schema: SuggestionSchema,
      fixModel: models.small,
    })

    const stored = await Promise.all(result.suggestions.map(async (s) => {
      const [event] = await db.insert(coachingEvents).values({
        userId: user.id,
        eventType: s.type,
        details: s,
      }).returning({
        id: coachingEvents.id,
        eventType: coachingEvents.eventType,
        details: coachingEvents.details,
        suggestedAt: coachingEvents.suggestedAt,
      })
      return event
    }))

    return { suggestions: stored, dataAnalyzed: { workouts: recentWorkouts.length, feedback: recentFeedback.length } }
  } catch (err: any) {
    console.error('Coaching analysis error:', err.message)
    throw createError({ statusCode: 500, statusMessage: 'Failed to generate coaching insights' })
  }
})
