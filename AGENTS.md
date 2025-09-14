# Repository Guidelines

## Project Structure & Module Organization
- `main.tsx`: Deno entry exporting `app.fetch` (Hono server).
- `routes/`: API handlers and helpers (`rtc.ts`, `observer.ts`, `utils.ts`). Keep side effects in handlers; put shared helpers in `utils.ts`.
- `frontend/index.html`: Minimal client UI served at `/`.
- `kmp/`: Kotlin Multiplatform app (Android/iOS/Desktop). See `kmp/README.md`.
- Config: `deno.json` (fmt/lint), `.gitignore`, Gradle files under `kmp/`.

## Build, Test, and Development Commands
- Run server: `OPENAI_API_KEY=... deno serve --allow-net --allow-env --env main.tsx` → visit `http://localhost:8000`.
- Format/Lint (TypeScript): `deno fmt` • `deno lint`.
- Deno tests: `deno test --allow-env`.
- Desktop app: `cd kmp && ./gradlew :composeApp:run`.
- Android debug APK: `cd kmp && ./gradlew :composeApp:assembleDebug`.
- Kotlin tests: `cd kmp && ./gradlew test` (or module-specific, e.g., `:composeApp:test`).

## Coding Style & Naming Conventions
- TypeScript: 2-space indent, semicolons, ES modules, named exports.
- `routes/` filenames: lowercase, short, dashes (e.g., `rtc.ts`).
- Kotlin: 4-space indent; Classes PascalCase; functions/props camelCase; constants UPPER_SNAKE.

## Testing Guidelines
- TypeScript: colocate tests as `*_test.ts`; mock network calls; use `OPENAI_API_KEY` only for manual/integration tests.
- Kotlin: common tests in `kmp/composeApp/src/commonTest`; platform tests in `androidTest`/`iosTest`.
- Prefer small, deterministic tests and module-level coverage.

## Commit & Pull Request Guidelines
- Commits: concise, imperative subject; optional scope (e.g., `routes: handle call errors`).
- PRs: include summary, linked issues, test plan/steps, and screenshots for UI changes. Note any env/config needs (e.g., `OPENAI_API_KEY`). Keep diffs focused.

## Security & Configuration Tips
- Never commit secrets. Load via `.env` (with `--env`) or shell exports.
- Avoid logging sensitive data; sanitize observer/route logs.
- External calls target OpenAI Realtime APIs—add timeouts and handle failures defensively.

