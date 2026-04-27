import { eq, and, gte, lte } from 'drizzle-orm'
import { db } from '../../../utils/db'
import { workoutAssignments } from '../../../../database/schema/assignments'
import { templateWorkouts } from '../../../../database/schema/templates'
import { userPrograms } from '../../../../database/schema/user-config'

export default defineEventHandler(async (event) => {
  const user = event.context.user
  if (!user) throw createError({ statusCode: 401, statusMessage: 'Unauthorized' })

  const query = getQuery(event)
  const from = (query.from as string) || new Date(new Date().setDate(new Date().getDate() - 30)).toISOString().split('T')[0]
  const to = (query.to as string) || new Date(new Date().setDate(new Date().getDate() + 60)).toISOString().split('T')[0]

  try {
    return await db.select({
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
        gte(workoutAssignments.assignedDate, from),
        lte(workoutAssignments.assignedDate, to),
      ))
      .orderBy(workoutAssignments.assignedDate)
  } catch (err: any) {
    console.error('Assignments fetch error:', err.message)
    throw createError({ statusCode: 500, statusMessage: 'Failed to fetch assignments' })
  }
})
