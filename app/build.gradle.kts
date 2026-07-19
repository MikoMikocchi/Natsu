plugins {
    id("natsu.android.application")
    alias(libs.plugins.module.graph.assert)
}

android {
    namespace = "io.mikoshift.natsu"
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        applicationId = "io.mikoshift.natsu"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "OAUTH_CLIENT_ID", "\"natsu-mobile\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:3000/v1/\"")
            buildConfigField("String", "ROOT_BASE_URL", "\"http://10.0.2.2:3000/\"")
        }
        create("staging") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"https://staging-api.natsu.mikoshift.io/v1/\"")
            buildConfigField("String", "ROOT_BASE_URL", "\"https://staging-api.natsu.mikoshift.io/\"")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"https://api.natsu.mikoshift.io/v1/\"")
            buildConfigField("String", "ROOT_BASE_URL", "\"https://api.natsu.mikoshift.io/\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

moduleGraphAssert {
    maxHeight = 4
    allowed =
        arrayOf(
            ":app -> :.*",
            ":core:data -> :core:.*",
            ":core:domain -> :core:model",
            ":core:domain -> :core:common",
            ":core:testing -> :core:.*",
            ":core:architecture-test -> :core:.*",
            ":core:architecture-test -> :feature:.*",
            ":feature:.* -> :core:domain",
            ":feature:.* -> :core:model",
            ":feature:.* -> :core:ui",
            ":feature:.* -> :core:navigation",
            ":core:database -> :core:model",
        )
    restricted =
        arrayOf(
            ":core:domain -X> :core:data",
            ":core:domain -X> :core:network",
            ":core:domain -X> :core:database",
            ":core:domain -X> :feature:.*",
            ":core:model -X> :core:.*",
            ":core:model -X> :feature:.*",
            ":core:model -X> :app",
            ":core:common -X> :core:.*",
            ":core:common -X> :feature:.*",
            ":core:common -X> :app",
            ":core:data -X> :feature:.*",
            ":core:data -X> :app",
            ":feature:.* -X> :core:data",
            ":feature:.* -X> :core:network",
            ":feature:.* -X> :core:database",
            ":feature:.* -X> :feature:.*",
            ":core:ui -X> :core:.*",
            ":core:ui -X> :feature:.*",
            ":core:ui -X> :app",
            ":core:navigation -X> :core:.*",
            ":core:navigation -X> :feature:.*",
            ":core:navigation -X> :app",
            ":core:network -X> :core:.*",
            ":core:network -X> :feature:.*",
            ":core:network -X> :app",
            ":core:database -X> :core:domain",
            ":core:database -X> :core:data",
            ":core:database -X> :core:network",
            ":core:database -X> :feature:.*",
            ":core:database -X> :app",
        )
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:library"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:reader"))
}
