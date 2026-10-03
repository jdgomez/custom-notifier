## Why

The Definition of Done requires tests for every change, and the testing strategy in `WAYOFWORKING.md` defines four levels that run on every pull request: JVM unit tests, Compose UI tests on Robolectric, pixel-exact screenshot tests with Roborazzi, and E2E tests on a real emulator that record video. None of them exists yet, so Phase 1 changes would have nowhere to put their tests.

**Depends on:** `add-android-project-skeleton`.
**Touches:** Gradle build (module build scripts, version catalog, `gradle.properties`), new test source sets in `:app` and `:domain`, committed screenshot references, a new `scripts/` entry point for E2E. Not parallelizable with `add-lint-setup` (both touch the Gradle build).

## What Changes

- JVM unit test setup for `:domain` and `:app`.
- Robolectric-based Compose UI tests for `:app`, with one real test for the placeholder screen.
- Roborazzi screenshot tests: reference images committed; any pixel difference fails `./gradlew test`; re-recording the references is an explicit command.
- Instrumented E2E tests on an emulator (AndroidX Test with the Orchestrator, Compose UI test, UI Automator for system UI), with one real test that launches the app.
- One E2E entry point script that runs the instrumented suite on a running emulator while recording the screen to a video file. CI uses the same script.
- Test commands are documented in `AGENTS.md`.

## Capabilities

### New Capabilities
- `test-infrastructure`: the test levels available, how each is run, what makes each fail, and where results, screenshots and videos end up.

### Modified Capabilities
- None.

## Non-goals

- Tests for product behavior (Phase 1 changes add them with each feature).
- Running tests in CI (`add-ci-workflows`).
- Code coverage thresholds. The Definition of Done asks for tests that exercise real behavior, not a coverage number.
- Notification and calendar E2E tests, which arrive with those features. This change only proves that UI Automator is wired in and can reach system UI.

## New dependencies (owner approval)

- JUnit 4 and `kotlin-test`.
- Robolectric.
- AndroidX Test: core, runner, rules, JUnit extensions, Orchestrator, test services.
- Compose UI test (`ui-test-junit4`, `ui-test-manifest` in debug).
- Roborazzi (Gradle plugin, core, Compose and JUnit rule artifacts).
- UI Automator.

All at their latest stable versions, pinned in the version catalog.

## Impact

- Committed binary files: Roborazzi reference PNGs (small, reviewed like code).
- Local E2E runs need a booted emulator (from `setup-local-toolchain`). They use CPU and RAM while running.
