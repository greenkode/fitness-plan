import { eq, and } from 'drizzle-orm'
import { db } from '../../../utils/db'
import { workoutAssignments } from '../../../../database/schema/assignments'
import { templateWorkouts, templateWorkoutBlocks, templateExercises } from '../../../../database/schema/templates'
import { userPrograms } from '../../../../database/schema/user-config'

export default defineEventHandler(async (event) => {
  const user = event.context.user
  if (!user) throw createError({ statusCode: 401, statusMessage: 'Unauthorized' })

  const date = getRouterParam(event, 'date')
  if (!date) throw createError({ statusCode: 400, statusMessage: 'Date required' })

  try {
    const assignment = await db.select({
      id: workoutAssignments.id,
      assignedDate: workoutAssignments.assignedDate,
      status: workoutAssignments.status,
      templateWorkoutId: workoutAssignments.templateWorkoutId,
      userProgramId: workoutAssignments.userProgramId,
      workoutTitle: templateWorkouts.title,
      workoutType: templateWorkouts.workoutType,
      estimatedDuration: templateWorkouts.estimatedDuration,
    })
      .from(workoutAssignments)
      .innerJoin(userPrograms, eq(workoutAssignments.userProgramId, userPrograms.id))
      .innerJoin(templateWorkouts, eq(workoutAssignments.templateWorkoutId, templateWorkouts.id))
      .where(and(
        eq(userPrograms.userId, user.id),
        eq(workoutAssignments.assignedDate, date),
      ))
      .limit(1)

    if (!assignment.length) {
      return null
    }

    const a = assignment[0]

    const blocks = await db.select({
      id: templateWorkoutBlocks.id,
      blockKey: templateWorkoutBlocks.blockKey,
      title: templateWorkoutBlocks.title,
      sortOrder: templateWorkoutBlocks.sortOrder,
    })
      .from(templateWorkoutBlocks)
      .where(eq(templateWorkoutBlocks.templateWorkoutId, a.templateWorkoutId))
      .orderBy(templateWorkoutBlocks.sortOrder)

    const blocksWithExercises = await Promise.all(blocks.map(async (block) => {
      const exercises = await db.select({
        id: templateExercises.id,
        name: templateExercises.name,
        prescription: templateExercises.prescription,
        sortOrder: templateExercises.sortOrder,
      })
        .from(templateExercises)
        .where(eq(templateExercises.blockId, block.id))
        .orderBy(templateExercises.sortOrder)

      return { ...block, exercises }
    }))

    return {
      assignmentId: a.id,
      date: a.assignedDate,
      status: a.status,
      workoutType: a.workoutType,
      title: a.workoutTitle,
      estimatedDuration: a.estimatedDuration,
      blocks: blocksWithExercises,
    }
  } catch (err: any) {
    console.error('Assignment fetch error:', err.message)
    throw createError({ statusCode: 500, statusMessage: 'Failed to fetch assignment' })
  }
})
