plugins {
    id("natsudroid.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.mikoshift.natsudroid.navigation"
}

dependencies {
    api(libs.kotlinx.serialization.json)
    implementation(libs.androidx.navigation.common)

    testImplementation(libs.junit)
}
