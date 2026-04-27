# Refactor: borrow AI-layer patterns from claw-code

**Goal:** apply three claw-code patterns to our AI layer — model registry, centralized JSON-extraction-with-fix-retry, and preflight validation — without over-engineering.

**Non-goals:** rewriting Mastra integration, adding new providers, adding tests infrastructure (deferred — separate decision).

**Scope:** only these files change
- `fitness-plan-client/server/utils/ai-model.ts` (refactor)
- `fitness-plan-client/server/api/ai/parse.post.ts` (refactor + fix import bug)
- `fitness-plan-client/server/api/ai/build-program.post.ts` (refactor)
- `fitness-plan-client/server/utils/ai-json.ts` (NEW)
- `fitness-plan-client/server/utils/ai-registry.ts` (NEW)

---

## Problems being fixed

1. **Duplication** — `resolveModel` and `resolveModelSet` in `ai-model.ts` have the same env-var → DB → fallback branch logic, written twice.
2. **Silent `catch {}`** at `ai-model.ts:83` swallows DB errors.
3. **Ad-hoc JSON extraction** — `text.match(/\{[\s\S]*\}/)` is copy-pasted in `parse.post.ts` and `build-program.post.ts`. Only `build-program.post.ts` has a fix-model retry; `parse.post.ts` falls straight back to regex even on recoverable JSON errors.
4. **Zod schemas exist but are unused at the boundary** — we parse JSON then stuff it into types without validating shape, so a hallucinated field shape propagates until it crashes further down.
5. **No preflight** — `build-program.post.ts` fires N parallel workout-generation calls with no context-window awareness. A wrong model ID from BYOK silently overflows or 400s.
6. **Latent bug** — `parse.post.ts:5` references `z` and `ParsedSetSchema` without importing them. The file only compiles because the function is never type-checked against that signature at a call site that would reveal it.

---

## Plan

### [x] Step 1 — Model registry (`server/utils/ai-registry.ts`)
- Introduce a `ModelDescriptor` type: `{ id, provider: 'anthropic' | 'ollama', contextWindow, maxOutputTokens }`.
- Register the handful of models we actually use (claude-sonnet-4-6, claude-opus-4-7, qwen2.5:14b, qwen2.5-coder:7b, qwen2.5:3b). Unknown models get a conservative default descriptor rather than failing.
- Export `getDescriptor(id: string): ModelDescriptor` and a factory `buildModel(descriptor, { apiKey?, baseUrl? }): LanguageModelV1` that wraps the Anthropic/OpenAI-compatible creation currently inlined in `ai-model.ts`.

### [x] Step 2 — Refactor `ai-model.ts`
- Collapse `resolveModel` + `resolveModelSet` duplication into one internal `resolveUserConfig(userId)` helper returning a typed config (`{ kind: 'anthropic', apiKey, modelId } | { kind: 'ollama', baseUrl?, modelId } | { kind: 'fallback' }`).
- Public `resolveModel` and `resolveModelSet` become thin wrappers that turn the config into models via `buildModel()` + the registry.
- Replace silent `catch {}` with a logged warning.
- Return `{ model, descriptor }` pairs so callers can access context-window info when they need it.

### [x] Step 3 — JSON-extraction helper (`server/utils/ai-json.ts`)
- `extractJson(text: string): unknown` — the regex extraction currently duplicated.
- `generateStructured<T>(opts: { model, system, prompt, schema: ZodSchema<T>, fixModel? }): Promise<T>` — calls `generateText`, tries to extract + Zod-parse, on failure optionally retries through `fixModel`, throws a typed `StructuredGenerationError` with the raw response attached for logging.
- This is the only new abstraction — justified because it de-dupes three call sites and wires in Zod validation that currently doesn't happen.

### [x] Step 4 — Refactor `parse.post.ts`
- Fix the missing `z` / `ParsedSetSchema` imports (real bug).
- Replace inline JSON extraction with `generateStructured(..., { schema: z.object({ sets: z.array(ParsedSetSchema) }) })`.
- Keep the regex fallback exactly as-is (it's a deliberate degradation, not an accident).

### [x] Step 5 — Refactor `build-program.post.ts`
- Replace `generateJson` / `extractJson` locals with `generateStructured` using `ProgramSkeletonSchema` and `WorkoutDetailSchema`.
- Add one preflight check at the top: resolve model set, look up descriptor, rough-estimate prompt tokens (chars/4), throw `400` if skeleton prompt exceeds model context window. This is the minimum useful preflight — not a full tokenizer, just a sanity check that saves a wasted API call when a user's BYOK points at a tiny-context model.
- Keep the per-workout `.catch()` fallback that synthesizes a placeholder block — that's deliberate graceful degradation.

### [x] Step 6 — Verification
- `nuxi dev` starts without type errors.
- `POST /api/ai/parse` with "bench 80kg x 8, 8, 6 @ RPE 8" still returns structured sets.
- `POST /api/ai/chat` still streams.
- `POST /api/ai/build-program` still returns a full program with phases + workouts.
- (Can't run these automatically without the full stack — will call out what I verified vs. what needs your hands-on confirmation.)

---

## What I am NOT doing (and why)

- **Mock Anthropic service / test harness** — this is the most valuable claw-code borrow long-term, but it's a separate investment (adds vitest, test fixtures, CI wiring). Worth doing after this refactor proves stable.
- **Full token counting** — we don't need `tiktoken` for a preflight sanity check. chars/4 is good enough to catch order-of-magnitude errors; adding a tokenizer dependency for more precision isn't worth it yet.
- **Provider expansion** (Grok, Kimi, Qwen-cloud) — claw-code supports these; we don't need them. The registry makes adding them later a one-liner if we do.
- **Removing `any` broadly** — only removing it in the files I'm already touching, per your "minimal impact" rule.

---

## Review

### Summary of actual changes
- **New:** `server/utils/ai-registry.ts` (42 lines) — `ModelDescriptor` type, `REGISTRY` map for 6 known models, `FALLBACK` for unknown IDs, `buildModel()` factory.
- **New:** `server/utils/ai-json.ts` (48 lines) — `extractJson()`, `generateStructured()` with optional `fixModel` retry, typed `StructuredGenerationError`.
- **Refactored:** `server/utils/ai-model.ts` — single `resolveUserConfig()` discriminated-union replaces duplicated branching across `resolveModel` / `resolveModelSet`. `catch {}` replaced with `console.warn`. `resolveModelSet()` now also returns `descriptors` alongside the models. Unused `getConversationModel/getStructuredModel/getSmallModel` exports deleted.
- **Refactored:** `server/api/ai/parse.post.ts` — fixed missing `z` + `ParsedSetSchema` imports, replaced inline JSON extraction with `generateStructured(..., { schema: ParseResponseSchema })`. Regex fallback preserved.
- **Refactored:** `server/api/ai/build-program.post.ts` — local `generateJson`/`extractJson` deleted. Both architect and per-workout calls use `generateStructured` with lenient raw schemas (defined inline since `ai-schemas.ts` was out of scope). Added pre-flight token-budget check at 80% of context window before firing any API calls. Graceful per-workout fallback preserved.

### Deviations from plan
- **Inherited type typo fix:** original `ai-model.ts` imported `LanguageModelV1` from `ai` — that name doesn't exist in `ai@6` (it's `LanguageModel`). Fixed in all three files I own (registry, json, model). This was a latent error in the baseline typecheck too.
- **Did not touch `ai-schemas.ts`:** raw schemas for AI output (before post-hoc `sortOrder` injection) went inline in `build-program.post.ts` to stay in scope. Fine for now; worth promoting if another endpoint needs them.

### Verification
- `tsc --noEmit` against `.nuxt/tsconfig.server.json`: all 5 touched/new files typecheck clean. Remaining errors in the repo (`chat.post.ts`, `ai-tools.ts`, `register.post.ts`, etc.) are pre-existing and unrelated — confirmed by stashing and re-running.
- **Not verified at runtime** — would require starting `nuxi dev` + hitting the endpoints with a real user + DB. You'll need to smoke-test: chat streams, parse returns structured sets, build-program returns a full phase/workout tree.

### Follow-ups for `tasks/lessons.md`
- When a Vercel AI SDK major version bumps, type names change (`LanguageModelV1` → `LanguageModel` between v4 and v6). Worth a note.
- `noUncheckedIndexedAccess` is on but the codebase has ~40 violations. Either turn it off or clean them up — current state is neither here nor there.
