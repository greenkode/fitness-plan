# Fitness Tracking Web Application

## Context

The current fitness plan is a static PWA (single HTML file with hardcoded data). The goal is to evolve it into a full web application with a database backend, multi-user auth, workout logging, and adaptive progression. This enables tracking actual performance, getting feedback, and having the program evolve based on real data.

## Tech Stack

- **Backend**: Spring Boot + Kotlin (CQRS, Flyway, PostgreSQL)
- **Frontend**: Vue 3 + TypeScript + Vite (PWA)
- **Database**: PostgreSQL
- **Hosting**: Railway (backend + DB) + static frontend
- **Auth**: JWT (access token in memory, refresh token in httpOnly cookie)

## Project Structure: Monorepo

```
fitness-app/
  backend/
    src/main/kotlin/com/fitnessplan/
      FitnessPlanApplication.kt
      config/          # SecurityConfig, JwtConfig, CorsConfig
      common/          # SecurityContextHelper
      auth/            # controller, command, commandhandler, query, queryhandler, model, repository
      program/         # controller, command, commandhandler, query, queryhandler, model, repository
      tracking/        # controller, command, commandhandler, query, queryhandler, model, repository
    src/main/resources/
      application.yml
      db/migration/    # V1-V7 Flyway migrations
    build.gradle.kts
    Dockerfile
  frontend/
    src/
      main.ts, App.vue
      router/          # Route definitions
      stores/          # Pinia: auth, program, tracking, ui
      api/             # Axios client + API modules
      types/           # TypeScript interfaces
      components/      # calendar/, workout/, nutrition/, tracking/, common/
      views/           # Login, Register, Calendar, WorkoutDetail, History, Dashboard
      composables/     # useAuth, useProgram, useCalendar, useTheme, usePwa
      utils/           # date.ts, week.ts (port existing week calculation logic)
    vite.config.ts
    Dockerfile
  docker-compose.yml
  docker-compose.dev.yml
```

## Database Schema

### Core Tables (V1-V4 migrations, all idempotent)

**users** — id (UUID PK), email (unique), password_hash, display_name, created_at, updated_at

**programs** — id (UUID PK), name, description, start_date, week_count (default 4), is_default (boolean)

**weeks** — id (UUID PK), program_id (FK), week_number, name, theme. Unique(program_id, week_number)

**days** — id (UUID PK), week_id (FK), day_of_week (0-6), day_name, type, type_label, title, duration, diet_type. Unique(week_id, day_of_week)

**workout_blocks** — id (UUID PK), day_id (FK), block_key, title, sort_order

**exercises** — id (UUID PK), workout_block_id (FK), name, prescription, sort_order

**day_tips** — id (UUID PK), day_id (FK), tip_text, sort_order

**nutrition_plans** — id (UUID PK), program_id (FK), day_of_week, summary, calories, diet_type

**meals** — id (UUID PK), nutrition_plan_id (FK), name, meal_time, protein, content, image_url, sort_order

**recipes** — id (UUID PK), meal_id (FK unique), title, instructions

**recipe_ingredients** — id (UUID PK), recipe_id (FK), ingredient, sort_order

**meal_alternatives** — id (UUID PK), meal_id (FK), alternative_text, sort_order

**snacks** — id (UUID PK), nutrition_plan_id (FK), name, suggestion, calories, sort_order

**workout_logs** — id (UUID PK), user_id (FK), day_id (FK), workout_date, started_at, completed_at, notes, overall_rpe

**exercise_logs** — id (UUID PK), workout_log_id (FK), exercise_id (FK), sort_order

**set_logs** — id (UUID PK), exercise_log_id (FK), set_number, weight_kg, reps, duration_seconds, rpe, completed, notes

**golden_rules** — id (UUID PK), rule_number (unique), title, subtitle, content (JSONB)

### Seed Migrations (V5-V7)

Transform the existing `monthlyProgram` (lines 2476-2912 of fitness-calendar.html), `nutritionByDay` (lines 2914-2997), and golden rules into SQL INSERT ... ON CONFLICT DO NOTHING statements. Use deterministic UUIDs (UUID v5) for idempotency.

## API Endpoints

### Auth

| Method | Path | Handler |
|--------|------|---------|
| POST | /api/auth/register | RegisterCommandHandler |
| POST | /api/auth/login | LoginCommandHandler |
| POST | /api/auth/refresh | RefreshTokenCommandHandler |
| GET | /api/auth/me | CurrentUserQueryHandler |

### Program (read-only for MVP)

| Method | Path | Handler |
|--------|------|---------|
| GET | /api/programs/current | GetProgramQueryHandler |
| GET | /api/programs/current/day?date=2026-02-15 | GetDayDetailQueryHandler |
| GET | /api/nutrition?dayOfWeek=0 | GetNutritionForDayQueryHandler |
| GET | /api/golden-rules | GetGoldenRulesQueryHandler |

### Tracking

| Method | Path | Handler |
|--------|------|---------|
| POST | /api/workout-logs | LogWorkoutCommandHandler |
| PUT | /api/workout-logs/{id} | UpdateWorkoutLogCommandHandler |
| POST | /api/workout-logs/{id}/exercises | LogExerciseCommandHandler |
| GET | /api/workout-logs?from=&to= | GetWorkoutLogsQueryHandler |
| GET | /api/exercises/{id}/history | GetExerciseHistoryQueryHandler |

## Authentication Flow

1. Registration/Login accepts password as `CharArray`, hashed with bcrypt, char array zeroed immediately
2. JWT access token (15 min) returned in response body, stored in Vue auth store (memory only)
3. Refresh token (7 day) set as httpOnly cookie
4. `JwtAuthenticationFilter` validates Bearer token, sets SecurityContext
5. `SecurityContextHelper.getCurrentUserId()` used by all handlers — user ID never passed from controllers

## Frontend Architecture

### Routes

- `/login`, `/register` — guest only
- `/` — CalendarView (main)
- `/workout/:date` — WorkoutDetailView (active workout logging)
- `/history` — HistoryView
- `/dashboard` — DashboardView (Phase 2)

### Key Components

- `MonthCalendar.vue` — port existing renderMonthCalendar logic
- `CalendarCell.vue` — day cell with type badge, week indicator
- `WorkoutModal.vue` — workout details + nutrition
- `WorkoutLogger.vue` — log sets with weight/reps/RPE per exercise
- `SetInput.vue` — individual set logging row

### Composables

- `useCalendar()` — port getWeekNumber/getDayOfWeek from existing JS (lines 2999-3019)
- `useAuth()` — login, register, token management
- `useProgram()` — fetch and cache program data
- `useTheme()` — dark/light toggle with localStorage

## Deployment

### Railway Setup

- **PostgreSQL**: Managed Railway Postgres
- **Backend**: Deploy from `/backend` Dockerfile, auto-deploy on push to main
- **Frontend**: Static build from `/frontend`, served via Nginx container or Railway static hosting
- Backend env: `DATABASE_URL`, `JWT_SECRET`, `SPRING_PROFILES_ACTIVE=prod`

### Docker Compose (local dev)

- postgres:16-alpine on port 5432
- backend on port 8080
- frontend on port 5173 (Vite dev server)

## Phased Implementation

### Phase 1: Core MVP (~6 weeks)

**1.1 Project Scaffolding (Week 1)**
- Init Spring Boot project (Kotlin, Flyway, Spring Security, Spring Data JPA)
- Init Vue 3 project (Vite, TypeScript, Pinia, Vue Router, vite-plugin-pwa)
- Docker Compose for local dev
- Create V1-V7 Flyway migrations + seed data

**1.2 Authentication (Week 2)**
- User model + repository
- PasswordService (char[] handling)
- JwtService + JwtAuthenticationFilter
- SecurityConfig + SecurityContextHelper
- Auth CQRS handlers + controller
- Vue: LoginView, RegisterView, auth store, axios JWT interceptor

**1.3 Program Display (Week 3)**
- Program query handlers + controller
- Nutrition query handler + controller
- Golden rules endpoint
- Vue: CalendarView, MonthCalendar, CalendarCell, WorkoutModal
- Port week rotation logic to TypeScript + Kotlin
- NutritionSection, MealCard components

**1.4 Workout Logging (Weeks 4-5)**
- WorkoutLog/ExerciseLog/SetLog models + repositories
- Logging command handlers + controller
- History query handlers
- Vue: WorkoutLogger, SetInput, WorkoutHistory components

**1.5 PWA + Polish (Week 6)**
- Workbox service worker config
- Offline support (cache program data, queue logs in IndexedDB)
- Theme toggle, responsive design, install banner
- Deploy to Railway

### Phase 2: Intelligence (~4 weeks)

- Auto-progression engine (suggest +2.5kg when RPE < 8 for 2 sessions)
- Dashboard with charts (Chart.js — volume over time, completion rate, streaks)
- Rest timer during workouts
- Exercise alternatives/swaps

### Phase 3: Advanced (~4 weeks)

- Body metrics tracking (weight, measurements)
- AI feedback integration (LLM-powered program adjustments)
- Social features (share progress)
- Custom program builder

## Critical Source Files

| File | Lines | Contains |
|------|-------|----------|
| fitness-calendar.html | 2476-2912 | `monthlyProgram` — all 4 weeks, 28 days of workout data to seed |
| fitness-calendar.html | 2914-2997 | `nutritionByDay` — 7 days of meals/recipes to seed |
| fitness-calendar.html | 2999-3019 | `getWeekNumber()` + `getDayData()` — week rotation logic to port |
| fitness-calendar.html | 3051-3089 | `renderMonthCalendar()` — calendar grid reference for Vue component |
| sw.js | full file | Service worker strategy reference for Workbox config |

## Verification

1. `docker-compose up` starts all services, Flyway runs migrations successfully
2. Register a user, login, verify JWT flow
3. GET /api/programs/current/day?date=today returns correct workout for the date
4. Calendar view renders month correctly with week badges
5. Click a day → modal shows workout + nutrition
6. Log a workout with sets → appears in history
7. PWA installs on mobile, works offline for cached program data
8. Deploy to Railway — full flow works in production
