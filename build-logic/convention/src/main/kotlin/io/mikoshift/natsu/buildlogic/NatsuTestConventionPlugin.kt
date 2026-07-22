package io.mikoshift.natsu.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.withType

class NatsuTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val testLauncher =
                extensions.getByType(JavaToolchainService::class.java).launcherFor {
                    languageVersion.set(JavaLanguageVersion.of(NatsuAndroidDefaults.JAVA_TOOLCHAIN))
                }

            tasks.withType<Test>().configureEach {
                javaLauncher.set(testLauncher)
                maxParallelForks =
                    (Runtime.getRuntime().availableProcessors() / 2)
                        .coerceIn(1, 8)
            }
        }
    }
}
