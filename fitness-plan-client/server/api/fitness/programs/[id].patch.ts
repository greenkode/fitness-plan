import { eq, and } from 'drizzle-orm'
import { db } from '../../../utils/db'
import { userPrograms } from '../../../../database/schema/user-config'

export default defineEventHandler(async (event) => {
  const user = event.context.user
  const id = getRouterParam(event, 'id')
  const body = await readBody(event)

  if (!id) {
    throw createError({ statusCode: 400, statusMessage: 'Program ID required' })
  }

  try {
    const updates: Record<string, unknown> = {}

    if (body.status === 'paused') {
      updates.status = 'paused'
      updates.pausedAt = new Date()
    } else if (body.status === 'active') {
      updates.status = 'active'
      updates.pausedAt = null
    } else if (body.status === 'completed') {
      updates.status = 'completed'
      updates.completedAt = new Date()
    }

    const [updated] = await db.update(userPrograms)
      .set(updates)
      .where(and(eq(userPrograms.id, id), eq(userPrograms.userId, user.id)))
      .returning()

    if (!updated) {
      throw createError({ statusCode: 404, statusMessage: 'Program not found' })
    }

    return updated
  } catch (err: any) {
    if (err.statusCode) throw err
    console.error('Program update error:', err.message)
    throw createError({ statusCode: 500, statusMessage: 'Failed to update program' })
  }
})
