package io.mikoshift.natsu.buildlogic

import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

class NatsuDetektConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("dev.detekt")

            val libs = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

            dependencies {
                add("detektPlugins", libs.findLibrary("detekt-ktlint").get())
            }

            extensions.configure<DetektExtension> {
                buildUponDefaultConfig.set(true)
                allRules.set(false)
                config.setFrom(rootProject.files("config/detekt/detekt.yml"))
                parallel.set(true)
                source.from("src/main/java", "src/main/kotlin")
            }

            tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
                val mainSources =
                    listOf("src/main/java", "src/main/kotlin")
                        .map { path -> file(path) }
                        .filter { it.exists() }
                if (mainSources.isNotEmpty()) {
                    setSource(mainSources)
                }
            }

            afterEvaluate {
                tasks
                    .matching { task ->
                        task.name.startsWith("detekt") &&
                            (
                                task.name.contains("UnitTest", ignoreCase = true) ||
                                    task.name.contains("AndroidTest", ignoreCase = true) ||
                                    task.name == "detektTest" ||
                                    task.name == "detektTestSourceSet"
                                )
                    }.configureEach {
                        enabled = false
                    }

                val primaryDetektTask =
                    tasks.findByName("detektProdReleaseSourceSet")
                        ?: tasks.findByName("detektReleaseSourceSet")
                        ?: tasks.findByName("detektMainSourceSet")
                        ?: tasks.findByName("detektMain")

                if (primaryDetektTask != null) {
                    tasks
                        .matching { task ->
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
    }
}
