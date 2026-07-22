package io.mikoshift.natsudroid.buildlogic

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

internal object NatsudroidAndroidDefaults {
    const val COMPILE_SDK = 37
    const val TARGET_SDK = 37
    const val MIN_SDK = 26
    const val JAVA_TOOLCHAIN = 21
    val JAVA_VERSION: JavaVersion = JavaVersion.VERSION_21
}

internal fun LibraryExtension.configureNatsudroidDefaults() {
    compileSdk {
        version = release(NatsudroidAndroidDefaults.COMPILE_SDK)
    }
    defaultConfig {
        minSdk = NatsudroidAndroidDefaults.MIN_SDK
    }
    compileOptions {
        sourceCompatibility = NatsudroidAndroidDefaults.JAVA_VERSION
        targetCompatibility = NatsudroidAndroidDefaults.JAVA_VERSION
    }
    lint {
        abortOnError = true
        checkReleaseBuilds = false
    }
}

internal fun ApplicationExtension.configureNatsudroidDefaults() {
    compileSdk {
        version = release(NatsudroidAndroidDefaults.COMPILE_SDK)
    }
    defaultConfig {
        minSdk = NatsudroidAndroidDefaults.MIN_SDK
        targetSdk = NatsudroidAndroidDefaults.TARGET_SDK
    }
    compileOptions {
        sourceCompatibility = NatsudroidAndroidDefaults.JAVA_VERSION
        targetCompatibility = NatsudroidAndroidDefaults.JAVA_VERSION
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
        jvmToolchain(NatsudroidAndroidDefaults.JAVA_TOOLCHAIN)
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(NatsudroidAndroidDefaults.JAVA_VERSION.toString()))
        }
    }
}

internal fun Project.configureNatsudroidTests() {
    val testLauncher =
        extensions.getByType(JavaToolchainService::class.java).launcherFor {
            languageVersion.set(JavaLanguageVersion.of(NatsudroidAndroidDefaults.JAVA_TOOLCHAIN))
        }

    tasks.withType<Test>().configureEach {
        javaLauncher.set(testLauncher)
        maxParallelForks =
            (Runtime.getRuntime().availableProcessors() / 2)
                .coerceIn(1, 8)
    }
}
