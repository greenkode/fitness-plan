import { eq } from 'drizzle-orm'
import { db } from '../../../utils/db'
import { programTemplates, templatePhases, templateWorkouts, templateWorkoutBlocks, templateExercises } from '../../../../database/schema/templates'
import { userPrograms, userSchedules } from '../../../../database/schema/user-config'
import { workoutAssignments } from '../../../../database/schema/assignments'
import type { ProgramProposal } from '../../../utils/ai-schemas'

export default defineEventHandler(async (event) => {
  const user = event.context.user
  if (!user) {
    throw createError({ statusCode: 401, statusMessage: 'Unauthorized' })
  }

  const { program } = await readBody<{ program: ProgramProposal }>(event)

  if (!program?.phases?.length) {
    throw createError({ statusCode: 400, statusMessage: 'Program must have at least one phase' })
  }

  try {
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
  } catch (err: any) {
    if (err.statusCode) throw err
    console.error('Program save error:', err.message)
    throw createError({ statusCode: 500, statusMessage: 'Failed to save program' })
  }
})
