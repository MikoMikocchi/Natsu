package io.mikoshift.natsu.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion

internal object NatsuAndroidDefaults {
    const val COMPILE_SDK = 37
    const val MIN_SDK = 26
    val JAVA_VERSION: JavaVersion = JavaVersion.VERSION_17
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
}

internal fun ApplicationExtension.configureNatsuDefaults() {
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
}

internal fun org.gradle.api.Project.applyKotlinAndroidIfNeeded() {
    if (extensions.findByName("kotlin") == null &&
        !pluginManager.hasPlugin("org.jetbrains.kotlin.android")
    ) {
        pluginManager.apply("org.jetbrains.kotlin.android")
    }
}

internal fun org.gradle.api.Project.applyKoverIfEnabled() {
    if (isKoverEnabled()) {
        pluginManager.apply("org.jetbrains.kotlinx.kover")
    }
}
