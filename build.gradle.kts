plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kover)
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

kover {
    reports {
        filters {
            excludes {
                packages(
                    "*.di",
                    "*.generated",
                    "*.Hilt_*",
                    "dagger.hilt.internal",
                )
                classes(
                    "*_*Factory",
                    "*_*Factory\$*",
                    "*_HiltModules*",
                    "*_MembersInjector",
                )
            }
        }
        total {
            html {
                onCheck = false
            }
            xml {
                onCheck = true
            }
        }
    }
}

tasks.register("ciCheck") {
    group = "verification"
    description = "Runs formatting, static analysis, architecture tests, module graph checks, unit tests, and coverage."
    dependsOn(
        "spotlessCheck",
        ":app:assertModuleGraph",
        ":core:architecture-test:test",
        "koverXmlReport",
    )
}

gradle.projectsEvaluated {
    tasks.named("ciCheck") {
        dependsOn(
            subprojects.mapNotNull { subproject ->
                subproject.tasks.findByName("detekt")?.let { "${subproject.path}:detekt" }
            },
        )
        dependsOn(
            subprojects
                .filter { it.path != ":core:architecture-test" }
                .mapNotNull { subproject ->
                    subproject.tasks.findByName("test")?.let { "${subproject.path}:test" }
                },
        )
    }
}

tasks.register("installGitHooks") {
    group = "setup"
    description = "Installs git hooks from .githooks/ into .git/hooks/."
    doLast {
        val hooksDir = rootProject.file(".githooks")
        val gitHooksDir = rootProject.file(".git/hooks")
        if (!gitHooksDir.exists()) {
            throw GradleException("Not a git repository: ${gitHooksDir.path}")
        }
        hooksDir.listFiles()?.filter { it.isFile }?.forEach { hook ->
            val target = gitHooksDir.resolve(hook.name)
            hook.copyTo(target, overwrite = true)
            target.setExecutable(true)
        }
    }
}
