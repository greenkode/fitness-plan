export default defineNuxtRouteMiddleware(async () => {
  const { loggedIn, fetch: fetchSession } = useUserSession()

  if (!loggedIn.value) {
    await fetchSession()
  }

  if (!loggedIn.value) {
    return navigateTo('/login')
  }
})
