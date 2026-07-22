package io.mikoshift.natsu.buildlogic

import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

class NatsuArchitectureTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("natsu.android.library")
            pluginManager.apply("natsu.test")

            extensions.configure<LibraryAndroidComponentsExtension> {
                beforeVariants(selector().all()) { builder ->
                    builder.enable = builder.buildType == "debug"
                }
            }

            tasks.withType<Test>().configureEach {
                useJUnitPlatform()
            }
        }
    }
}
