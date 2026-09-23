## Why

The Definition of Done requires lint to pass with zero warnings (Android Lint, ktlint, detekt). Static analysis has to exist and be strict before product code arrives: adding strictness later means cleaning up an existing backlog, while adding it now costs nothing.

**Depends on:** `add-android-project-skeleton`.
**Touches:** Gradle build (root and module build scripts, version catalog), new config files (`.editorconfig`, `config/detekt/detekt.yml`, Android Lint config), existing skeleton sources if a finding forces a fix. Not parallelizable with `add-test-infrastructure` (both touch the Gradle build).

## What Changes

- Android Lint configured so that any warning fails the build, with no baseline file.
- ktlint enforced through a Gradle plugin, with rules configured in `.editorconfig`, including the Compose naming exception.
- detekt enforced through its Gradle plugin, with a committed config that builds on the defaults, plus the Compose rule set.
- One documented command runs all three analyzers, and the Gradle `check` task includes them.
- `AGENTS.md` lists the lint command and the "no suppression without justification" rule.

## Capabilities

### New Capabilities
- `static-analysis`: which analyzers guard the codebase, what counts as a failure, and how findings can (and cannot) be silenced.

### Modified Capabilities
- None.

## Non-goals

- Running lint in CI (`add-ci-workflows` calls the same command).
- Formatting on save or IDE integration.
- Custom lint rules written for this project.

## New dependencies (owner approval)

- ktlint Gradle plugin (`org.jlleitschuh.gradle.ktlint`) and the ktlint engine it runs.
- detekt Gradle plugin (`io.gitlab.arturbosch.detekt`, or its successor coordinates if the stable line has moved).
- Compose rules for detekt (`io.nlopez.compose.rules:detekt`).
- Android Lint is already part of the Android Gradle Plugin (no new dependency).

All at their latest stable versions compatible with the project's Kotlin version, pinned in the version catalog.

## Impact

- Every later change must pass `./gradlew lintAll` locally and in the gate.
- Build time grows slightly on `check`.
