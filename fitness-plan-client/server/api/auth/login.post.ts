import { eq } from 'drizzle-orm'
import { users } from '../../../database/schema'
import { loginSchema } from '../../utils/validation'
import { db } from '../../utils/db'

export default defineEventHandler(async (event) => {
  const body = await readValidatedBody(event, loginSchema.parse)

  const user = await db.query.users.findFirst({
    where: eq(users.email, body.email.toLowerCase()),
  })

  if (!user || !user.passwordHash) {
    throw createError({ statusCode: 401, statusMessage: 'Invalid email or password' })
  }

  const valid = await verifyPassword(user.passwordHash, body.password)
  if (!valid) {
    throw createError({ statusCode: 401, statusMessage: 'Invalid email or password' })
  }

  await setUserSession(event, {
    user: { id: user.id, email: user.email, name: user.name },
  })

  return { id: user.id, email: user.email, name: user.name }
})
