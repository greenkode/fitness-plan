import { eq } from 'drizzle-orm'
import { db } from '../../utils/db'
import { userAiSettings } from '../../../database/schema'

export default defineEventHandler(async (event) => {
  const user = event.context.user
  const settings = await db.query.userAiSettings.findFirst({
    where: eq(userAiSettings.userId, user.id),
  })

  if (!settings) {
    return { provider: 'ollama', modelId: 'llama3', hasApiKey: false, baseUrl: null }
  }

  return {
    provider: settings.provider,
    modelId: settings.modelId,
    hasApiKey: !!settings.apiKeyEnc,
    baseUrl: settings.baseUrl,
  }
})
