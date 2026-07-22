plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(
            mapOf(
                "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                "ktlint_standard_filename" to "disabled",
                "ktlint_standard_no-wildcard-imports" to "disabled",
                "max_line_length" to "120",
            ),
        )
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
    }
}

fun Project.hasUnitTestSources(): Boolean {
    if (path == ":app") {
        return false
    }
    return file("src/test").exists()
}

tasks.register("ciCheck") {
    group = "verification"
    description = "CI: formatting, lint, module graph, unit tests."
    dependsOn(
        "spotlessCheck",
        ":app:assertModuleGraph",
        ":app:lintDebug",
    )
}

tasks.register("dev") {
    group = "application"
    description = "Assemble debug APK for local development."
    dependsOn(":app:assembleDebug")
}

subprojects {
    afterEvaluate {
        if (!hasUnitTestSources()) {
            return@afterEvaluate
        }

        val testTask = tasks.findByName("testDebugUnitTest") ?: tasks.findByName("test")
        if (testTask != null) {
            rootProject.tasks.named("ciCheck").configure {
                dependsOn(testTask)
            }
        }
    }
}
