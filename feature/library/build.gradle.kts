plugins {
    id("natsudroid.android.feature")
}

android {
    namespace = "io.mikoshift.natsudroid.feature.library"
}

dependencies {
    implementation(libs.androidx.compose.material.icons.extended)
}
