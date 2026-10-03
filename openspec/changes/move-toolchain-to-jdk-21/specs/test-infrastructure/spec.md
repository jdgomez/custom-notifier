## ADDED Requirements

### Requirement: JVM UI tests run on the app's target SDK
Robolectric UI tests and screenshot tests SHALL run against the same Android SDK level the app targets, with no lower SDK pinned for test execution.

#### Scenario: Test SDK matches the target SDK
- **GIVEN** the app's target SDK level
- **WHEN** the JVM test command runs the Robolectric UI and screenshot tests
- **THEN** the tests run on that SDK level, and the test reports show it

#### Scenario: Target SDK raised later
- **GIVEN** a later change raises the app's target SDK
- **WHEN** the JVM test command runs
- **THEN** the Robolectric tests follow the new target SDK without a separate test-only SDK setting to update
