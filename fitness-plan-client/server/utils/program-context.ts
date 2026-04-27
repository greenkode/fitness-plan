import { eq, and, desc } from 'drizzle-orm'
import { db } from './db'
import { workoutLogs, exerciseLogs, setLogs } from '../../database/schema/tracking'
import { sessionFeedback } from '../../database/schema/feedback'
import { coachingEvents } from '../../database/schema/coaching'
import { userPrograms, userProfiles, userSchedules } from '../../database/schema/user-config'
import { programTemplates, templatePhases } from '../../database/schema/templates'

export async function buildProgramContext(userId: string, programId: string): Promise<string> {
  try {
    const programRows = await db.select({
      id: userPrograms.id,
      templateId: userPrograms.templateId,
      status: userPrograms.status,
      startedAt: userPrograms.startedAt,
      currentPhaseId: userPrograms.currentPhaseId,
      templateName: programTemplates.name,
      templateDesc: programTemplates.description,
      category: programTemplates.category,
      difficulty: programTemplates.difficultyLevel,
    })
      .from(userPrograms)
      .innerJoin(programTemplates, eq(userPrograms.templateId, programTemplates.id))
      .where(and(eq(userPrograms.id, programId), eq(userPrograms.userId, userId)))
      .limit(1)

    if (!programRows.length) return ''
    const program = programRows[0]

    const profile = await db.query.userProfiles.findFirst({
      where: eq(userProfiles.userId, userId),
    })

    const schedule = await db.select({
      dayOfWeek: userSchedules.dayOfWeek,
      trainingType: userSchedules.trainingType,
    })
      .from(userSchedules)
      .where(eq(userSchedules.userId, userId))
      .orderBy(userSchedules.dayOfWeek)

    const phases = await db.select({
      id: templatePhases.id,
      name: templatePhases.name,
      phaseNumber: templatePhases.phaseNumber,
      theme: templatePhases.theme,
    })
      .from(templatePhases)
      .where(eq(templatePhases.templateId, program.templateId))
      .orderBy(templatePhases.phaseNumber)
      .catch(() => [])

    const allWorkouts = await db.select({
      id: workoutLogs.id,
      date: workoutLogs.workoutDate,
      startedAt: workoutLogs.startedAt,
      completedAt: workoutLogs.completedAt,
      overallRpe: workoutLogs.overallRpe,
    })
      .from(workoutLogs)
      .where(eq(workoutLogs.userId, userId))
      .orderBy(desc(workoutLogs.workoutDate))

    const exerciseHistory = new Map<string, { count: number; maxWeight: number; maxReps: number; lastSeen: string; totalVolume: number }>()
    const recentWorkoutDetails: string[] = []

    for (let i = 0; i < allWorkouts.length; i++) {
      const w = allWorkouts[i]
      const exercises = await db.select({
        id: exerciseLogs.id,
        name: exerciseLogs.exerciseName,
      }).from(exerciseLogs).where(eq(exerciseLogs.workoutLogId, w.id))

      const exerciseStats: string[] = []
      for (const ex of exercises) {
        const sets = await db.select({
          weight: setLogs.weightKg,
          reps: setLogs.reps,
          rpe: setLogs.rpe,
        }).from(setLogs).where(eq(setLogs.exerciseLogId, ex.id))

        if (sets.length === 0) continue

        const totalVol = sets.reduce((s, set) => s + ((set.weight || 0) * (set.reps || 0)), 0)
        const maxWeight = sets.reduce((m, s) => Math.max(m, s.weight || 0), 0)
        const maxReps = sets.reduce((m, s) => Math.max(m, s.reps || 0), 0)
        const rpeSets = sets.filter(s => s.rpe !== null)
        const avgRpe = rpeSets.length ? rpeSets.reduce((a, b) => a + (b.rpe || 0), 0) / rpeSets.length : null

        const stat = exerciseHistory.get(ex.name) || { count: 0, maxWeight: 0, maxReps: 0, lastSeen: w.date, totalVolume: 0 }
        stat.count++
        stat.maxWeight = Math.max(stat.maxWeight, maxWeight)
        stat.maxReps = Math.max(stat.maxReps, maxReps)
        stat.totalVolume += totalVol
        if (w.date > stat.lastSeen) stat.lastSeen = w.date
        exerciseHistory.set(ex.name, stat)

        if (i < 8) {
          const setSummary = sets.map(s => `${s.weight ?? '?'}kg×${s.reps ?? '?'}${s.rpe ? `@${s.rpe}` : ''}`).join(', ')
          exerciseStats.push(`    ${ex.name}: ${setSummary}${avgRpe ? ` (avg diff ${avgRpe.toFixed(1)})` : ''}`)
        }
      }

      if (i < 8 && exerciseStats.length > 0) {
        const duration = w.startedAt && w.completedAt
          ? Math.round((new Date(w.completedAt).getTime() - new Date(w.startedAt).getTime()) / 60000)
          : null
        recentWorkoutDetails.push(`  ${w.date}${duration ? ` (${duration}min)` : ''}:\n${exerciseStats.join('\n')}`)
      }
    }

    const allFeedback = await db.select({
      date: sessionFeedback.feedbackDate,
      fatigue: sessionFeedback.fatigueLevel,
      soreness: sessionFeedback.sorenessLevel,
      motivation: sessionFeedback.motivationLevel,
      sleep: sessionFeedback.sleepQuality,
      stress: sessionFeedback.stressLevel,
      notes: sessionFeedback.freeText,
    })
      .from(sessionFeedback)
      .where(eq(sessionFeedback.userId, userId))
      .orderBy(desc(sessionFeedback.feedbackDate))
      .limit(15)

    const acceptedSuggestions = await db.select({
      eventType: coachingEvents.eventType,
      details: coachingEvents.details,
      suggestedAt: coachingEvents.suggestedAt,
    })
      .from(coachingEvents)
      .where(and(
        eq(coachingEvents.userId, userId),
        eq(coachingEvents.accepted, true),
      ))
      .orderBy(desc(coachingEvents.suggestedAt))
      .limit(5)

    const dates = new Set(allWorkouts.filter(w => w.completedAt).map(w => w.date))
    let currentStreak = 0
    let checkDate = new Date()
    checkDate.setHours(0, 0, 0, 0)
    while (true) {
      const dateStr = checkDate.toISOString().split('T')[0]
      if (dates.has(dateStr)) {
        currentStreak++
        checkDate.setDate(checkDate.getDate() - 1)
      } else if (currentStreak === 0) {
        const yesterday = new Date(checkDate)
        yesterday.setDate(yesterday.getDate() - 1)
        if (dates.has(yesterday.toISOString().split('T')[0])) {
          checkDate = yesterday
          continue
        }
        break
      } else {
        break
      }
    }

    const currentPhase = phases.find(p => p.id === program.currentPhaseId)
    const sections: string[] = []

    sections.push(`## Active Program: ${program.templateName}`)
    if (program.templateDesc) sections.push(program.templateDesc)
    sections.push(`Category: ${program.category || 'general'} | Difficulty: ${program.difficulty || 'intermediate'} | Status: ${program.status}`)
    sections.push(`Started: ${new Date(program.startedAt).toISOString().split('T')[0]}`)
    if (currentPhase) sections.push(`Current Phase: ${currentPhase.name} (${currentPhase.phaseNumber}/${phases.length})${currentPhase.theme ? ` — ${currentPhase.theme}` : ''}`)

    if (schedule.length > 0) {
      const dayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
      sections.push(`Weekly Schedule: ${schedule.map(s => `${dayNames[s.dayOfWeek]}=${s.trainingType}`).join(', ')}`)
    }

    if (profile) {
      const profileBits: string[] = []
      if (profile.bodyWeightKg) profileBits.push(`bodyweight ${profile.bodyWeightKg}kg`)
      if (profile.experienceLevel) profileBits.push(`experience ${profile.experienceLevel}`)
      if (profileBits.length) sections.push(`User profile: ${profileBits.join(' | ')}`)
    }

    sections.push(`\n## Training Stats`)
    sections.push(`Total workouts logged: ${allWorkouts.filter(w => w.completedAt).length}`)
    sections.push(`Current streak: ${currentStreak} days`)

    if (exerciseHistory.size > 0) {
      const allExercises = [...exerciseHistory.entries()]
        .sort(([, a], [, b]) => b.count - a.count)
      const totalVol = allExercises.reduce((s, [, stat]) => s + stat.totalVolume, 0)
      sections.push(`Total volume lifted: ${Math.round(totalVol).toLocaleString()}kg`)

      const exerciseSummary = allExercises.slice(0, 15).map(([name, s]) =>
        `  - ${name}: ${s.count} sessions, max ${s.maxWeight}kg × ${s.maxReps} reps (last: ${s.lastSeen})`)
      sections.push(`\n## Exercise History (top 15 by frequency)\n${exerciseSummary.join('\n')}`)
    }

    if (recentWorkoutDetails.length > 0) {
      sections.push(`\n## Recent Workouts (last 8 sessions, full set detail)\n${recentWorkoutDetails.join('\n\n')}`)
    } else {
      sections.push(`\n## Recent Workouts: none logged yet`)
    }

    if (allFeedback.length > 0) {
      const feedbackLines = allFeedback.map(f => {
        const parts = []
        if (f.fatigue !== null) parts.push(`fatigue ${f.fatigue}`)
        if (f.soreness !== null) parts.push(`soreness ${f.soreness}`)
        if (f.motivation !== null) parts.push(`motivation ${f.motivation}`)
        if (f.sleep !== null) parts.push(`sleep ${f.sleep}`)
        if (f.stress !== null) parts.push(`stress ${f.stress}`)
        return `  - ${f.date}: ${parts.join(', ')}${f.notes ? ` — "${f.notes}"` : ''}`
      })
      sections.push(`\n## Session Feedback (last 15)\n${feedbackLines.join('\n')}`)
    }

    if (acceptedSuggestions.length > 0) {
      const suggestionLines = acceptedSuggestions.map(s => {
        const d = s.details as any
        return `  - ${new Date(s.suggestedAt).toISOString().split('T')[0]} [${s.eventType}]: ${d?.title || ''}`
      })
      sections.push(`\n## Recently Accepted Coaching Suggestions\n${suggestionLines.join('\n')}`)
    }

    return sections.join('\n')
  } catch (err: any) {
    console.error('Failed to build program context:', err.message)
    return ''
  }
}
