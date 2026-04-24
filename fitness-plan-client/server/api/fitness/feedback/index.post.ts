import { db } from '../../../utils/db'
import { sessionFeedback } from '../../../../database/schema/feedback'
import { feedbackSchema } from '../../../utils/validation'

export default defineEventHandler(async (event) => {
  const user = event.context.user
  const body = await readValidatedBody(event, feedbackSchema.parse)

  const [feedback] = await db.insert(sessionFeedback).values({
    userId: user.id,
    workoutLogId: body.workoutLogId,
    feedbackDate: body.feedbackDate,
    fatigueLevel: body.fatigueLevel,
    sorenessLevel: body.sorenessLevel,
    motivationLevel: body.motivationLevel,
    sleepQuality: body.sleepQuality,
    stressLevel: body.stressLevel,
    freeText: body.freeText,
  }).returning({ id: sessionFeedback.id })

  return { id: feedback.id }
})
