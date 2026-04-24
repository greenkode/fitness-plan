CREATE TABLE "user_sessions" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"user_id" uuid NOT NULL,
	"token_hash" text NOT NULL,
	"expires_at" timestamp with time zone NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "users" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"email" text NOT NULL,
	"password_hash" text,
	"name" text,
	"avatar_url" text,
	"provider" text DEFAULT 'email' NOT NULL,
	"provider_id" text,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	"updated_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "program_templates" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"name" text NOT NULL,
	"description" text,
	"category" text,
	"difficulty_level" text,
	"is_system" boolean DEFAULT false NOT NULL,
	"created_by" uuid
);
--> statement-breakpoint
CREATE TABLE "template_exercises" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"block_id" uuid NOT NULL,
	"name" text NOT NULL,
	"prescription" text,
	"sort_order" integer DEFAULT 0 NOT NULL
);
--> statement-breakpoint
CREATE TABLE "template_phases" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"template_id" uuid NOT NULL,
	"phase_number" integer NOT NULL,
	"name" text NOT NULL,
	"theme" text,
	"description" text,
	"progression_criteria" jsonb
);
--> statement-breakpoint
CREATE TABLE "template_workout_blocks" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"template_workout_id" uuid NOT NULL,
	"block_key" text NOT NULL,
	"title" text NOT NULL,
	"sort_order" integer DEFAULT 0 NOT NULL
);
--> statement-breakpoint
CREATE TABLE "template_workout_tips" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"template_workout_id" uuid NOT NULL,
	"tip_text" text NOT NULL,
	"sort_order" integer DEFAULT 0 NOT NULL
);
--> statement-breakpoint
CREATE TABLE "template_workouts" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"phase_id" uuid NOT NULL,
	"workout_type" text NOT NULL,
	"title" text NOT NULL,
	"estimated_duration" integer,
	"sort_order" integer DEFAULT 0 NOT NULL
);
--> statement-breakpoint
CREATE TABLE "nutrition_templates" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"template_id" uuid NOT NULL,
	"training_type" text NOT NULL,
	"summary" text,
	"calories" integer
);
--> statement-breakpoint
CREATE TABLE "template_meal_alternatives" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"meal_id" uuid NOT NULL,
	"alternative_text" text NOT NULL,
	"sort_order" integer DEFAULT 0 NOT NULL
);
--> statement-breakpoint
CREATE TABLE "template_meals" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"nutrition_template_id" uuid NOT NULL,
	"name" text NOT NULL,
	"meal_time" text,
	"protein" text,
	"content" text,
	"image_url" text,
	"sort_order" integer DEFAULT 0 NOT NULL
);
--> statement-breakpoint
CREATE TABLE "template_recipe_ingredients" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"recipe_id" uuid NOT NULL,
	"ingredient" text NOT NULL,
	"sort_order" integer DEFAULT 0 NOT NULL
);
--> statement-breakpoint
CREATE TABLE "template_recipes" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"meal_id" uuid NOT NULL,
	"title" text NOT NULL,
	"instructions" text,
	CONSTRAINT "template_recipes_meal_id_unique" UNIQUE("meal_id")
);
--> statement-breakpoint
CREATE TABLE "template_snacks" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"nutrition_template_id" uuid NOT NULL,
	"name" text NOT NULL,
	"suggestion" text,
	"calories" integer,
	"sort_order" integer DEFAULT 0 NOT NULL
);
--> statement-breakpoint
CREATE TABLE "user_ai_settings" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"user_id" uuid NOT NULL,
	"provider" text DEFAULT 'ollama' NOT NULL,
	"model_id" text,
	"api_key_enc" text,
	"base_url" text,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	"updated_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "user_profiles" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"user_id" uuid NOT NULL,
	"experience_level" text DEFAULT 'beginner' NOT NULL,
	"goals" jsonb,
	"available_equipment" jsonb,
	"body_weight_kg" real,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	"updated_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "user_programs" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"user_id" uuid NOT NULL,
	"template_id" uuid NOT NULL,
	"started_at" timestamp with time zone DEFAULT now() NOT NULL,
	"current_phase_id" uuid,
	"status" text DEFAULT 'active' NOT NULL,
	"paused_at" timestamp with time zone,
	"completed_at" timestamp with time zone
);
--> statement-breakpoint
CREATE TABLE "user_schedules" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"user_id" uuid NOT NULL,
	"day_of_week" integer NOT NULL,
	"training_type" text NOT NULL
);
--> statement-breakpoint
CREATE TABLE "workout_assignments" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"user_program_id" uuid NOT NULL,
	"assigned_date" date NOT NULL,
	"template_workout_id" uuid NOT NULL,
	"status" text DEFAULT 'pending' NOT NULL,
	"notes" text
);
--> statement-breakpoint
CREATE TABLE "exercise_logs" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"workout_log_id" uuid NOT NULL,
	"template_exercise_id" uuid,
	"exercise_name" text NOT NULL,
	"sort_order" integer DEFAULT 0 NOT NULL
);
--> statement-breakpoint
CREATE TABLE "set_logs" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"exercise_log_id" uuid NOT NULL,
	"set_number" integer NOT NULL,
	"weight_kg" real,
	"reps" integer,
	"duration_seconds" integer,
	"rpe" real,
	"completed" boolean DEFAULT true NOT NULL,
	"notes" text
);
--> statement-breakpoint
CREATE TABLE "workout_logs" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"user_id" uuid NOT NULL,
	"assignment_id" uuid,
	"workout_date" date NOT NULL,
	"started_at" timestamp with time zone,
	"completed_at" timestamp with time zone,
	"notes" text,
	"overall_rpe" real
);
--> statement-breakpoint
CREATE TABLE "session_feedback" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"user_id" uuid NOT NULL,
	"workout_log_id" uuid,
	"feedback_date" date NOT NULL,
	"fatigue_level" integer,
	"soreness_level" integer,
	"motivation_level" integer,
	"sleep_quality" integer,
	"stress_level" integer,
	"free_text" text,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "coaching_events" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"user_id" uuid NOT NULL,
	"event_type" text NOT NULL,
	"details" jsonb,
	"suggested_at" timestamp with time zone DEFAULT now() NOT NULL,
	"accepted" boolean,
	"resolved_at" timestamp with time zone
);
--> statement-breakpoint
CREATE TABLE "golden_rules" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"rule_number" integer NOT NULL,
	"title" text NOT NULL,
	"subtitle" text,
	"content" jsonb
);
--> statement-breakpoint
CREATE TABLE "rag_embeddings" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"user_id" uuid,
	"content" text NOT NULL,
	"embedding" text,
	"metadata" jsonb,
	"chunk_index" integer,
	"source_type" text NOT NULL,
	"source_id" text
);
--> statement-breakpoint
ALTER TABLE "user_sessions" ADD CONSTRAINT "user_sessions_user_id_users_id_fk" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "program_templates" ADD CONSTRAINT "program_templates_created_by_users_id_fk" FOREIGN KEY ("created_by") REFERENCES "public"."users"("id") ON DELETE no action ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "template_exercises" ADD CONSTRAINT "template_exercises_block_id_template_workout_blocks_id_fk" FOREIGN KEY ("block_id") REFERENCES "public"."template_workout_blocks"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "template_phases" ADD CONSTRAINT "template_phases_template_id_program_templates_id_fk" FOREIGN KEY ("template_id") REFERENCES "public"."program_templates"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "template_workout_blocks" ADD CONSTRAINT "template_workout_blocks_template_workout_id_template_workouts_id_fk" FOREIGN KEY ("template_workout_id") REFERENCES "public"."template_workouts"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "template_workout_tips" ADD CONSTRAINT "template_workout_tips_template_workout_id_template_workouts_id_fk" FOREIGN KEY ("template_workout_id") REFERENCES "public"."template_workouts"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "template_workouts" ADD CONSTRAINT "template_workouts_phase_id_template_phases_id_fk" FOREIGN KEY ("phase_id") REFERENCES "public"."template_phases"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "nutrition_templates" ADD CONSTRAINT "nutrition_templates_template_id_program_templates_id_fk" FOREIGN KEY ("template_id") REFERENCES "public"."program_templates"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "template_meal_alternatives" ADD CONSTRAINT "template_meal_alternatives_meal_id_template_meals_id_fk" FOREIGN KEY ("meal_id") REFERENCES "public"."template_meals"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "template_meals" ADD CONSTRAINT "template_meals_nutrition_template_id_nutrition_templates_id_fk" FOREIGN KEY ("nutrition_template_id") REFERENCES "public"."nutrition_templates"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "template_recipe_ingredients" ADD CONSTRAINT "template_recipe_ingredients_recipe_id_template_recipes_id_fk" FOREIGN KEY ("recipe_id") REFERENCES "public"."template_recipes"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "template_recipes" ADD CONSTRAINT "template_recipes_meal_id_template_meals_id_fk" FOREIGN KEY ("meal_id") REFERENCES "public"."template_meals"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "template_snacks" ADD CONSTRAINT "template_snacks_nutrition_template_id_nutrition_templates_id_fk" FOREIGN KEY ("nutrition_template_id") REFERENCES "public"."nutrition_templates"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "user_ai_settings" ADD CONSTRAINT "user_ai_settings_user_id_users_id_fk" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "user_profiles" ADD CONSTRAINT "user_profiles_user_id_users_id_fk" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "user_programs" ADD CONSTRAINT "user_programs_user_id_users_id_fk" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "user_programs" ADD CONSTRAINT "user_programs_template_id_program_templates_id_fk" FOREIGN KEY ("template_id") REFERENCES "public"."program_templates"("id") ON DELETE no action ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "user_programs" ADD CONSTRAINT "user_programs_current_phase_id_template_phases_id_fk" FOREIGN KEY ("current_phase_id") REFERENCES "public"."template_phases"("id") ON DELETE no action ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "user_schedules" ADD CONSTRAINT "user_schedules_user_id_users_id_fk" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "workout_assignments" ADD CONSTRAINT "workout_assignments_user_program_id_user_programs_id_fk" FOREIGN KEY ("user_program_id") REFERENCES "public"."user_programs"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "workout_assignments" ADD CONSTRAINT "workout_assignments_template_workout_id_template_workouts_id_fk" FOREIGN KEY ("template_workout_id") REFERENCES "public"."template_workouts"("id") ON DELETE no action ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "exercise_logs" ADD CONSTRAINT "exercise_logs_workout_log_id_workout_logs_id_fk" FOREIGN KEY ("workout_log_id") REFERENCES "public"."workout_logs"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "exercise_logs" ADD CONSTRAINT "exercise_logs_template_exercise_id_template_exercises_id_fk" FOREIGN KEY ("template_exercise_id") REFERENCES "public"."template_exercises"("id") ON DELETE no action ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "set_logs" ADD CONSTRAINT "set_logs_exercise_log_id_exercise_logs_id_fk" FOREIGN KEY ("exercise_log_id") REFERENCES "public"."exercise_logs"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "workout_logs" ADD CONSTRAINT "workout_logs_user_id_users_id_fk" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "workout_logs" ADD CONSTRAINT "workout_logs_assignment_id_workout_assignments_id_fk" FOREIGN KEY ("assignment_id") REFERENCES "public"."workout_assignments"("id") ON DELETE no action ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "session_feedback" ADD CONSTRAINT "session_feedback_user_id_users_id_fk" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "session_feedback" ADD CONSTRAINT "session_feedback_workout_log_id_workout_logs_id_fk" FOREIGN KEY ("workout_log_id") REFERENCES "public"."workout_logs"("id") ON DELETE no action ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "coaching_events" ADD CONSTRAINT "coaching_events_user_id_users_id_fk" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "rag_embeddings" ADD CONSTRAINT "rag_embeddings_user_id_users_id_fk" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
CREATE UNIQUE INDEX "users_email_idx" ON "users" USING btree ("email");--> statement-breakpoint
CREATE UNIQUE INDEX "template_phases_template_phase_idx" ON "template_phases" USING btree ("template_id","phase_number");--> statement-breakpoint
CREATE UNIQUE INDEX "user_ai_settings_user_id_idx" ON "user_ai_settings" USING btree ("user_id");--> statement-breakpoint
CREATE UNIQUE INDEX "user_profiles_user_id_idx" ON "user_profiles" USING btree ("user_id");--> statement-breakpoint
CREATE UNIQUE INDEX "user_schedules_user_day_idx" ON "user_schedules" USING btree ("user_id","day_of_week");--> statement-breakpoint
CREATE UNIQUE INDEX "workout_assignments_program_date_idx" ON "workout_assignments" USING btree ("user_program_id","assigned_date");--> statement-breakpoint
CREATE UNIQUE INDEX "golden_rules_rule_number_idx" ON "golden_rules" USING btree ("rule_number");--> statement-breakpoint
CREATE INDEX "rag_embeddings_source_type_idx" ON "rag_embeddings" USING btree ("source_type");