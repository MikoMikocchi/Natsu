package io.mikoshift.natsu.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class NatsuAndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            applyKotlinAndroidIfNeeded()

            extensions.configure<LibraryExtension> {
                configureNatsuDefaults()
            }
            configureKotlinJvmTarget()
            configureNatsuTests()
        }
    }
}
