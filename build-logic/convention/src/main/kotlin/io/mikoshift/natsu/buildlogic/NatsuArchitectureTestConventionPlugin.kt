package io.mikoshift.natsu.buildlogic

import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class NatsuArchitectureTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("natsu.test")

            extensions.configure<LibraryExtension> {
                compileSdk {
                    version = release(37)
                }
                defaultConfig {
                    minSdk = 26
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_11
                    targetCompatibility = JavaVersion.VERSION_11
                }
            }

            extensions.configure<LibraryAndroidComponentsExtension> {
                beforeVariants(selector().all()) { builder ->
                    builder.enable = builder.buildType == "debug"
                }
            }
        }
    }
}
