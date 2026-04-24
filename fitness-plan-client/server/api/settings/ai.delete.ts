import { eq } from 'drizzle-orm'
import { db } from '../../utils/db'
import { userAiSettings } from '../../../database/schema'

export default defineEventHandler(async (event) => {
  const user = event.context.user

  await db.update(userAiSettings)
    .set({ apiKeyEnc: null, updatedAt: new Date() })
    .where(eq(userAiSettings.userId, user.id))

  setResponseStatus(event, 204)
  return null
})
