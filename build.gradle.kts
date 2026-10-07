plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}

subprojects {
    apply(plugin = rootProject.libs.plugins.ktlint.get().pluginId)
    apply(plugin = rootProject.libs.plugins.detekt.get().pluginId)

    extensions.configure<dev.detekt.gradle.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        // detekt 2.x has no maxIssues: failing on Warning severity makes any finding fail the build.
        failOnSeverity = dev.detekt.gradle.extensions.FailOnSeverity.Warning
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    }

    dependencies {
        "detektPlugins"(rootProject.libs.detekt.compose.rules)
    }
}

// Single entry point for every analyzer: Android Lint, ktlint and detekt.
tasks.register("lintAll") {
    group = "verification"
    description = "Runs Android Lint, ktlint and detekt on every module."
    dependsOn(":app:lint")
    subprojects {
        dependsOn(tasks.named("ktlintCheck"), tasks.named("detekt"))
    }
}
