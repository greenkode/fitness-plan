import { eq, desc } from 'drizzle-orm'
import { db } from '../../../utils/db'
import { userPrograms } from '../../../../database/schema/user-config'
import { programTemplates, templatePhases } from '../../../../database/schema/templates'

export default defineEventHandler(async (event) => {
  const user = event.context.user

  try {
    const programs = await db.select({
      id: userPrograms.id,
      templateId: userPrograms.templateId,
      startedAt: userPrograms.startedAt,
      status: userPrograms.status,
      pausedAt: userPrograms.pausedAt,
      completedAt: userPrograms.completedAt,
      templateName: programTemplates.name,
      templateDescription: programTemplates.description,
      templateCategory: programTemplates.category,
      templateDifficulty: programTemplates.difficultyLevel,
      currentPhaseId: userPrograms.currentPhaseId,
    })
      .from(userPrograms)
      .innerJoin(programTemplates, eq(userPrograms.templateId, programTemplates.id))
      .where(eq(userPrograms.userId, user.id))
      .orderBy(desc(userPrograms.startedAt))

    const results = await Promise.all(programs.map(async (p) => {
      const phases = await db.select({
        id: templatePhases.id,
        name: templatePhases.name,
        phaseNumber: templatePhases.phaseNumber,
      }).from(templatePhases)
        .where(eq(templatePhases.templateId, p.templateId))
        .orderBy(templatePhases.phaseNumber)

      const currentPhase = phases.find(ph => ph.id === p.currentPhaseId)

      return {
        id: p.id,
        templateName: p.templateName,
        templateDescription: p.templateDescription,
        category: p.templateCategory,
        difficulty: p.templateDifficulty,
        status: p.status,
        startedAt: p.startedAt,
        pausedAt: p.pausedAt,
        completedAt: p.completedAt,
        currentPhase: currentPhase?.name || null,
        currentPhaseNumber: currentPhase?.phaseNumber || null,
        totalPhases: phases.length,
      }
    }))

    return results
  } catch (err: any) {
    if (err.statusCode) throw err
    console.error('Programs fetch error:', err.message)
    throw createError({ statusCode: 500, statusMessage: 'Failed to fetch programs' })
  }
})
