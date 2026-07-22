import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "io.mikoshift.natsudroid.buildlogic"

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
            id = "natsudroid.jvm.library"
            implementationClass = "io.mikoshift.natsudroid.buildlogic.NatsudroidJvmLibraryConventionPlugin"
        }
        register("androidLibrary") {
            id = "natsudroid.android.library"
            implementationClass = "io.mikoshift.natsudroid.buildlogic.NatsudroidAndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "natsudroid.android.feature"
            implementationClass = "io.mikoshift.natsudroid.buildlogic.NatsudroidAndroidFeatureConventionPlugin"
        }
        register("androidApplication") {
            id = "natsudroid.android.application"
            implementationClass = "io.mikoshift.natsudroid.buildlogic.NatsudroidAndroidApplicationConventionPlugin"
        }
    }
}
