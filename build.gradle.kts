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

val enableKover = (findProperty("enableKover") as String?)?.toBooleanStrictOrNull() == true

fun pathToModule(path: String): String? =
    when {
        path.startsWith("app/") -> ":app"
        path.startsWith("core/") -> ":core:${path.substringAfter("core/").substringBefore("/")}"
        path.startsWith("feature/") -> ":feature:${path.substringAfter("feature/").substringBefore("/")}"
        else -> null
    }

fun requiresFullCi(changedFiles: List<String>): Boolean =
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

fun org.gradle.api.Project.unitTestTaskPath(): String? =
    listOf("testDevDebugUnitTest", "testDebugUnitTest", "test")
        .firstNotNullOfOrNull { taskName -> tasks.findByName(taskName)?.path }

fun org.gradle.api.Project.detektTaskPath(): String? =
    tasks.findByName("detekt")?.path

fun requiresDetekt(
    changedFiles: List<String>,
    modulePath: String,
): Boolean =
    changedFiles.any { path ->
        pathToModule(path) == modulePath && path.endsWith(".kt")
    }

fun org.gradle.api.Project.hasUnitTestSources(): Boolean {
    if (path == ":app") {
        return false
    }
    return file("src/test").exists()
}

fun resolveAffectedTaskPaths(
    changedFiles: List<String>,
    root: org.gradle.api.Project,
): List<String> {
    if (changedFiles.isEmpty()) {
        return emptyList()
    }
    if (requiresFullCi(changedFiles)) {
        return listOf(":ciCheck")
    }

    val taskPaths =
        linkedSetOf(
            "spotlessKotlinCheck",
            "spotlessKotlinGradleCheck",
            ":app:assertModuleGraph",
        )

    val affectedModules = changedFiles.mapNotNull { pathToModule(it) }.toSet()

    affectedModules
        .filter { modulePath -> requiresDetekt(changedFiles, modulePath) }
        .forEach { modulePath ->
            root.project(modulePath).detektTaskPath()?.let { taskPaths += it }
        }

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

val gitBaseRefProvider = providers.gradleProperty("gitBaseRef").orElse("main")

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

tasks.register("detektCheck") {
    group = "verification"
    description = "Runs detekt on all modules."
}

tasks.register("ciCheck") {
    group = "verification"
    description = "CI: formatting, detekt, app lint, module graph, unit tests, architecture tests."
    dependsOn(
        "spotlessKotlinCheck",
        "spotlessKotlinGradleCheck",
        "detektCheck",
        ":app:assertModuleGraph",
        ":app:lintDevDebug",
        ":core:architecture-test:testDebugUnitTest",
    )
    if (enableKover) {
        dependsOn("koverXmlReport", "koverVerify")
    }
}

tasks.register("affectedCheck") {
    group = "verification"
    description =
        "Fast local/PR check for changes since gitBaseRef (default: main). " +
        "Use -PgitBaseRef=--cached for staged changes."
    dependsOn(
        changedFilesProvider.map { changedFiles ->
            resolveAffectedTaskPaths(changedFiles, project).map { taskPath ->
                tasks.named(taskPath)
            }
        },
    )
}

tasks.register("dev") {
    group = "application"
    description = "Assemble dev debug APK for local development."
    dependsOn(":app:assembleDevDebug")
}

subprojects {
    afterEvaluate {
        tasks.findByName("detekt")?.let { detektTask ->
            rootProject.tasks.named("detektCheck").configure {
                dependsOn(detektTask)
            }
        }

        if (!hasUnitTestSources()) {
            return@afterEvaluate
        }

        unitTestTaskPath()?.let { testTaskPath ->
            rootProject.tasks.named("ciCheck").configure {
                dependsOn(testTaskPath)
            }
        }
    }
}
