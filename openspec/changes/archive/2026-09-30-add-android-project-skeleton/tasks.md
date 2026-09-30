## 0. Decision

- [x] 0.1 (owner) Decide the `applicationId` / package root: `dev.jdgomez.customnotifier` (see design.md)

## 1. Gradle foundation

- [x] 1.1 Add the Gradle wrapper (latest stable) with `distributionSha256Sum` set
- [x] 1.2 Create `settings.gradle.kts`, root `build.gradle.kts`, `gradle.properties` (configuration cache, build cache, parallel, bounded daemon heap)
- [x] 1.3 Create `gradle/libs.versions.toml` with every plugin and library version used

## 2. Domain module

- [x] 2.1 Create `:domain` as a Kotlin/JVM module with JVM toolchain 17 and warnings as errors
- [x] 2.2 Verify that referencing an Android type from `:domain` fails compilation (manual check, not committed)

## 3. App module

- [x] 3.1 Create `:app` with the Android application plugin, Kotlin, the Compose compiler plugin, `minSdk 26`, `compileSdk`/`targetSdk` at the installed platform, warnings as errors
- [x] 3.2 Add the dependency on `:domain`
- [x] 3.3 Add the placeholder Compose activity showing the app name from `strings.xml`

## 4. Verification

- [x] 4.1 `./gradlew assembleDebug` succeeds from a clean checkout
- [x] 4.2 Install on the local emulator and confirm the placeholder screen shows; take a screenshot for the PR
- [x] 4.3 Install and launch on the `cn-api26` AVD from `setup-local-toolchain`; confirm the placeholder screen shows without crashing and take a screenshot for the PR (mandatory: the change is not done without it)

## 5. Docs and gate

- [x] 5.1 Add build and install commands to `AGENTS.md`; update the "Project state" section
- [x] 5.2 Commit on `change/add-android-project-skeleton` with Conventional Commits
- [x] 5.3 Run the no-mistakes gate with `--skip ci` (no CI on `main` yet)
