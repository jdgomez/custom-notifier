## Why

Robolectric runs Android SDK 36 and later only on JDK 21, and the project's toolchain is JDK 17. `add-test-infrastructure` therefore pinned Robolectric to SDK 35, so the Robolectric UI tests and the Roborazzi screenshots run on an older Android than the app targets (API 37). Moving to JDK 21 now, before `add-ci-workflows`, means CI starts on the final toolchain instead of being migrated later.

**Depends on:** `add-test-infrastructure`. JDK 21 (apt `openjdk-21-jdk-headless`) is already installed on the development machine next to JDK 17, by the owner.
**Touches:** Gradle module build scripts (`jvmToolchain`), `app/src/test/resources/robolectric.properties`, possibly the committed screenshot references, `AGENTS.md`, `docs/development-setup.md`, `docs/WAYOFWORKING.md` (machine description line), the planning artifacts of `add-ci-workflows`, and the development machine (JDK 17 removal by the owner). Not parallelizable with any change that touches the Gradle build or `add-ci-workflows`.

## What Changes

- The Kotlin/Java toolchain of `:app` and `:domain` moves from 17 to 21.
- Robolectric runs the app's target SDK instead of the SDK 35 pin; the pin and its `AGENTS.md` note are removed.
- Screenshot references are re-recorded only if rendering on the new SDK differs; the owner reviews any image change in the PR diff.
- `docs/development-setup.md` describes JDK 21 everywhere (versions table, install, verify, remove), and `WAYOFWORKING.md` and `AGENTS.md` stop naming JDK 17.
- The not-yet-implemented `add-ci-workflows` change is updated to use Java 21 (Temurin 21) in CI.
- After the PR is merged and verified, the owner removes JDK 17 from the machine, leaving JDK 21 as the only JDK. The procedure is documented.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `development-environment`: the Java toolchain requirement changes from JDK 17 to JDK 21, as the only JDK on the machine.
- `test-infrastructure`: Robolectric UI and screenshot tests run on the app's target SDK, not on a lower pinned SDK.

## Non-goals

- Raising `minSdk`, `targetSdk` or `compileSdk`.
- Upgrading AGP, Kotlin, Gradle or any library beyond what JDK 21 strictly requires.
- Adopting Java 21 language features or APIs in app code.
- Implementing CI (`add-ci-workflows` only gets its planning artifacts updated here).

## New dependencies (owner approval)

- None in the build. JDK 21 itself is a machine dependency, already approved and installed by the owner.

## Impact

- Every local build, lint and test run uses JDK 21.
- Screenshot references may change once; later changes compare against the new references.
- `add-ci-workflows` (#7) inherits Java 21.
