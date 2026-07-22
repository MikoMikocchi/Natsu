plugins {
    id("natsudroid.android.feature")
}

android {
    namespace = "io.mikoshift.natsudroid.feature.reader"
}

dependencies {
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)
}
