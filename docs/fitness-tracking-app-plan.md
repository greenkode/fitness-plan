# Krachtix Fitness — Application Plan

## Context

Multi-user fitness tracking application built on the existing Krachtix platform. Each user gets a fully dynamic, personalized experience: they pick a program template, set their own training schedule, and the AI adapts their progression based on logged performance and feedback. No fixed rotations — the system learns and adjusts.

## Current State

### What Exists

**Backend (`fitness-plan-backend/`)** — Krachtix platform, fully operational:
- 5 microservices: identity-ms (port 9193), core-ms (port 9192), audit-ms (port 9180), rag-ms, notification
- OAuth2/OIDC auth server with 2FA, trusted devices, rate limiting, account lockout
- CQRS with Pipelinr, Spring Modulith, multi-tenant architecture
- Billing/Stripe, Hazelcast caching, Hibernate Search
- RAG microservice with LangChain4j and pgVector
- 61 Flyway migrations, KotlinLogging, Maven build (Java 21, Kotlin 2.1)
- Package: `com.krachtix.ledger`

**Frontend (`fitness-plan-client/`)** — Nuxt 3 proof-of-concept:
- Static display of 4 hardcoded workout days (`data/program.ts`)
- `CalendarCard.vue` — shows day label, movement, sets with weight/reps/RPE
- `FeedbackCard.vue` — textarea + Submit/Ask AI buttons (non-functional)
- `useCalendar` composable — date formatting only
- Pinia installed but no stores created
- PWA manifest + service worker (offline caching)
- Dark theme with Tailwind CSS, teal/cyan accents
- Single route (`/`), no auth, no API integration

**Static PWA (`fitness-calendar.html`)** — legacy reference:
- 4-week basics-focused program (Foundation → Strength Building → Progressive Overload → Integration)
- 7-day nutrition plans with meals, recipes, alternatives
- Golden rules content
- Source data for the first program template

## Tech Stack

- **Full-Stack**: Nuxt 3 + TypeScript (Nitro server routes for API, Vue SPA for frontend)
- **ORM**: Drizzle ORM (SQL-like TS syntax, migrations)
- **AI Orchestration**: Mastra (agents, RAG pipelines, model router — 3,600+ models, 99 providers)
- **Chat UI**: Nuxt UI Chat Components + Vercel AI SDK (streaming, tool calling)
- **LLM**: BYOK multi-provider (Ollama default, OpenAI, Anthropic, Google, Mistral, Groq)
- **RAG**: Mastra RAG + pgvector + @mastra/fastembed
- **Database**: PostgreSQL + pgvector
- **Auth**: nuxt-auth-utils (session-based, OAuth providers)
- **State**: Pinia
- **Validation**: Zod
- **Build**: nuxi (single artifact)

## Core Concepts

### Multi-User, Fully Dynamic

Every aspect is per-user and configurable:

- **Program**: User selects from a template library, gets their own copy to customize
- **Schedule**: User defines exactly which days they train and what type (gym, cardio, recovery, rest) — any combination
- **Progression**: AI-adaptive — no fixed cycle length. The system analyzes performance logs and feedback to decide when to advance phases, adjust volume, or suggest deloads
- **Workouts**: Assigned dynamically based on the user's active program phase + their schedule for that day

### Template Library

Templates are read-only blueprints. The first template is the current basics program from `fitness-calendar.html`. Templates define:
- Phases (not fixed "weeks" — phases can be any duration)
- Workout types per phase (what exercises, blocks, prescriptions)
- Nutrition guidelines per training type (training day, active day, recovery day)
- Progression criteria (what triggers advancement to next phase)

When a user selects a template, a `user_program` is created — a mutable copy they own.

### AI-Adaptive Progression

The AI coaching engine analyzes:
- Recent workout logs (volume trends, RPE averages, completion rates)
- Session feedback (fatigue, soreness, motivation, sleep, stress)
- Progress toward goals (strength milestones, consistency streaks)

Based on this, it:
- Suggests phase transitions ("You've hit 5×5 squats at RPE 7 for 3 sessions — ready for Progressive Overload phase")
- Recommends weight increases (+2.5kg when RPE < 8 consistently)
- Proposes deloads when fatigue/soreness trends upward
- All suggestions require user confirmation before applying

## Backend — Fitness Module

### Package Structure

```
com.krachtix.ledger.fitness/
  commons/              # DTOs, enums, gateway interfaces
  core/
    config/             # Module config
    template/           # Program templates (read-only library)
    program/            # User programs (active, mutable per-user)
    schedule/           # User training schedules
    assignment/         # Workout assignments (daily workout generation)
    tracking/           # WorkoutLog, ExerciseLog, SetLog
    feedback/           # Session feedback
    nutrition/          # Nutrition templates + user nutrition
    coaching/           # AI coaching engine (calls rag-ms)
    progression/        # Progression analysis + phase transition logic
    controller/         # REST controllers
    repository/         # JPA repositories
  app/                  # Spring Boot application module config
```

### Database Schema

All migrations idempotent, no comments, no schema declarations.

**Program Templates (read-only library)**

- **program_templates** — id (UUID PK), name, description, category, difficulty_level, is_system (boolean), created_by (FK nullable)
- **template_phases** — id (UUID PK), template_id (FK), phase_number, name, theme, description, progression_criteria (JSONB). Unique(template_id, phase_number)
- **template_workouts** — id (UUID PK), phase_id (FK), workout_type (enum: push/pull/legs/upper/lower/full_body/cardio/recovery), title, estimated_duration, sort_order
- **template_workout_blocks** — id (UUID PK), template_workout_id (FK), block_key, title, sort_order
- **template_exercises** — id (UUID PK), block_id (FK), name, prescription, sort_order
- **template_workout_tips** — id (UUID PK), template_workout_id (FK), tip_text, sort_order

**Nutrition Templates**

- **nutrition_templates** — id (UUID PK), template_id (FK), training_type (enum: training_day/active_day/recovery_day), summary, calories
- **template_meals** — id (UUID PK), nutrition_template_id (FK), name, meal_time, protein, content, image_url, sort_order
- **template_recipes** — id (UUID PK), meal_id (FK unique), title, instructions
- **template_recipe_ingredients** — id (UUID PK), recipe_id (FK), ingredient, sort_order
- **template_meal_alternatives** — id (UUID PK), meal_id (FK), alternative_text, sort_order
- **template_snacks** — id (UUID PK), nutrition_template_id (FK), name, suggestion, calories, sort_order

**User Configuration**

- **user_profiles** — id (UUID PK), user_id (FK to identity, unique), experience_level (enum: beginner/intermediate/advanced), goals (JSONB), available_equipment (JSONB), body_weight_kg (nullable), created_at, updated_at
- **user_schedules** — id (UUID PK), user_id (FK), day_of_week (0-6), training_type (enum: gym/cardio/recovery/rest). Unique(user_id, day_of_week)
- **user_programs** — id (UUID PK), user_id (FK), template_id (FK), started_at, current_phase_id (FK to template_phases), status (enum: active/paused/completed), paused_at (nullable), completed_at (nullable)

**Workout Assignments (system-generated per user)**

- **workout_assignments** — id (UUID PK), user_program_id (FK), assigned_date (date), template_workout_id (FK), status (enum: pending/completed/skipped/rescheduled), notes. Unique(user_program_id, assigned_date)

**Tracking (user-generated)**

- **workout_logs** — id (UUID PK), user_id (FK), assignment_id (FK nullable), workout_date, started_at, completed_at, notes, overall_rpe
- **exercise_logs** — id (UUID PK), workout_log_id (FK), template_exercise_id (FK nullable), exercise_name, sort_order
- **set_logs** — id (UUID PK), exercise_log_id (FK), set_number, weight_kg, reps, duration_seconds, rpe, completed, notes

**Feedback**

- **session_feedback** — id (UUID PK), user_id (FK), workout_log_id (FK nullable), feedback_date, fatigue_level (1-10), soreness_level (1-10), motivation_level (1-10), sleep_quality (1-10), stress_level (1-10), free_text, created_at

**AI Coaching**

- **coaching_events** — id (UUID PK), user_id (FK), event_type (enum: phase_advance/weight_increase/volume_adjust/deload_suggest/milestone_reached), details (JSONB), suggested_at, accepted (boolean nullable), resolved_at (nullable)

**Reference Data**

- **golden_rules** — id (UUID PK), rule_number (unique), title, subtitle, content (JSONB)

### Seed Data

Transform from `fitness-calendar.html` into the first system template:
- `monthlyProgram` (lines 2476-2912) → `program_templates` + `template_phases` + `template_workouts` + `template_exercises`
- `nutritionByDay` (lines 2914-2997) → `nutrition_templates` + `template_meals` + `template_recipes`
- Golden rules → `golden_rules`
- Use deterministic UUIDs (UUID v5) for idempotency, INSERT ... ON CONFLICT DO NOTHING

### API Endpoints

Authentication handled by identity-ms. User ID from SecurityContext (never from controller).

**Templates (public, read-only)**

| Method | Path | Handler |
|--------|------|---------|
| GET | /api/fitness/templates | ListTemplatesQueryHandler |
| GET | /api/fitness/templates/{id} | GetTemplateDetailQueryHandler |

**User Profile & Schedule**

| Method | Path | Handler |
|--------|------|---------|
| POST | /api/fitness/profile | CreateProfileCommandHandler |
| PUT | /api/fitness/profile | UpdateProfileCommandHandler |
| GET | /api/fitness/profile | GetProfileQueryHandler |
| PUT | /api/fitness/schedule | SetScheduleCommandHandler |
| GET | /api/fitness/schedule | GetScheduleQueryHandler |

**User Program**

| Method | Path | Handler |
|--------|------|---------|
| POST | /api/fitness/programs | StartProgramCommandHandler (from template) |
| GET | /api/fitness/programs/active | GetActiveProgramQueryHandler |
| PUT | /api/fitness/programs/{id}/pause | PauseProgramCommandHandler |
| PUT | /api/fitness/programs/{id}/resume | ResumeProgramCommandHandler |

**Assignments (system-generated daily workouts)**

| Method | Path | Handler |
|--------|------|---------|
| GET | /api/fitness/assignments | GetAssignmentsQueryHandler (query: from, to) |
| GET | /api/fitness/assignments/today | GetTodayAssignmentQueryHandler |
| PUT | /api/fitness/assignments/{id}/skip | SkipAssignmentCommandHandler |

**Tracking**

| Method | Path | Handler |
|--------|------|---------|
| POST | /api/fitness/workout-logs | LogWorkoutCommandHandler |
| PUT | /api/fitness/workout-logs/{id} | UpdateWorkoutLogCommandHandler |
| POST | /api/fitness/workout-logs/{id}/exercises | LogExerciseCommandHandler |
| GET | /api/fitness/workout-logs | GetWorkoutLogsQueryHandler (query: from, to) |
| GET | /api/fitness/exercises/{id}/history | GetExerciseHistoryQueryHandler |

**Feedback**

| Method | Path | Handler |
|--------|------|---------|
| POST | /api/fitness/feedback | SubmitFeedbackCommandHandler |
| GET | /api/fitness/feedback | GetFeedbackQueryHandler (query: from, to) |

**Nutrition**

| Method | Path | Handler |
|--------|------|---------|
| GET | /api/fitness/nutrition/today | GetTodayNutritionQueryHandler |
| GET | /api/fitness/nutrition | GetNutritionQueryHandler (query: trainingType) |

**AI Coaching**

| Method | Path | Handler |
|--------|------|---------|
| POST | /api/fitness/chat | ChatCommandHandler (calls rag-ms) |
| GET | /api/fitness/coaching/summary | GetWeeklySummaryQueryHandler |
| GET | /api/fitness/coaching/suggestions | GetPendingSuggestionsQueryHandler |
| PUT | /api/fitness/coaching/suggestions/{id}/accept | AcceptSuggestionCommandHandler |
| PUT | /api/fitness/coaching/suggestions/{id}/dismiss | DismissSuggestionCommandHandler |

## Frontend — Nuxt 3

### Directory Structure

```
fitness-plan-client/
  pages/
    index.vue                  # Landing (unauthenticated) or dashboard (authenticated)
    login.vue                  # OAuth2 login redirect
    onboarding.vue             # Profile setup + template selection + schedule
    calendar.vue               # Month calendar view with assignments
    workout/[date].vue         # Active workout logging for a specific day
    history.vue                # Workout history timeline
    chat.vue                   # AI coaching chat interface
    settings.vue               # Profile, schedule, program management
  components/
    calendar/
      MonthCalendar.vue        # Month grid showing assignments
      CalendarCell.vue         # Individual day cell with workout type
      DayTypeBadge.vue         # Gym/cardio/recovery badge
    workout/
      WorkoutDetail.vue        # Full workout breakdown
      WorkoutLogger.vue        # Log sets with weight/reps/RPE
      SetInput.vue             # Individual set logging row
      ExerciseCard.vue         # Single exercise with sets
    nutrition/
      NutritionSection.vue     # Day's nutrition based on training type
      MealCard.vue             # Individual meal with recipe
    feedback/
      FeedbackCard.vue         # Post-session feedback (existing, needs wiring)
      FeedbackPrompts.vue      # Structured prompts (fatigue, soreness, etc.)
    chat/
      ChatWindow.vue           # AI chat interface
      ChatMessage.vue          # Individual message bubble
      ChatInput.vue            # Message input with send
    coaching/
      SuggestionCard.vue       # AI suggestion with accept/dismiss
      WeeklySummary.vue        # Performance summary card
      MilestoneAlert.vue       # Strength milestone notification
    onboarding/
      TemplateSelector.vue     # Browse and pick a program template
      ScheduleBuilder.vue      # Set training days and types
      ProfileForm.vue          # Experience level, goals, equipment
    common/
      AppHeader.vue            # Nav bar with auth state
      ThemeToggle.vue          # Dark/light switch
  composables/
    useAuth.ts                 # OAuth2 flow via identity-ms
    useProfile.ts              # User profile + schedule management
    useProgram.ts              # Active program + assignments
    useCalendar.ts             # Calendar rendering helpers
    useTracking.ts             # Workout logging API calls
    useFeedback.ts             # Feedback submission
    useChat.ts                 # AI chat interaction
    useCoaching.ts             # Suggestions, summaries
    useTheme.ts                # Dark/light toggle with localStorage
  stores/
    auth.ts                    # User state, tokens
    program.ts                 # Active program, assignments cache
    tracking.ts                # Active workout session state
    coaching.ts                # Pending suggestions, weekly summary
  types/
    template.ts                # ProgramTemplate, Phase, Workout types
    program.ts                 # UserProgram, Assignment types
    tracking.ts                # WorkoutLog, ExerciseLog, SetLog types
    feedback.ts                # Feedback types
    chat.ts                    # Chat message types
    profile.ts                 # UserProfile, Schedule types
  utils/
    date.ts                    # Date formatting helpers
```

### User Flow

1. **Register/Login** → OAuth2 via identity-ms
2. **Onboarding** (first login):
   - Set profile (experience level, goals, equipment)
   - Browse template library, pick a program
   - Set training schedule (which days, what type)
3. **Dashboard/Calendar** → Shows month view with assigned workouts per day
4. **Today's Workout** → Tap today → see assigned workout detail → log sets
5. **Post-Workout** → Feedback prompts (fatigue, soreness, etc.)
6. **Chat** → Ask AI questions, log via natural language, get coaching
7. **Suggestions** → AI suggests phase changes, weight increases — user accepts/dismisses
8. **History** → Timeline of past workouts with logged data

### Auth Integration

Frontend authenticates via identity-ms OAuth2 flow:
1. Login page redirects to identity-ms `/oauth2/authorize`
2. Callback receives authorization code
3. Exchange code for access token + refresh token
4. Access token stored in Pinia (memory only)
5. Refresh token in httpOnly cookie (handled by identity-ms)
6. Nuxt middleware protects authenticated routes

### Chat-First Workout Logging

Per `docs/features.md`, the primary logging interface is conversational:
- User types: "Bench 80kg x 8, 8, 6 @ RPE 8"
- Backend parses via AI (rag-ms) into structured set data
- Shows parsed result for confirmation before saving
- Manual form entry (`SetInput.vue`) available as fallback
- Post-session feedback collected through chat prompts

## Phased Implementation

### Phase 1: Foundation

**1.1 Backend Fitness Module Setup**
- Create fitness module within Krachtix backend (Maven submodule)
- Flyway migrations for template tables + user config tables + tracking tables
- Seed the first program template from fitness-calendar.html data
- Template query handlers (list templates, get template detail)
- Golden rules endpoint

**1.2 User Onboarding**
- User profile model + CQRS (create/update/get)
- User schedule model + CQRS (set/get)
- Start program command (creates user_program from template)
- Frontend: onboarding flow (profile → template selection → schedule builder)
- Frontend: OAuth2 auth integration with identity-ms

**1.3 Assignments & Calendar**
- Assignment generation logic (maps user schedule + active program phase → daily workouts)
- Assignment query handlers (today, date range)
- Frontend: MonthCalendar with assignments, CalendarCell, DayTypeBadge
- Frontend: workout detail view from assignment

**1.4 Workout Logging**
- WorkoutLog/ExerciseLog/SetLog models + repositories
- Logging command handlers (CQRS)
- History query handlers
- Frontend: WorkoutLogger, SetInput, ExerciseCard
- Frontend: workout history page

**1.5 Nutrition**
- Nutrition template query handlers (based on training type for the day)
- Frontend: NutritionSection, MealCard components

### Phase 2: Feedback & AI

**2.1 Session Feedback**
- Session feedback model + CQRS handlers
- Frontend: wire FeedbackCard, add FeedbackPrompts (structured)
- Feedback linked to workout logs

**2.2 Chat-Based Logging**
- ChatWindow, ChatMessage, ChatInput components
- Chat command handler that calls rag-ms for natural language parsing
- Confirmation flow before persisting parsed data
- Manual form entry as fallback

**2.3 RAG Content**
- Ingest program templates, golden rules, exercise guidance into rag-ms vector store
- Nutrition notes as retrievable context
- Exercise form cues as reference data

### Phase 3: Adaptive Coaching

**3.1 Progression Engine**
- Analyze workout logs: volume trends, RPE averages, completion rates
- Analyze feedback: fatigue/soreness/motivation trends
- Phase transition logic (when to advance, when to deload)
- Coaching events model + CQRS
- Frontend: SuggestionCard (accept/dismiss), MilestoneAlert

**3.2 Weekly Summaries**
- Aggregate logs + feedback into weekly performance summary
- AI-generated insights via rag-ms
- Frontend: WeeklySummary component

**3.3 Dashboard**
- Charts (volume over time, completion rate, streaks, RPE trends)
- Strength milestone tracking (pull-ups, dips, squat milestones)
- Phase 2 calisthenics unlock notifications

### Phase 4: Advanced

- Rest timer during active workouts
- Exercise alternatives/swaps within a template
- Body metrics tracking (weight, measurements)
- AI-generated program adjustments (LLM proposes modifications, user confirms)
- Custom program/template builder
- Export/delete workout history (GDPR compliance)
- Additional templates (PPL, upper/lower, full body 3x, calisthenics Phase 2)

## Critical Source Files

| File | Lines | Contains |
|------|-------|----------|
| fitness-calendar.html | 2476-2912 | `monthlyProgram` — 4-week basics program to seed as first template |
| fitness-calendar.html | 2914-2997 | `nutritionByDay` — 7 days of meals/recipes to seed |
| fitness-plan-client/components/FeedbackCard.vue | full | Existing feedback UI (needs wiring to API) |
| fitness-plan-client/components/CalendarCard.vue | full | Existing calendar card (needs expansion) |
| fitness-plan-client/data/program.ts | full | WorkoutDay type reference (to be replaced by API) |
| docs/features.md | full | AI-assisted features vision (chat logging, coaching, RAG) |

## Verification

1. Backend fitness module starts, Flyway migrations run against existing Krachtix DB
2. GET /api/fitness/templates returns the seeded basics program template
3. OAuth2 login from Nuxt frontend → identity-ms → redirect back with token
4. New user completes onboarding: profile → pick template → set schedule
5. GET /api/fitness/assignments/today returns correct workout for the user's schedule + phase
6. Calendar view renders month with personalized workout assignments
7. Click a day → shows assigned workout detail + nutrition for training type
8. Log a workout with sets via form → appears in history
9. Log a workout via chat ("Squat 100kg x 5, 5, 5 @ RPE 7") → parses correctly
10. Submit session feedback → stored and linked to workout
11. AI suggests phase advancement after consistent performance → user accepts → phase updates
12. PWA installs on mobile, works offline for cached assignments
