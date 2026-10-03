## Why

`scripts/e2e.sh` can report success when no instrumented test ran. During the gate of PR #24, the first run on a freshly booted `cn-api26` printed `INSTALL_FAILED_ALREADY_EXISTS` during install, Gradle still reported `BUILD SUCCESSFUL`, the script exited 0, and no test-result XML was written. A re-run on the same emulator passed with `LaunchTest` executed. A green E2E run that tested nothing undermines every gate and, soon, CI (#7): it must be impossible.

## What Changes

- Reproduce the false green the way the user runs it: boot `cn-api26` fresh, run `scripts/e2e.sh`, observe exit 0 with no test results. Check whether `cn-api37` shows the same behaviour.
- Find and fix the root cause of the install failure, so the first run after a fresh boot actually executes the instrumented tests.
- Make `scripts/e2e.sh` exit non-zero, with a clear message, when the run produced no test results or executed zero tests. Video and reports are still written on that path, as on every other failure.
- Update `AGENTS.md` where it describes when `scripts/e2e.sh` exits non-zero.

## Non-goals

- New E2E tests or changes to the existing `LaunchTest`.
- CI wiring of the E2E suite (that is `add-ci-workflows`, #7).
- Changing the emulator set (`cn-api37`, `cn-api26`) or the Orchestrator / clean-state setup, unless the root cause lives there.
- New dependencies. If the root-cause fix needs one, the executor stops and escalates.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `test-infrastructure`: the requirement "E2E tests on an emulator with video" gains a scenario: a run that executes no tests exits non-zero.

## Impact

- Depends on: nothing open (`add-test-infrastructure` and `move-toolchain-to-jdk-21` are merged).
- Touches: `scripts/e2e.sh`; possibly the instrumented-test configuration in `app/build.gradle.kts` (root cause); `AGENTS.md`; `openspec/specs/test-infrastructure` (via the delta spec). Machine: the `cn-api26` and `cn-api37` AVDs, booted one at a time.
- New dependencies: none.
- Parallelizable with `add-ci-workflows` (#7) except for any shared edits to `AGENTS.md`.
