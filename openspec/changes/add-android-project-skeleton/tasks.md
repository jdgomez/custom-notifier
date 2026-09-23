## 0. Decision

- [x] 0.1 (owner) Decide the `applicationId` / package root: `dev.jdgomez.customnotifier` (see design.md)

## 1. Gradle foundation

- [ ] 1.1 Add the Gradle wrapper (latest stable) with `distributionSha256Sum` set
- [ ] 1.2 Create `settings.gradle.kts`, root `build.gradle.kts`, `gradle.properties` (configuration cache, build cache, parallel, bounded daemon heap)
- [ ] 1.3 Create `gradle/libs.versions.toml` with every plugin and library version used

## 2. Domain module

- [ ] 2.1 Create `:domain` as a Kotlin/JVM module with JVM toolchain 17 and warnings as errors
- [ ] 2.2 Verify that referencing an Android type from `:domain` fails compilation (manual check, not committed)

## 3. App module

- [ ] 3.1 Create `:app` with the Android application plugin, Kotlin, the Compose compiler plugin, `minSdk 26`, `compileSdk`/`targetSdk` at the installed platform, warnings as errors
- [ ] 3.2 Add the dependency on `:domain`
- [ ] 3.3 Add the placeholder Compose activity showing the app name from `strings.xml`

## 4. Verification

- [ ] 4.1 `./gradlew assembleDebug` succeeds from a clean checkout
- [ ] 4.2 Install on the local emulator and confirm the placeholder screen shows; take a screenshot for the PR
- [ ] 4.3 Install on an API 26 emulator image (temporary; installing it is an owner-approved download) or record why this is deferred to the E2E setup in `add-test-infrastructure`

## 5. Docs and gate

- [ ] 5.1 Add build and install commands to `AGENTS.md`; update the "Project state" section
- [ ] 5.2 Commit on `change/add-android-project-skeleton` with Conventional Commits
- [ ] 5.3 Run the no-mistakes gate with `--skip ci` (no CI on `main` yet)
