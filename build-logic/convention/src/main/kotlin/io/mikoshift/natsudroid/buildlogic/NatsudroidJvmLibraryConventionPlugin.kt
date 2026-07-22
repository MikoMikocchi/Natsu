package io.mikoshift.natsudroid.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class NatsudroidJvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")

            extensions.configure<KotlinJvmProjectExtension> {
                jvmToolchain(NatsudroidAndroidDefaults.JAVA_TOOLCHAIN)
                compilerOptions {
                    jvmTarget.set(JvmTarget.fromTarget(NatsudroidAndroidDefaults.JAVA_TOOLCHAIN.toString()))
                }
            }
            configureNatsudroidTests()
        }
    }
}
