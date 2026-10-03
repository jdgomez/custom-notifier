## MODIFIED Requirements

### Requirement: E2E tests on an emulator with video
Instrumented E2E tests SHALL run through one documented entry point against a running emulator, SHALL record a video of the whole run, and SHALL write the video and test reports to a known output directory, whether the tests pass or fail. The entry point SHALL report success only when at least one instrumented test was executed and none failed.

#### Scenario: E2E run produces a video
- **GIVEN** a booted emulator
- **WHEN** the user runs the E2E entry point
- **THEN** the app is launched by at least one test that verifies its main screen, and a playable video file plus the test report exist in the documented output directory

#### Scenario: Failing E2E still keeps evidence
- **GIVEN** a booted emulator and a failing E2E test
- **WHEN** the E2E entry point runs
- **THEN** it exits with a non-zero status, and the video and report are still written

#### Scenario: No emulator running
- **GIVEN** no emulator or device is connected
- **WHEN** the user runs the E2E entry point
- **THEN** it fails immediately with a message saying an emulator must be booted

#### Scenario: First run after a fresh boot executes the tests
- **GIVEN** an emulator that was just booted from a cold start
- **WHEN** the user runs the E2E entry point for the first time
- **THEN** the instrumented tests are executed and their results appear in the test report

#### Scenario: Run that executes no tests fails
- **GIVEN** a booted emulator and a run in which no instrumented test is executed, for any reason
- **WHEN** the E2E entry point finishes
- **THEN** it exits with a non-zero status and a message saying no tests were executed, and the video and whatever reports exist are still written
