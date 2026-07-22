import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "io.mikoshift.natsu.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("jvmLibrary") {
            id = "natsu.jvm.library"
            implementationClass = "io.mikoshift.natsu.buildlogic.NatsuJvmLibraryConventionPlugin"
        }
        register("androidLibrary") {
            id = "natsu.android.library"
            implementationClass = "io.mikoshift.natsu.buildlogic.NatsuAndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "natsu.android.feature"
            implementationClass = "io.mikoshift.natsu.buildlogic.NatsuAndroidFeatureConventionPlugin"
        }
        register("androidApplication") {
            id = "natsu.android.application"
            implementationClass = "io.mikoshift.natsu.buildlogic.NatsuAndroidApplicationConventionPlugin"
        }
    }
}
