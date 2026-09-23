## Context

The repository contains only docs, `AGENTS.md`, `.gitignore` (which already covers Gradle and Android build output) and `openspec/`. The toolchain comes from `setup-local-toolchain`: JDK 17 and an Android SDK with the latest stable platform. `WAYOFWORKING.md` requires ports and adapters by construction: a pure Kotlin domain, with persistence, notifications and calendar as adapters the domain does not know about.

## Goals / Non-Goals

**Goals:**
- The smallest structure that enforces domain purity through the build itself, not through convention.
- A layout later changes can extend without restructuring.

**Non-Goals:**
- Convention plugins (`build-logic/`). With two modules, they are premature. Add them when a third module duplicates configuration.

## Decisions

### Two modules: `:domain` (Kotlin/JVM) and `:app` (Android)
`:domain` applies only the Kotlin JVM plugin, so Android APIs are not on its classpath and compilation fails if they are used. This is stronger than a lint rule.
- Alternative: one module with packages plus an architecture test. Rejected: an Android module can always reach Android APIs, so the boundary would depend on a test rather than on the compiler.
- Alternative: separate `:data`, `:notifications` and `:calendar` modules now. Rejected: nothing would be in them yet. Phase 1 adds adapter modules if the `:app` module grows large enough to need them.

### Kotlin DSL + version catalog + committed wrapper
`settings.gradle.kts`, `build.gradle.kts` for each module, and `gradle/libs.versions.toml`. Plugins are declared through `plugins {}` with catalog aliases. The wrapper's `distributionSha256Sum` is set, so the downloaded Gradle distribution is verified.

### JVM target 17 via a Kotlin toolchain
Use `kotlin { jvmToolchain(17) }` in both modules, matching the installed JDK and CI.

### SDK levels
`minSdk = 26` (from `SESSION0.md`). `compileSdk` and `targetSdk` are set to the platform installed by `setup-local-toolchain` (the latest stable API level).
This change verifies the minimum version itself: the debug APK is installed and launched on both the `cn-api<N>` and the `cn-api26` AVDs from `setup-local-toolchain`. The API 26 run is not deferred to a later change, because CI runs a single API level (see `add-ci-workflows`).

### Warnings as errors
Set `allWarningsAsErrors = true` in the Kotlin compiler options of both modules. Android Lint's own warnings are handled in `add-lint-setup`.

### Gradle performance settings
Enable the configuration cache, the build cache and parallel execution in `gradle.properties`. Set a bounded daemon heap (`-Xmx2g`), because this machine also runs several agent processes.

### `applicationId` and package root: `dev.jdgomez.customnotifier`
Decided by the owner on 2026-09-23. The `applicationId` is permanent once the app is on Google Play, so it uses a domain the owner controls (`jdgomez.dev`), reversed. This is the standard practice. The same value is the `namespace` and the Kotlin package root.
- Alternatives considered: `app.customnotifier` (claims no domain, so another developer could publish it first), `io.github.<account>.customnotifier` (valid, but tied to a GitHub account name).
- Where the domain is hosted does not matter: Android and Google Play never resolve the `applicationId` through DNS or HTTP.
- Approved exception to the "no personal references" rule: the domain is the owner's public brand. This exception covers only the `applicationId`, the `namespace` and the package root. Every other artifact still uses "the user" / "the owner".

### Placeholder screen
One `ComponentActivity` with `setContent { MaterialTheme { ... } }`, showing the app name from `strings.xml`. The name lives in string resources from the start, so the Spanish translation later is a resource addition.

## Risks / Trade-offs

- [The latest AGP/Kotlin/Compose combination has an incompatibility] → Pick the newest combination the Compose and AGP release notes declare compatible. Record the versions in the catalog. Do not upgrade AGP and Kotlin separately without checking.
- [`allWarningsAsErrors` blocks progress on a deprecation in generated or third-party code] → Fix at the source. Never suppress globally. A narrow, commented `@Suppress` needs reviewer approval.
- [First build is slow and heavy on RAM] → The daemon heap is bounded. This is a one-time dependency download.
