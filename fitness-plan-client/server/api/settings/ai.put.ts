import { eq } from 'drizzle-orm'
import { db } from '../../utils/db'
import { userAiSettings } from '../../../database/schema'
import { aiSettingsSchema } from '../../utils/validation'
import { encrypt } from '../../utils/crypto'

export default defineEventHandler(async (event) => {
  const user = event.context.user
  const body = await readValidatedBody(event, aiSettingsSchema.parse)

  const apiKeyEnc = body.apiKey ? encrypt(body.apiKey) : null

  const existing = await db.query.userAiSettings.findFirst({
    where: eq(userAiSettings.userId, user.id),
  })

  if (existing) {
    await db.update(userAiSettings)
      .set({
        provider: body.provider,
        modelId: body.modelId,
        apiKeyEnc: apiKeyEnc ?? existing.apiKeyEnc,
        baseUrl: body.baseUrl,
        updatedAt: new Date(),
      })
      .where(eq(userAiSettings.userId, user.id))
  } else {
    await db.insert(userAiSettings).values({
      userId: user.id,
      provider: body.provider,
      modelId: body.modelId,
      apiKeyEnc,
      baseUrl: body.baseUrl,
    })
  }

  return {
    provider: body.provider,
    modelId: body.modelId,
    hasApiKey: !!apiKeyEnc || !!existing?.apiKeyEnc,
    baseUrl: body.baseUrl,
  }
})
