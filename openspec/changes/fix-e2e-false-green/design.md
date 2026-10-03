## Context

`scripts/e2e.sh` starts the emulator screen recording, runs `./gradlew connectedDebugAndroidTest`, and exits with Gradle's status. Instrumented tests run through the AndroidX Test Orchestrator (`execution = "ANDROIDX_TEST_ORCHESTRATOR"`, `clearPackageData = true`), which installs the orchestrator and test-services APKs (`androidTestUtil`) next to the app and test APKs. The script trusts Gradle's exit code, so any path where Gradle succeeds without executing tests is reported as green.

The observed failure (`INSTALL_FAILED_ALREADY_EXISTS` on the first run after a fresh boot of `cn-api26`, re-run green) points at the install phase, most likely one of the `androidTestUtil` APKs, but this is unconfirmed and must be established by reproduction.

## Goals / Non-Goals

**Goals:**
- The first E2E run after a fresh boot executes the tests on both AVDs.
- `scripts/e2e.sh` cannot exit 0 unless at least one test was executed and none failed, whatever the cause of a no-test run.

**Non-Goals:**
- New tests, CI wiring, emulator changes, new dependencies (see proposal).

## Decisions

### 1. Reproduce before fixing
Boot `cn-api26` from a cold start (no snapshot, per `docs/development-setup.md`), run `scripts/e2e.sh` exactly as the user does, and confirm exit 0 with no test-result XML. Repeat on `cn-api37`. Capture the install output and `adb logcat` around the install to find which APK fails and why. If it does not reproduce after a few cold boots, record the attempts and still deliver decision 3, which does not depend on the root cause.

### 2. Fix the root cause where it lives
The fix goes where the cause is (Gradle test configuration, install step, or script), not a retry around it. A blind "run twice" or "retry on failure" is rejected: it hides the cause and doubles run time. If the cause is a known bug in the Android Gradle plugin or emulator image with no clean fix inside the current dependencies, the executor stops and escalates with the evidence instead of improvising.

### 3. Independent guard: count executed tests from the result XML
After Gradle finishes, the script reads the JUnit XML results under `app/build/outputs/androidTest-results/connected/` and sums the `tests` attribute of each `<testsuite>`. If there are no result files or the sum is 0, it prints `No instrumented tests were executed.` to stderr and exits non-zero (even if Gradle succeeded). Gradle's non-zero status still wins when it fails. The existing `finish` trap keeps writing video and reports on this path too.

Alternatives considered: grepping Gradle's console output (fragile, depends on log format) and parsing the HTML report (meant for humans). The XML is the stable machine-readable artifact the reports are built from. The parsing stays in plain shell tools already on the machine; no new tool.

### 4. Proving the guard
The bug-fix rule requires a check that fails before the fix. The guard is shown to work by forcing a no-test run (for example, a run filtered to a test class that does not exist, if that yields zero executed tests with Gradle green) and showing exit 0 before the guard and non-zero after. The evidence (commands and exit codes) goes into the PR. The script has no automated test harness today; adding one is out of scope.

## Risks / Trade-offs

- [The install failure may be intermittent and hard to reproduce] -> decision 3 makes it harmless (a red run, never a false green) even if the root cause is not found; the outcome of decision 1 is reported either way.
- [Result XML location could change with an Android Gradle plugin upgrade] -> a missing directory makes the script fail loudly, not pass silently, so an upgrade surfaces it immediately.
