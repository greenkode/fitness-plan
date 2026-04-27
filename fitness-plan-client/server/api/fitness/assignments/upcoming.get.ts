import { eq, and, gte, lte } from 'drizzle-orm'
import { db } from '../../../utils/db'
import { workoutAssignments } from '../../../../database/schema/assignments'
import { templateWorkouts } from '../../../../database/schema/templates'
import { userPrograms } from '../../../../database/schema/user-config'

export default defineEventHandler(async (event) => {
  const user = event.context.user
  if (!user) throw createError({ statusCode: 401, statusMessage: 'Unauthorized' })

  const today = new Date()
  const weekAhead = new Date()
  weekAhead.setDate(weekAhead.getDate() + 7)

  const todayStr = today.toISOString().split('T')[0]
  const weekStr = weekAhead.toISOString().split('T')[0]

  try {
    const assignments = await db.select({
      date: workoutAssignments.assignedDate,
      status: workoutAssignments.status,
      title: templateWorkouts.title,
      workoutType: templateWorkouts.workoutType,
      estimatedDuration: templateWorkouts.estimatedDuration,
    })
      .from(workoutAssignments)
      .innerJoin(userPrograms, eq(workoutAssignments.userProgramId, userPrograms.id))
      .innerJoin(templateWorkouts, eq(workoutAssignments.templateWorkoutId, templateWorkouts.id))
      .where(and(
        eq(userPrograms.userId, user.id),
        eq(userPrograms.status, 'active'),
        gte(workoutAssignments.assignedDate, todayStr),
        lte(workoutAssignments.assignedDate, weekStr),
      ))
      .orderBy(workoutAssignments.assignedDate)

    return assignments
  } catch (err: any) {
    console.error('Upcoming assignments error:', err.message)
    throw createError({ statusCode: 500, statusMessage: 'Failed to fetch assignments' })
  }
})
