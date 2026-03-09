# Fitness Plan Features (AI-Assisted)

## Chat-First Workout Logging
- Users can log workouts via a chat UI (e.g., "Bench 80kg x 8, 8, 6 @ RPE 8").
- The assistant parses exercises, sets, reps, weight, duration, RPE, and notes.
- The assistant can ask clarifying questions when entries are ambiguous.
- Manual form entry remains available as a fallback for edge cases.

## Feedback Collection
- The chat UI collects post-session feedback (fatigue, soreness, motivation, sleep, stress).
- Users can add free-form feedback at any time (e.g., pain, technique issues).
- Feedback is linked to the workout date and specific exercises when possible.

## AI Coaching Loop
- The assistant summarizes weekly performance and recovery patterns.
- It proposes intensity adjustments based on recent performance + feedback.
- Adjustments are suggestions that require user confirmation before applying.

## Reporting & Insights
- Reports combine logged sets/reps and qualitative feedback.
- Users can request summaries in natural language (e.g., "show my squat trend").
- Reports highlight volume, consistency, and perceived exertion trends.

## RAG Microservice (rag-ms)
- The assistant can cite program rules, exercise guidance, and nutrition notes.
- Retrieval is optimized for small data volume (no ingestion service for now).

## Security & Privacy
- Users can export or delete their workout history and feedback.
- PII and health-related entries are stored and accessed with least privilege.
