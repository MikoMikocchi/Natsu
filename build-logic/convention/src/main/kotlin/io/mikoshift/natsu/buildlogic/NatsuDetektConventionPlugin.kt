package io.mikoshift.natsu.buildlogic

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

class NatsuDetektConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("io.gitlab.arturbosch.detekt")

            extensions.configure<DetektExtension> {
                buildUponDefaultConfig = true
                allRules = false
                config.setFrom(rootProject.files("config/detekt/detekt.yml"))
                parallel = true
                source.from("src/main/java", "src/main/kotlin")
            }

            tasks.withType<Detekt>().configureEach {
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
                    tasks.findByName("detektDevDebugSourceSet")
                        ?: tasks.findByName("detektDebugSourceSet")
                        ?: tasks.findByName("detektMainSourceSet")
                        ?: tasks.findByName("detektMain")
                        ?: tasks.findByName("detektReleaseSourceSet")
                        ?: tasks.findByName("detektProdReleaseSourceSet")

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
