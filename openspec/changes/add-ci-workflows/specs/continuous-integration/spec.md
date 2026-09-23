## Purpose

Defines when continuous integration runs on GitHub, what it verifies on every pull request and push to main, and which evidence (reports, screenshot diffs, E2E video) it publishes for the owner's review.

## ADDED Requirements

### Requirement: CI runs on every pull request and on main
CI SHALL run automatically for every pull request targeting `main` (on open and on every new push) and for every push to `main`.

#### Scenario: New commit on a pull request
- **GIVEN** an open pull request targeting `main`
- **WHEN** a new commit is pushed to its branch
- **THEN** a CI run starts for that commit and reports its checks on the pull request

#### Scenario: Superseded run
- **GIVEN** a CI run in progress for a pull request
- **WHEN** a newer commit is pushed to the same pull request
- **THEN** the older run is cancelled and only the newest commit's run continues

### Requirement: Verify check
CI SHALL expose a check named `verify` that builds the app and runs the full lint command and the JVM test command (unit, UI and screenshot verification), with the same commands documented for local use. It SHALL fail if any of them fails.

#### Scenario: Lint finding in a pull request
- **GIVEN** a pull request that introduces a lint finding
- **WHEN** CI runs
- **THEN** the `verify` check fails, and its log names the finding

#### Scenario: Screenshot difference in a pull request
- **GIVEN** a pull request that changes a covered screen without re-recording its reference
- **WHEN** CI runs
- **THEN** the `verify` check fails, and the comparison images are available as an artifact of the run

### Requirement: E2E check with video
CI SHALL expose a check named `e2e` that runs the E2E entry point on a hardware-accelerated Android emulator. It SHALL publish the E2E video and test reports as run artifacts, whether the tests pass or fail.

#### Scenario: Passing E2E run
- **GIVEN** a pull request whose E2E tests pass
- **WHEN** CI finishes
- **THEN** the `e2e` check is green, and the run has an artifact containing a playable video of the E2E run

#### Scenario: Failing E2E run
- **GIVEN** a pull request that breaks an E2E test
- **WHEN** CI finishes
- **THEN** the `e2e` check fails, and the video and report are still available as artifacts

### Requirement: Least privilege and pinned actions
Workflows SHALL request read-only repository permissions unless a job needs more, with the reason stated in a comment. Every third-party action SHALL be referenced by a full commit SHA. Workflows SHALL use no repository secrets.

#### Scenario: Workflow audit
- **GIVEN** the committed workflow files
- **WHEN** they are inspected
- **THEN** every `uses:` reference is pinned to a 40-character commit SHA, the default token permission is `contents: read`, and no `secrets.` reference appears

### Requirement: Gradle wrapper integrity
CI SHALL verify that the committed Gradle wrapper JAR matches an official Gradle release before running any build.

#### Scenario: Tampered wrapper
- **GIVEN** a pull request that replaces the wrapper JAR with an unofficial binary
- **WHEN** CI runs
- **THEN** the run fails before executing the build
