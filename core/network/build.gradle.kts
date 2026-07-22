plugins {
    id("natsudroid.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.mikoshift.natsudroid.core.network"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization.converter)
}
