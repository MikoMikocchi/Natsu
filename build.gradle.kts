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

fun pathToModule(path: String): String? =
    when {
        path.startsWith("app/") -> ":app"
        path.startsWith("core/") -> ":core:${path.substringAfter("core/").substringBefore("/")}"
        path.startsWith("feature/") -> ":feature:${path.substringAfter("feature/").substringBefore("/")}"
        else -> null
    }

fun requiresFullVerification(changedFiles: List<String>): Boolean =
    changedFiles.any { path ->
        path.startsWith("build-logic/") ||
            path.startsWith("gradle/") ||
            path.endsWith("libs.versions.toml") ||
            path == "build.gradle.kts" ||
            path == "settings.gradle.kts" ||
            path.startsWith("config/detekt/") ||
            path.startsWith(".github/")
    }

fun requiresArchitectureTest(changedFiles: List<String>): Boolean =
    changedFiles.any { path ->
        (path.endsWith(".kt") || path.endsWith(".kts")) &&
            path.contains("/src/main/") &&
            !path.startsWith("core/architecture-test/")
    }

fun resolveAffectedModules(changedFiles: List<String>): Set<String> =
    changedFiles.mapNotNull { pathToModule(it) }.toSet()

fun org.gradle.api.Project.unitTestTaskPath(): String? =
    listOf("testDevDebugUnitTest", "testDebugUnitTest", "test")
        .firstNotNullOfOrNull { taskName -> tasks.findByName(taskName)?.path }

fun org.gradle.api.Project.hasUnitTestSources(): Boolean {
    if (path == ":app") {
        return false
    }
    return sequenceOf("src/test/kotlin", "src/test/java")
        .map { file(it) }
        .filter { it.isDirectory }
        .flatMap { dir ->
            dir.walkTopDown().maxDepth(3).filter { it.isFile && it.extension == "kt" }
        }.any()
}

fun resolveVerificationTaskPaths(
    changedFiles: List<String>,
    root: org.gradle.api.Project,
): List<String> {
    if (changedFiles.isEmpty()) {
        return emptyList()
    }
    if (requiresFullVerification(changedFiles)) {
        return listOf(":quickCheck")
    }

    val taskPaths = linkedSetOf<String>()
    val affectedModules = resolveAffectedModules(changedFiles)

    affectedModules
        .mapNotNull { modulePath -> root.project(modulePath).takeIf { it.hasUnitTestSources() } }
        .forEach { project ->
            project.unitTestTaskPath()?.let { taskPaths += it }
        }

    if (requiresArchitectureTest(changedFiles)) {
        taskPaths += ":core:architecture-test:testDebugUnitTest"
    }

    return taskPaths.toList()
}

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
            verify {
                rule {
                    minBound(40)
                }
            }
        }
    }
}

tasks.register("staticAnalysisCheck") {
    group = "verification"
    description = "Formatting, module graph, and detekt."
    dependsOn(
        "spotlessKotlinCheck",
        "spotlessKotlinGradleCheck",
        ":app:assertModuleGraph",
    )
}

tasks.register("quickCheck") {
    group = "verification"
    description = "Fast verification: unit tests (modules with tests, single variant) and architecture tests."
    dependsOn(
        ":app:assertModuleGraph",
        ":core:architecture-test:testDebugUnitTest",
    )
}

val gitBaseRefProvider = providers.gradleProperty("gitBaseRef").orElse("HEAD")

val changedFilesProvider =
    providers
        .exec {
            val baseRef = gitBaseRefProvider.get()
            if (baseRef == "--cached") {
                commandLine("git", "diff", "--name-only", "--cached")
            } else {
                commandLine("git", "diff", "--name-only", baseRef)
            }
            workingDir(rootDir)
        }.standardOutput.asText
        .map { output ->
            output.lines().filter { it.isNotBlank() }
        }

tasks.register("affectedCheck") {
    group = "verification"
    description =
        "Runs verification for modules changed since gitBaseRef (default: HEAD). " +
        "Use -PgitBaseRef=main to compare against main, or --cached for staged changes."
    dependsOn(
        changedFilesProvider.map { changedFiles ->
            resolveVerificationTaskPaths(changedFiles, project).map { taskPath ->
                tasks.named(taskPath)
            }
        },
    )
}

tasks.register("ciCheck") {
    group = "verification"
    description = "Full CI verification: static analysis, unit tests, architecture tests, and coverage."
    dependsOn("staticAnalysisCheck", "quickCheck")
    if (enableKover) {
        dependsOn("koverXmlReport", "koverVerify")
    }
}

subprojects {
    afterEvaluate {
        tasks.findByName("detekt")?.let { detektTask ->
            rootProject.tasks.named("staticAnalysisCheck").configure {
                dependsOn(detektTask)
            }
        }

        if (!hasUnitTestSources()) {
            return@afterEvaluate
        }

        unitTestTaskPath()?.let { testTaskPath ->
            rootProject.tasks.named("quickCheck").configure {
                dependsOn(testTaskPath)
            }
        }
    }
}
