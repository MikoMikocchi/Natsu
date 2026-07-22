plugins {
    id("natsudroid.android.application")
    alias(libs.plugins.module.graph.assert)
}

android {
    namespace = "io.mikoshift.natsudroid"
    defaultConfig {
        applicationId = "io.mikoshift.natsudroid"
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "OAUTH_CLIENT_ID", "\"natsudroid-mobile\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:3000/v1/\"")
            buildConfigField("String", "ROOT_BASE_URL", "\"http://10.0.2.2:3000/\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("String", "BASE_URL", "\"https://api.natsu.mikoshift.io/v1/\"")
            buildConfigField("String", "ROOT_BASE_URL", "\"https://api.natsu.mikoshift.io/\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

moduleGraphAssert {
    maxHeight = 4
    configurations = setOf("api", "implementation")
    allowed =
        arrayOf(
            ":app -> :.*",
            ":core:testing -> :core:.*",
            ":core:data -> :core:.*",
            ":core:domain -> :core:.*",
            ":core:database -> :core:model",
            ":feature:.* -> :core:.*",
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
    rootProject.subprojects
        .filter { it.path.startsWith(":feature:") }
        .forEach { implementation(project(it.path)) }
}
