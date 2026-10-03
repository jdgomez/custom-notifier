## Purpose

Defines the static analyzers that guard the codebase (Android Lint, ktlint, detekt), what counts as a failure, and the rules for silencing a finding, so every change meets the zero-warnings Definition of Done.

## ADDED Requirements

### Requirement: Single lint entry point
The build SHALL expose one documented command that runs Android Lint, ktlint and detekt on every module where they apply. The standard `check` task SHALL include all three.

#### Scenario: Clean codebase
- **GIVEN** the codebase has no findings
- **WHEN** the user runs the documented lint command
- **THEN** the command succeeds and reports zero warnings and zero errors from all three analyzers

### Requirement: Any finding fails the build
A warning or error from any of the three analyzers SHALL make the lint command fail. There SHALL be no baseline file that hides existing findings.

#### Scenario: Android Lint warning
- **GIVEN** app code that triggers an Android Lint warning (for example a hardcoded user-facing string)
- **WHEN** the lint command runs
- **THEN** it fails and names the file, the line and the rule

#### Scenario: Formatting violation
- **GIVEN** Kotlin code that breaks the configured ktlint style
- **WHEN** the lint command runs
- **THEN** it fails and names the file, the line and the rule

#### Scenario: Code smell
- **GIVEN** Kotlin code that breaks an active detekt rule (for example a function above the complexity threshold)
- **WHEN** the lint command runs
- **THEN** it fails and names the file, the line and the rule

### Requirement: Compose-aware rules
Kotlin style and smell rules SHALL accept Jetpack Compose conventions (PascalCase `@Composable` functions) and SHALL flag Compose-specific mistakes covered by the Compose rule set.

#### Scenario: Composable naming
- **GIVEN** a `@Composable` function named in PascalCase
- **WHEN** the lint command runs
- **THEN** no naming finding is reported for it

### Requirement: Automatic formatting available
The build SHALL expose a command that automatically fixes the formatting findings it can fix, so agents do not reformat by hand.

#### Scenario: Auto-format
- **GIVEN** code with auto-fixable formatting findings
- **WHEN** the user runs the documented format command, followed by the lint command
- **THEN** the formatting findings are gone

### Requirement: Suppressions are local and justified
A finding SHALL only be silenced at the narrowest scope (a single declaration or line), with a comment that explains why. Global rule deactivations SHALL live only in the committed analyzer configuration files, with a comment for each deactivated rule.

#### Scenario: Suppression review
- **GIVEN** a change that adds a suppression annotation or deactivates a rule
- **WHEN** the change is reviewed
- **THEN** the suppression has a justifying comment and the narrowest possible scope, or the review rejects it
