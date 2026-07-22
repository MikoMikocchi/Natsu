package io.mikoshift.natsu.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class NatsuJvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            pluginManager.apply("natsu.detekt")
            pluginManager.apply("natsu.test")
            applyKoverIfEnabled()

            extensions.configure<KotlinJvmProjectExtension> {
                jvmToolchain(26)
            }
        }
    }
}
