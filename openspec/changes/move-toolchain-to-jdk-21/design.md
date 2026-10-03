## Context

`:app` and `:domain` set `jvmToolchain(17)`. Gradle itself already runs on JDK 21, the machine default since the owner installed it next to JDK 17; `./gradlew check assembleDebug` on `main` passes in that state. `app/src/test/resources/robolectric.properties` pins Robolectric to SDK 35, with a comment and an `AGENTS.md` line explaining why. The app targets API 37. See proposal.md - Why.

## Goals / Non-Goals

**Goals:**
- One JDK (21) for Gradle, compilation, Robolectric and the SDK tools.
- Robolectric follows the app's target SDK with no test-only override.

**Non-Goals:**
- Changing the bytecode level the app exposes to Android beyond what the toolchain change implies, or using Java 21 APIs in app code.

## Decisions

### Toolchain 21 in both modules
Set `jvmToolchain(21)` in `:app` and `:domain`. Kotlin's JVM target and Java's source/target follow the toolchain, so all three stay consistent without separate settings. AGP's D8 desugars the bytecode for `minSdk 26`.
- Alternative: keep `jvmToolchain(17)` and run only Gradle on JDK 21. Rejected: Robolectric runs inside the test JVM, which follows the toolchain, so SDK 36+ would still be unavailable.

### Robolectric follows `targetSdk` by default
Remove the `sdk=` pin. Without it, Robolectric runs each test on the app's `targetSdk`, so a later target bump needs no test change. Delete `robolectric.properties` if nothing else remains in it.
- Alternative: pin `sdk=37` explicitly. Rejected: it duplicates `targetSdk` and drifts silently when the target changes.
- If the current Robolectric release does not support SDK 37, stop and escalate (a newer, possibly non-stable Robolectric is an owner decision). Do not re-pin a lower SDK.

### Screenshot references
Run the screenshot verification first. Re-record only if it fails because rendering differs on the new SDK. Inspect the new image, describe the difference in the PR, and attach the old and new images so the owner can approve it.

### CI planning follows the toolchain
Update `add-ci-workflows` (`design.md` "Java distribution in CI" and task 1.3) to Temurin 21, so the change implemented later already matches.

### JDK 17 removal is the last, owner-only step
JDK 17 stays installed until this change is merged, because `main` needs it until then. After merge, the owner runs the documented `apt remove` for the JDK 17 packages only. `ca-certificates-java` and `java-common` are shared with JDK 21 and must stay. The guide's removal section changes to the JDK 21 package names.

## Risks / Trade-offs

- [A Gradle plugin (ktlint, detekt 2.x alpha, Roborazzi) misbehaves on a JDK 21 toolchain] → `./gradlew check` and the lint proofs run on the new toolchain. A failure that needs a plugin upgrade outside the catalog's current versions is escalated, not worked around.
- [SDK 37 rendering changes the screenshot reference] → Expected at most once. The owner approves the image diff in the PR.
- [Removing JDK 17 also removes packages JDK 21 needs] → The documented command names only the `openjdk-17-*` packages. The verification step is re-run after the removal.
