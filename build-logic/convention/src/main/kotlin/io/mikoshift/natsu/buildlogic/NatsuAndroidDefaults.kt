package io.mikoshift.natsu.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

internal object NatsuAndroidDefaults {
    const val COMPILE_SDK = 37
    const val TARGET_SDK = 37
    const val MIN_SDK = 26
    const val JAVA_TOOLCHAIN = 21
    val JAVA_VERSION: JavaVersion = JavaVersion.VERSION_21
}

internal fun LibraryExtension.configureNatsuDefaults() {
    compileSdk {
        version = release(NatsuAndroidDefaults.COMPILE_SDK)
    }
    defaultConfig {
        minSdk = NatsuAndroidDefaults.MIN_SDK
    }
    compileOptions {
        sourceCompatibility = NatsuAndroidDefaults.JAVA_VERSION
        targetCompatibility = NatsuAndroidDefaults.JAVA_VERSION
    }
    lint {
        abortOnError = true
        checkReleaseBuilds = false
    }
}

internal fun ApplicationExtension.configureNatsuDefaults() {
    compileSdk {
        version = release(NatsuAndroidDefaults.COMPILE_SDK)
    }
    defaultConfig {
        minSdk = NatsuAndroidDefaults.MIN_SDK
        targetSdk = NatsuAndroidDefaults.TARGET_SDK
    }
    compileOptions {
        sourceCompatibility = NatsuAndroidDefaults.JAVA_VERSION
        targetCompatibility = NatsuAndroidDefaults.JAVA_VERSION
    }
    lint {
        abortOnError = true
        checkReleaseBuilds = false
    }
}

internal fun Project.applyKotlinAndroidIfNeeded() {
    if (extensions.findByName("kotlin") == null &&
        !pluginManager.hasPlugin("org.jetbrains.kotlin.android")
    ) {
        pluginManager.apply("org.jetbrains.kotlin.android")
    }
}

internal fun Project.configureKotlinJvmTarget() {
    extensions.findByType(KotlinAndroidProjectExtension::class.java)?.apply {
        jvmToolchain(NatsuAndroidDefaults.JAVA_TOOLCHAIN)
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(NatsuAndroidDefaults.JAVA_VERSION.toString()))
        }
    }
}

internal fun Project.configureNatsuTests() {
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
