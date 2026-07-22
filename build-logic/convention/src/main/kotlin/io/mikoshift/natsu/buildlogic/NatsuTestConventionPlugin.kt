package io.mikoshift.natsu.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType

class NatsuTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            tasks.withType<Test>().configureEach {
                maxParallelForks =
                    (Runtime.getRuntime().availableProcessors() / 2)
                        .coerceIn(1, 4)
            }

            if (path == ":app") {
                tasks.withType<Test>().configureEach {
                    if (name.contains("Staging", ignoreCase = true) ||
                        name.contains("Prod", ignoreCase = true)
                    ) {
                        enabled = false
                    }
                }
            }
        }
    }
}
