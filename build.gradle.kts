plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    parallel = true
}

subprojects {
    pluginManager.withPlugin("com.android.application") {
        applyDetektAndKover()
    }
    pluginManager.withPlugin("com.android.library") {
        applyDetektAndKover()
    }
    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        applyDetektAndKover()
    }
}

fun Project.applyDetektAndKover() {
    if (!pluginManager.hasPlugin("dev.detekt")) {
        apply(plugin = "dev.detekt")
    }
    if (!pluginManager.hasPlugin("org.jetbrains.kotlinx.kover")) {
        apply(plugin = "org.jetbrains.kotlinx.kover")
    }
    dependencies {
        add("detektPlugins", rootProject.libs.detekt.ktlint)
    }

    extensions.configure<dev.detekt.gradle.extensions.DetektExtension> {
        source.from(
            "src/main/java",
            "src/main/kotlin",
        )
    }

    tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
        val mainSources = listOf("src/main/java", "src/main/kotlin").map { path ->
            file(path)
        }.filter { it.exists() }
        if (mainSources.isNotEmpty()) {
            setSource(mainSources)
        }
    }

    afterEvaluate {
        tasks.matching { task ->
            task.name.startsWith("detekt") && (
                task.name.contains("UnitTest", ignoreCase = true) ||
                task.name.contains("AndroidTest", ignoreCase = true) ||
                task.name == "detektTest" ||
                task.name == "detektTestSourceSet"
                )
        }.configureEach {
            enabled = false
        }

        val primaryDetektTask = tasks.findByName("detektProdReleaseSourceSet")
            ?: tasks.findByName("detektReleaseSourceSet")
            ?: tasks.findByName("detektMainSourceSet")
            ?: tasks.findByName("detektMain")

        if (primaryDetektTask != null) {
            tasks.matching { task ->
                task.name.startsWith("detekt") &&
                    (task.name.endsWith("SourceSet") || task.name == "detektMain") &&
                    task != primaryDetektTask
            }.configureEach {
                enabled = false
            }
            tasks.named("detekt").configure {
                setDependsOn(listOf(primaryDetektTask))
            }
        }
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
    description = "Runs static analysis, unit tests, and module graph checks for CI."
    dependsOn("detekt", ":app:assertModuleGraph", "koverXmlReport")
}

gradle.projectsEvaluated {
    tasks.named("ciCheck") {
        dependsOn(
            subprojects.mapNotNull { subproject ->
                subproject.tasks.findByName("test")?.let { "${subproject.path}:test" }
            },
        )
    }
}
