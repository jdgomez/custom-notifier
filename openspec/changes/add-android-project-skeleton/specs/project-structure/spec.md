## Purpose

Defines how the Android app is built and split into modules, and the structural guarantees that keep the domain logic free of Android dependencies and the build reproducible.

## ADDED Requirements

### Requirement: Reproducible command-line build
The repository SHALL build from a clean checkout using only the committed Gradle wrapper and the documented local toolchain, without an IDE.

#### Scenario: Clean debug build
- **GIVEN** a clean checkout on a machine set up per `docs/development-setup.md`
- **WHEN** the user runs `./gradlew assembleDebug`
- **THEN** the build succeeds and produces a debug APK

### Requirement: Centralized dependency versions
Every plugin and library version SHALL be declared once, in the version catalog. Build scripts SHALL reference catalog entries instead of literal versions.

#### Scenario: Version bump in one place
- **GIVEN** a library used by the build
- **WHEN** a developer changes its version in the version catalog
- **THEN** every module that uses it picks up the new version, and no build script has to change

### Requirement: Domain module is pure Kotlin
The domain module SHALL be a plain Kotlin/JVM module with no dependency on the Android SDK or on any Android library, so its code compiles and runs its tests on the JVM without an emulator.

#### Scenario: Android API used in the domain
- **GIVEN** the domain module
- **WHEN** a developer references an Android API (for example `android.content.Context`) from domain code
- **THEN** compilation of the domain module fails

#### Scenario: Dependency direction
- **GIVEN** the app module and the domain module
- **WHEN** the module dependency graph is inspected
- **THEN** the app module depends on the domain module, and the domain module depends on no other project module

### Requirement: Minimum Android version
The app SHALL install and launch on Android 8.0 (API 26) and later.

#### Scenario: Launch on the minimum version
- **GIVEN** a device or emulator running API 26
- **WHEN** the debug APK is installed and launched
- **THEN** the app opens its main screen without crashing

### Requirement: Placeholder main screen
Until product features exist, the app SHALL open a single Compose screen showing the app name.

#### Scenario: Launch shows the app name
- **GIVEN** the debug app is installed
- **WHEN** the user launches it
- **THEN** a screen showing the app name is displayed

### Requirement: Compiler warnings fail the build
The build SHALL treat Kotlin compiler warnings as errors in every module.

#### Scenario: Warning introduced
- **GIVEN** code that produces a Kotlin compiler warning
- **WHEN** the module is compiled
- **THEN** the build fails and reports the warning
