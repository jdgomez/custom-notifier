## Context

The skeleton (`add-android-project-skeleton`) has two modules: `:app` (Android + Compose) and `:domain` (Kotlin/JVM). Kotlin compiler warnings already fail the build. Versions live in `gradle/libs.versions.toml`.

## Goals / Non-Goals

**Goals:**
- Zero-warning analysis that is enforceable from one command, identical locally, in the gate and in CI.

**Non-Goals:**
- Tuning thresholds for code that does not exist yet. Start from the defaults and adjust only when real code shows a rule is wrong for this project, with a comment.

## Decisions

### Android Lint: strict, no baseline
In `:app`: `lint { warningsAsErrors = true; abortOnError = true; checkDependencies = true }`, with no `baseline`. The `lint.xml` file is created only if a specific check must be configured, and each entry needs a comment.

### ktlint via `org.jlleitschuh.gradle.ktlint`
It applies to every Kotlin module and provides `ktlintCheck` / `ktlintFormat`. The style lives in `.editorconfig` (the standard ktlint mechanism, also read by IDEs):
- `ktlint_code_style = ktlint_official`
- `ktlint_function_naming_ignore_when_annotated_with = Composable`
- Alternative: `kotlinter`. Also valid, but less widely used in Android projects. Rejected for ecosystem familiarity.
- Alternative: running ktlint through detekt's formatting plugin. Rejected: one tool per concern, and the ktlint integration there lags behind ktlint releases.

### detekt with the Compose rule set
Apply the plugin to every Kotlin module with `buildUponDefaultConfig = true` and a committed `config/detekt/detekt.yml` that contains only overrides. Add the Compose rules through `detektPlugins`. Set `warningsAsErrors: true` in the config and `failOnSeverity = Warning` in the Gradle extension (detekt 2.x has no `maxIssues`), so any finding fails the build. No baseline.
- Compatibility: detekt must support the project's Kotlin version. If the stable 1.x line does not support it, use the detekt 2.x line and record the reason in the catalog comment. Downgrading Kotlin to suit detekt is not an option.

### Aggregate task
Register a root task `lintAll` that depends on `:app:lint`, `ktlintCheck` and `detekt` across modules. Wire `check` to depend on these analyzers. The format command is `./gradlew ktlintFormat`.

### Typed-resolution detekt
Use plain `detekt` first, because it is fast. Consider `detektMain` (type resolution) later, when real code shows a need, as a separate decision.

## Risks / Trade-offs

- [detekt and ktlint disagree on a formatting rule] → ktlint owns formatting. Turn off detekt's overlapping style rules in `detekt.yml`, with a comment.
- [Android Lint flags missing translations once Spanish arrives] → Expected. The translation change fixes those findings instead of suppressing them.
- [`checkDependencies` slows lint as modules grow] → Acceptable at this size. Revisit if lint time becomes a bottleneck in CI.
