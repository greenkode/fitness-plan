import { eq } from 'drizzle-orm'
import { db } from '../../../utils/db'
import { userProfiles } from '../../../../database/schema/user-config'
import { profileSchema } from '../../../utils/validation'

export default defineEventHandler(async (event) => {
  const user = event.context.user
  const body = await readValidatedBody(event, profileSchema.parse)

  const existing = await db.query.userProfiles.findFirst({
    where: eq(userProfiles.userId, user.id),
  })

  if (existing) {
    const [updated] = await db.update(userProfiles)
      .set({
        experienceLevel: body.experienceLevel,
        goals: body.goals,
        availableEquipment: body.availableEquipment,
        bodyWeightKg: body.bodyWeightKg,
        updatedAt: new Date(),
      })
      .where(eq(userProfiles.userId, user.id))
      .returning()

    return updated
  }

  const [created] = await db.insert(userProfiles).values({
    userId: user.id,
    experienceLevel: body.experienceLevel,
    goals: body.goals,
    availableEquipment: body.availableEquipment,
    bodyWeightKg: body.bodyWeightKg,
  }).returning()

  return created
})
