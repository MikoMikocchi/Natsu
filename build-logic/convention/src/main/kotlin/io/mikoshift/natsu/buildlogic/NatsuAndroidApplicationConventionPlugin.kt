package io.mikoshift.natsu.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class NatsuAndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            applyKotlinAndroidIfNeeded()
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
            pluginManager.apply("com.google.devtools.ksp")
            pluginManager.apply("com.google.dagger.hilt.android")

            extensions.configure<ApplicationExtension> {
                configureNatsuDefaults()
            }
            configureKotlinJvmTarget()
            configureNatsuTests()

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

            dependencies {
                add("implementation", platform(libs.findLibrary("androidx-compose-bom").get()))
                add("implementation", libs.findLibrary("androidx-activity-compose").get())
                add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
                add("implementation", libs.findLibrary("androidx-core-ktx").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
                add("implementation", libs.findLibrary("androidx-navigation-compose").get())
                add("implementation", libs.findLibrary("kotlinx-coroutines-android").get())
                add("implementation", libs.findLibrary("kotlinx-serialization-json").get())
                add("implementation", libs.findLibrary("androidx-work-runtime-ktx").get())
                add("implementation", libs.findLibrary("androidx-hilt-work").get())
                add("implementation", libs.findLibrary("hilt-android").get())
                add("ksp", libs.findLibrary("hilt-compiler").get())
                add("ksp", libs.findLibrary("kotlin-metadata-jvm").get())
                add("ksp", libs.findLibrary("androidx-hilt-compiler").get())
                add("implementation", libs.findLibrary("hilt-navigation-compose").get())

                add("testImplementation", project(":core:testing"))
                add("testImplementation", libs.findLibrary("junit").get())
                add("androidTestImplementation", platform(libs.findLibrary("androidx-compose-bom").get()))
                add("androidTestImplementation", libs.findLibrary("androidx-compose-material3").get())
                add("androidTestImplementation", project(":core:testing"))
                add("androidTestImplementation", libs.findLibrary("androidx-compose-ui-test-junit4").get())
                add("androidTestImplementation", libs.findLibrary("androidx-espresso-core").get())
                add("androidTestImplementation", libs.findLibrary("androidx-junit").get())
                add("debugImplementation", libs.findLibrary("androidx-compose-ui-test-manifest").get())
                add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
            }
        }
    }
}
