import { db } from '../../../utils/db'
import { programTemplates } from '../../../../database/schema'

export default defineEventHandler(async () => {
  return db.select({
    id: programTemplates.id,
    name: programTemplates.name,
    description: programTemplates.description,
    category: programTemplates.category,
    difficultyLevel: programTemplates.difficultyLevel,
    isSystem: programTemplates.isSystem,
  }).from(programTemplates)
})
