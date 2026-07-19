plugins {
    id("natsu.android.library")
}

android {
    namespace = "io.mikoshift.natsu.architecture.test"
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

dependencies {
    testImplementation(libs.archunit.junit4)
    testImplementation(libs.junit)

    testImplementation(project(":core:model"))
    testImplementation(project(":core:common"))
    testImplementation(project(":core:domain"))
    testImplementation(project(":core:network"))
    testImplementation(project(":core:database"))
    testImplementation(project(":core:data"))
    testImplementation(project(":core:ui"))
    testImplementation(project(":core:navigation"))
    testImplementation(project(":feature:auth"))
    testImplementation(project(":feature:library"))
    testImplementation(project(":feature:profile"))
    testImplementation(project(":feature:reader"))
}
