# Repository Guidelines

## Project Structure & Module Organization
- `main.tsx`: Deno entry exporting `app.fetch` (Hono server).
- `routes/`: API handlers (`rtc.ts`, `observer.ts`, `utils.ts`).
- `frontend/index.html`: Client UI served at `/`.
- `kmp/`: Kotlin Multiplatform (Android/iOS/Desktop) app; see `kmp/README.md`.
- Config: `deno.json` (compiler/lint), `.gitignore`, workspace meta.

## Build, Test, and Development Commands
- Run server (requires `OPENAI_API_KEY`):
  - `OPENAI_API_KEY=... deno serve --allow-net --allow-env --env main.tsx`
  - Visit `http://localhost:8000`.
- Format & lint (TypeScript): `deno fmt` • `deno lint`.
- Deno tests (if added): `deno test --allow-env`.
- Kotlin app (Desktop): `cd kmp && ./gradlew :composeApp:run`.
- Kotlin app (Android debug): `cd kmp && ./gradlew :composeApp:assembleDebug`.
- Kotlin tests: `cd kmp && ./gradlew test` (or module-specific `:composeApp:test`).

## Coding Style & Naming Conventions
- TypeScript (Deno): 2-space indent, semicolons, ES modules, named exports.
  - Files in `routes/`: lowercase with dashes/short names (e.g., `rtc.ts`).
  - Keep side effects in route handlers; isolate helpers in `utils.ts`.
- Kotlin: follow Kotlin style (4-space indent).
  - Classes `PascalCase`, functions/props `camelCase`, constants `UPPER_SNAKE`.

## Testing Guidelines
- TypeScript: place tests next to sources as `*_test.ts`; mock network calls when possible. Use `OPENAI_API_KEY` only for manual/integration tests.
- Kotlin: add common tests under `kmp/composeApp/src/commonTest`. Platform tests may live under `androidTest`/`iosTest` as needed.
- Aim for small, deterministic tests; prefer module-level coverage over end-to-end.

## Commit & Pull Request Guidelines
- Commits: concise, imperative subject; optional scope (e.g., `routes: handle call errors`).
- PRs: include summary, linked issues, test plan/steps, and screenshots for UI changes. Note any env/config needs (`OPENAI_API_KEY`). Keep diffs focused.

## Security & Configuration Tips
- Never commit secrets. Use `.env` (loaded via `--env`) or shell exports for `OPENAI_API_KEY`.
- Avoid logging sensitive data. The observer route logs messages—sanitize before adding new logs.
- Network calls go to OpenAI Realtime APIs; handle failures and timeouts defensively.
