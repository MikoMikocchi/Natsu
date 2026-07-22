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

val isCi = System.getenv("CI")?.isNotEmpty() == true

val modulesWithUnitTests =
    setOf(
        ":core:data",
        ":core:domain",
        ":core:model",
        ":core:navigation",
        ":feature:auth",
        ":feature:library",
        ":feature:profile",
        ":feature:reader",
    )

fun gitChangedFiles(baseRef: String): List<String> {
    val process =
        ProcessBuilder("git", "diff", "--name-only", baseRef)
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
    process.waitFor()
    if (process.exitValue() != 0) {
        return emptyList()
    }
    return process.inputStream.bufferedReader().readLines().filter { it.isNotBlank() }
}

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
    description = "Formatting (.kt), line limits, module graph, and detekt."
    dependsOn(
        "spotlessKotlinCheck",
        "verifyMaxSourceFileLines",
        ":app:assertModuleGraph",
    )
}

if (isCi) {
    tasks.named("staticAnalysisCheck").configure {
        dependsOn("spotlessKotlinGradleCheck")
    }
}

tasks.register("quickCheck") {
    group = "verification"
    description = "Fast verification: unit tests (modules with tests, single variant) and architecture tests."
    dependsOn(
        ":app:assertModuleGraph",
        ":core:architecture-test:testDebugUnitTest",
    )
}

tasks.register("affectedCheck") {
    group = "verification"
    description =
        "Runs unit tests for modules changed since gitBaseRef (default: HEAD). " +
        "Use -PgitBaseRef=main to compare against main."
    notCompatibleWithConfigurationCache("Resolves changed modules via git at execution time.")

    val baseRef = findProperty("gitBaseRef") as String? ?: "HEAD"

    doLast {
        val changedFiles = gitChangedFiles(baseRef)
        if (changedFiles.isEmpty()) {
            logger.lifecycle("No changes since $baseRef.")
            return@doLast
        }

        val taskPaths = linkedSetOf<String>()

        if (requiresFullVerification(changedFiles)) {
            logger.lifecycle("Build configuration changed — running quickCheck.")
            taskPaths += ":quickCheck"
        } else {
            val affectedModules = resolveAffectedModules(changedFiles)
            val testModules = affectedModules.intersect(modulesWithUnitTests)

            if (testModules.isEmpty()) {
                logger.lifecycle("No unit-test modules affected (changed: ${affectedModules.joinToString()}).")
            } else {
                logger.lifecycle("Affected test modules: ${testModules.joinToString()}")
                testModules.forEach { modulePath ->
                    project(modulePath).unitTestTaskPath()?.let { taskPaths += it }
                }
            }

            if (requiresArchitectureTest(changedFiles)) {
                taskPaths += ":core:architecture-test:testDebugUnitTest"
            }
        }

        if (taskPaths.isEmpty()) {
            logger.lifecycle("No verification tasks to run.")
            return@doLast
        }

        logger.lifecycle("Running: ${taskPaths.joinToString(" ")}")
        val process =
            ProcessBuilder(
                rootDir.resolve("gradlew").absolutePath,
                *taskPaths.toTypedArray(),
                "--build-cache",
            )
                .directory(rootDir)
                .inheritIO()
                .start()
        if (process.waitFor() != 0) {
            throw GradleException("Affected verification failed.")
        }
    }
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
        }

        if (path !in modulesWithUnitTests) {
            return@afterEvaluate
        }

        unitTestTaskPath()?.let { testTaskPath ->
            rootProject.tasks.named("quickCheck").configure {
                dependsOn(testTaskPath)
            }
        }
    }
}
