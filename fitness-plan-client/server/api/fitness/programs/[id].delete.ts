import { eq, and, inArray, sql } from 'drizzle-orm'
import { db } from '../../../utils/db'
import { userPrograms } from '../../../../database/schema/user-config'
import { workoutAssignments } from '../../../../database/schema/assignments'
import { workoutLogs } from '../../../../database/schema/tracking'
import { programTemplates } from '../../../../database/schema/templates'

export default defineEventHandler(async (event) => {
  const user = event.context.user
  if (!user) throw createError({ statusCode: 401, statusMessage: 'Unauthorized' })

  const id = getRouterParam(event, 'id')
  if (!id) throw createError({ statusCode: 400, statusMessage: 'Program ID required' })

  const existing = await db.query.userPrograms.findFirst({
    where: and(eq(userPrograms.id, id), eq(userPrograms.userId, user.id)),
  })
  if (!existing) throw createError({ statusCode: 404, statusMessage: 'Program not found' })

  try {
    await db.transaction(async (tx) => {
      const assignmentIds = await tx.select({ id: workoutAssignments.id })
        .from(workoutAssignments)
        .where(eq(workoutAssignments.userProgramId, id))

      if (assignmentIds.length > 0) {
        await tx.update(workoutLogs)
          .set({ assignmentId: null })
          .where(inArray(workoutLogs.assignmentId, assignmentIds.map(a => a.id)))
      }

      await tx.update(userPrograms)
        .set({ currentPhaseId: null })
        .where(eq(userPrograms.id, id))

      await tx.delete(userPrograms).where(eq(userPrograms.id, id))

      const templateInUse = await tx.select({ id: userPrograms.id })
        .from(userPrograms)
        .where(eq(userPrograms.templateId, existing.templateId))
        .limit(1)

      if (templateInUse.length === 0) {
        await tx.delete(programTemplates)
          .where(and(
            eq(programTemplates.id, existing.templateId),
            sql`${programTemplates.isSystem} = false`,
          ))
      }
    })

    return { ok: true }
  } catch (err: any) {
    console.error('Program delete error:', err.message)
    throw createError({ statusCode: 500, statusMessage: 'Failed to delete program' })
  }
})
