## Why

There is no Gradle project yet, so there is nothing to lint, test, run in CI or put through the gate. This change creates the smallest buildable Android app with the module structure `WAYOFWORKING.md` requires: a pure Kotlin domain module separated from the Android UI. Every later change adds to this structure.

**Depends on:** `setup-local-toolchain`.
**Touches:** repository root (Gradle files, wrapper, `settings.gradle.kts`, `gradle/libs.versions.toml`), new `app/` and `domain/` modules, `.gitignore` if needed.

## What Changes

- Gradle build with Kotlin DSL, a version catalog (`gradle/libs.versions.toml`) as the single place for versions, and the Gradle wrapper committed.
- `:domain` module: pure Kotlin/JVM, no Android dependencies, empty except for a package placeholder.
- `:app` module: Android application in Kotlin with Jetpack Compose and Material 3, minimum SDK 26, depending on `:domain`, and showing a single placeholder screen with the app name.
- Kotlin compiler warnings treated as errors from day one.
- Commands to build and install the debug app are documented in `AGENTS.md`.

## Capabilities

### New Capabilities
- `project-structure`: how the app is built and split into modules, and the guarantees that structure gives (domain without Android dependencies, reproducible build from the wrapper, minimum Android version).

### Modified Capabilities
- None.

## Non-goals

- Any product behavior (Phase 1).
- Lint tooling (`add-lint-setup`) and test frameworks (`add-test-infrastructure`).
- Persistence, notification or calendar adapters and their modules. They are added when Phase 1 needs them.
- Release signing and build types other than `debug` and a default `release` (Phase 2).
- App icon and final UX (owner UX checkpoint in Phase 1).

## New dependencies (owner approval)

- Gradle (through the wrapper), Android Gradle Plugin, Kotlin Gradle plugin and Kotlin Compose compiler plugin.
- AndroidX: Core KTX, Activity Compose, Lifecycle runtime.
- Jetpack Compose BOM, Compose UI, Compose Material 3, Compose UI tooling (debug only).

All at their latest stable versions at implementation time, pinned in the version catalog.

## Impact

- New build: the first `./gradlew` run downloads Gradle and dependencies into `~/.gradle`.
- `applicationId`, `namespace` and Kotlin package root: `dev.jdgomez.customnotifier` (decided by the owner; permanent once published, see design.md).
