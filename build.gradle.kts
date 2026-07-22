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

val spotlessRatchetFrom = findProperty("spotlessRatchetFrom") as String? ?: "main"

val enableKover =
    (findProperty("enableKover") as String?)?.toBooleanStrictOrNull()
        ?: System.getenv("CI")?.isNotEmpty() == true

spotless {
    kotlin {
        ratchetFrom(spotlessRatchetFrom)
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
        ratchetFrom(spotlessRatchetFrom)
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
    }
}

tasks.register("preCommitCheck") {
    group = "verification"
    description = "Fast local checks: formatting (changed files only) and module graph."
    dependsOn(
        "spotlessCheck",
        ":app:assertModuleGraph",
    )
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

tasks.register("verifyMaxSourceFileLines") {
    group = "verification"
    description = "Fails when any production Kotlin source file exceeds the line limit."
    val maxLines = 500
    val rootDirectory = layout.projectDirectory
    val sourceFiles =
        rootDirectory.asFileTree.matching {
            include("**/src/main/java/**/*.kt", "**/src/main/kotlin/**/*.kt")
            exclude("**/build/**")
        }
    inputs.files(sourceFiles).ignoreEmptyDirectories()

    doLast {
        val rootDirFile = rootDirectory.asFile
        val violations = mutableListOf<String>()
        sourceFiles.forEach { file ->
            val lineCount = file.readLines().size
            if (lineCount > maxLines) {
                violations += "${file.relativeTo(rootDirFile)}: $lineCount lines (max $maxLines)"
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Source files exceed $maxLines lines:\n${violations.joinToString("\n")}",
            )
        }
    }
}

tasks.register("staticAnalysisCheck") {
    group = "verification"
    description = "Formatting, line limits, module graph, and detekt."
    dependsOn(
        "spotlessCheck",
        "verifyMaxSourceFileLines",
        ":app:assertModuleGraph",
    )
}

tasks.register("quickCheck") {
    group = "verification"
    description = "Fast local verification: detekt, unit tests (single variant), architecture tests."
    dependsOn(
        ":app:assertModuleGraph",
        ":core:architecture-test:testDebugUnitTest",
    )
}

tasks.register("ciCheck") {
    group = "verification"
    description = "Full CI verification: static analysis, unit tests, architecture tests, and coverage."
    dependsOn("staticAnalysisCheck", "quickCheck")
    if (enableKover) {
        dependsOn("koverXmlReport")
    }
}

subprojects {
    afterEvaluate {
        tasks.findByName("detekt")?.let { detektTask ->
            rootProject.tasks.named("staticAnalysisCheck").configure {
                dependsOn(detektTask)
            }
            rootProject.tasks.named("quickCheck").configure {
                dependsOn(detektTask)
            }
        }

        if (path == ":core:architecture-test") {
            return@afterEvaluate
        }

        val unitTestTaskPath =
            listOf("testDevDebugUnitTest", "testDebugUnitTest", "test")
                .firstNotNullOfOrNull { taskName -> tasks.findByName(taskName)?.path }
                ?: return@afterEvaluate

        rootProject.tasks.named("quickCheck").configure {
            dependsOn(unitTestTaskPath)
        }
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
