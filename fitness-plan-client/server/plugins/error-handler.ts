export default defineNitroPlugin((nitroApp) => {
  nitroApp.hooks.hook('error', (error: any) => {
    if (error.message?.startsWith('Failed query:')) {
      error.statusCode = error.statusCode || 500
      error.statusMessage = 'Internal Server Error'
      error.message = 'A database error occurred'
      if (error.stack) {
        error.stack = ''
      }
      if (error.data) {
        error.data = undefined
      }
    }
  })
})
