## Context

The skeleton has `:domain` (Kotlin/JVM, empty) and `:app` (Compose placeholder screen). `add-lint-setup` may land before or after this change. Both edit the Gradle build, so they run one after the other, and whichever lands second rebases. The local emulator comes from `setup-local-toolchain` (AVD `cn-api<N>`, `google_apis` x86_64).

## Goals / Non-Goals

**Goals:**
- Exactly the same commands locally, in the gate and in CI.
- Evidence (reports, screenshot diffs, video) that CI can upload as artifacts without extra logic.

**Non-Goals:**
- A test for the empty `:domain` module. A test with nothing real to assert would break the "tests exercise real behavior" rule. The domain test setup is proved in the PR with a throwaway test that is not committed. The first committed domain tests come with Phase 1's domain change.

## Decisions

### JUnit 4 everywhere, with `kotlin-test` assertions
Robolectric and AndroidX instrumented tests run on JUnit 4, so using it in `:domain` as well gives agents one test API.
- Alternative: JUnit 5 in `:domain`. It is more modern, but mixing frameworks splits conventions and runners. Rejected for now. It can be revisited through an ADR if property-based testing (for example Kotest) is wanted for the depletion math in Phase 1.

### Robolectric with native graphics for UI and screenshot tests
`@GraphicsMode(NATIVE)` and `testOptions.unitTests.isIncludeAndroidResources = true`. Compose UI tests use `createComposeRule()`.

### Roborazzi: verify by default, record explicitly
Set `roborazzi.test.verify=true` in `gradle.properties`, so plain `./gradlew test` verifies screenshots. Recording is `./gradlew recordRoborazziDebug`. References live under `app/src/test/screenshots/` (committed). Comparison output goes to `build/outputs/roborazzi` (already git-ignored). The change threshold is 0, for pixel-exact comparison.
- Alternative: Paparazzi. Rejected: it uses a different rendering stack from the Robolectric UI tests, and Roborazzi reuses the same Robolectric setup.

### E2E: `connectedDebugAndroidTest` on a booted emulator, wrapped by `scripts/e2e.sh`
The script:
1. fails fast if `adb devices` shows no device
2. starts emulator-level recording with `adb emu screenrecord start --time-limit <s> build/e2e/e2e-run.webm`
3. runs `./gradlew connectedDebugAndroidTest`
4. always stops the recording (trap), copies test reports to `build/e2e/`
5. exits with the Gradle exit code

Emulator-level recording has no 3-minute limit (unlike `adb shell screenrecord`) and records the whole run, including system UI such as the notification shade.
- Alternative: Gradle Managed Devices. They boot and tear down emulators themselves, but the device is hidden from `adb emu`, which makes recording awkward. They could still be adopted later for local convenience, through an ADR.
- Alternative: one video per test with `screenrecord` inside a JUnit rule. Rejected for now: it adds more moving parts and hits the 3-minute limit. Revisit if the single video becomes hard to review.

### Test isolation with the AndroidX Test Orchestrator
Set `testInstrumentationRunnerArguments["clearPackageData"] = "true"` and `execution = "ANDROIDX_TEST_ORCHESTRATOR"`.

### UI Automator is wired in and exercised once
The launch E2E test starts the app from the launcher intent through UI Automator, then asserts the main screen with the Compose test rule. This proves both libraries work together before notifications and calendar need them.

### Output locations
Output goes to `app/build/reports/tests/`, `app/build/outputs/roborazzi/` and `build/e2e/`, and these paths are documented in `AGENTS.md`. CI uploads the same paths.

## Risks / Trade-offs

- [Screenshot references differ between the local machine and CI] → Both are Linux x86_64 using Robolectric's bundled native graphics. If a difference appears anyway, CI is the source of truth: re-record inside the same container image CI uses and document how.
- [`adb emu screenrecord` is not available in some emulator versions] → Verify during implementation. Fallback: chained `adb shell screenrecord` segments inside the script.
- [The emulator slows the machine while agents run] → E2E runs only when asked (not part of `check`). In the gate it runs once per change.
