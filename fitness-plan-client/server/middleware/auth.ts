export default defineEventHandler(async (event) => {
  const path = getRequestURL(event).pathname

  const publicPaths = [
    '/api/auth/login',
    '/api/auth/register',
    '/api/fitness/templates',
  ]

  if (!path.startsWith('/api/') || publicPaths.some(p => path.startsWith(p))) {
    return
  }

  const session = await getUserSession(event)
  if (!session?.user) {
    throw createError({ statusCode: 401, statusMessage: 'Authentication required' })
  }

  event.context.user = session.user
})
