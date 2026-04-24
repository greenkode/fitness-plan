import { eq } from 'drizzle-orm'
import { users } from '../../../database/schema'
import { registerSchema } from '../../utils/validation'
import { db } from '../../utils/db'

export default defineEventHandler(async (event) => {
  const body = await readValidatedBody(event, registerSchema.parse)

  const existing = await db.query.users.findFirst({
    where: eq(users.email, body.email.toLowerCase()),
  })

  if (existing) {
    throw createError({ statusCode: 409, statusMessage: 'Email already registered' })
  }

  const passwordHash = await hashPassword(body.password)

  const [user] = await db.insert(users).values({
    email: body.email.toLowerCase(),
    passwordHash,
    name: body.name,
    provider: 'email',
  }).returning({ id: users.id, email: users.email, name: users.name })

  await setUserSession(event, {
    user: { id: user.id, email: user.email, name: user.name },
  })

  return { id: user.id, email: user.email, name: user.name }
})
