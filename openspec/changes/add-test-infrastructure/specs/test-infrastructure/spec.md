## Purpose

Defines the four test levels (JVM unit, Robolectric UI, Roborazzi screenshot, emulator E2E with video), how each is run, what makes each fail, and where its reports, screenshots and videos are written.

## ADDED Requirements

### Requirement: JVM tests without an emulator
Unit tests for the domain and app modules and Compose UI tests SHALL run on the JVM with one documented command, without a device or emulator.

#### Scenario: Run JVM-level tests
- **GIVEN** a machine without a running emulator
- **WHEN** the user runs the documented JVM test command
- **THEN** unit tests, Robolectric UI tests and screenshot verification run, and the command fails if any of them fails

### Requirement: Placeholder screen covered by a UI test
The app's placeholder screen SHALL have a Robolectric Compose UI test that checks the app name is displayed, by querying the rendered UI rather than source text.

#### Scenario: UI test catches a missing app name
- **GIVEN** the placeholder screen no longer displays the app name
- **WHEN** the JVM test command runs
- **THEN** the UI test fails

### Requirement: Pixel-exact screenshot verification
Screenshot tests SHALL compare rendered screens against committed reference images, and any pixel difference SHALL fail the JVM test command. Re-recording references SHALL require a separate, explicit command.

#### Scenario: Unintended visual change
- **GIVEN** committed reference images
- **WHEN** a change alters any pixel of a covered screen and the JVM test command runs
- **THEN** the command fails and writes a comparison image showing the difference

#### Scenario: Intended visual change
- **GIVEN** a change that deliberately alters a covered screen
- **WHEN** the developer runs the documented record command
- **THEN** the reference images are updated, and the updated images appear in the change's diff for review

### Requirement: E2E tests on an emulator with video
Instrumented E2E tests SHALL run through one documented entry point against a running emulator, SHALL record a video of the whole run, and SHALL write the video and test reports to a known output directory, whether the tests pass or fail.

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

### Requirement: Tests are isolated
Each instrumented test SHALL start from a clean app state, so the outcome of one test does not depend on the tests that ran before it.

#### Scenario: Order independence
- **GIVEN** two instrumented tests where the first changes app state
- **WHEN** they run in either order
- **THEN** both give the same result
