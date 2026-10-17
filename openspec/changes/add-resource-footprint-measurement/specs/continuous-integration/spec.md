## MODIFIED Requirements

### Requirement: Verify check
CI SHALL expose a check named `verify` that builds the app in its debug and its shrunk release variant and runs the full lint command and the JVM test command (unit, UI and screenshot verification), with the same commands documented for local use. It SHALL fail if any of them fails.

#### Scenario: Lint finding in a pull request
- **GIVEN** a pull request that introduces a lint finding
- **WHEN** CI runs
- **THEN** the `verify` check fails, and its log names the finding

#### Scenario: Screenshot difference in a pull request
- **GIVEN** a pull request that changes a covered screen without re-recording its reference
- **WHEN** CI runs
- **THEN** the `verify` check fails, and the comparison images are available as an artifact of the run

#### Scenario: Release build cannot be shrunk
- **GIVEN** a pull request that makes the release build fail, for example a shrinking error about a missing class
- **WHEN** CI runs
- **THEN** the `verify` check fails, and its log names the error
