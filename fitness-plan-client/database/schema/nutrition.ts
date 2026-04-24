import { pgTable, uuid, text, integer } from 'drizzle-orm/pg-core'
import { programTemplates } from './templates'

export const nutritionTemplates = pgTable('nutrition_templates', {
  id: uuid('id').defaultRandom().primaryKey(),
  templateId: uuid('template_id').notNull().references(() => programTemplates.id, { onDelete: 'cascade' }),
  trainingType: text('training_type').notNull(),
  summary: text('summary'),
  calories: integer('calories'),
})

export const templateMeals = pgTable('template_meals', {
  id: uuid('id').defaultRandom().primaryKey(),
  nutritionTemplateId: uuid('nutrition_template_id').notNull().references(() => nutritionTemplates.id, { onDelete: 'cascade' }),
  name: text('name').notNull(),
  mealTime: text('meal_time'),
  protein: text('protein'),
  content: text('content'),
  imageUrl: text('image_url'),
  sortOrder: integer('sort_order').notNull().default(0),
})

export const templateRecipes = pgTable('template_recipes', {
  id: uuid('id').defaultRandom().primaryKey(),
  mealId: uuid('meal_id').notNull().references(() => templateMeals.id, { onDelete: 'cascade' }).unique(),
  title: text('title').notNull(),
  instructions: text('instructions'),
})

export const templateRecipeIngredients = pgTable('template_recipe_ingredients', {
  id: uuid('id').defaultRandom().primaryKey(),
  recipeId: uuid('recipe_id').notNull().references(() => templateRecipes.id, { onDelete: 'cascade' }),
  ingredient: text('ingredient').notNull(),
  sortOrder: integer('sort_order').notNull().default(0),
})

export const templateMealAlternatives = pgTable('template_meal_alternatives', {
  id: uuid('id').defaultRandom().primaryKey(),
  mealId: uuid('meal_id').notNull().references(() => templateMeals.id, { onDelete: 'cascade' }),
  alternativeText: text('alternative_text').notNull(),
  sortOrder: integer('sort_order').notNull().default(0),
})

export const templateSnacks = pgTable('template_snacks', {
  id: uuid('id').defaultRandom().primaryKey(),
  nutritionTemplateId: uuid('nutrition_template_id').notNull().references(() => nutritionTemplates.id, { onDelete: 'cascade' }),
  name: text('name').notNull(),
  suggestion: text('suggestion'),
  calories: integer('calories'),
  sortOrder: integer('sort_order').notNull().default(0),
})
