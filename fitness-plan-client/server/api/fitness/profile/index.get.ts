import { eq } from 'drizzle-orm'
import { db } from '../../../utils/db'
import { userProfiles } from '../../../../database/schema/user-config'

export default defineEventHandler(async (event) => {
  const user = event.context.user

  const profile = await db.query.userProfiles.findFirst({
    where: eq(userProfiles.userId, user.id),
  })

  return profile ?? null
})
