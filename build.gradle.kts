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
    pluginManager.withPlugin("org.jetbrains.kotlin.android") {
        applyDetektAndKover()
    }
    pluginManager.withPlugin("org.jetbrains.kotlin.plugin.compose") {
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
    tasks.matching { task ->
        task.name.contains("detekt", ignoreCase = true) && task.name.contains("Test", ignoreCase = true)
    }.configureEach {
        enabled = false
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
