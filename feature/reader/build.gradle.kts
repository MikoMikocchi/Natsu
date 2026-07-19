plugins {
    id("natsu.android.feature")
}

android {
    namespace = "io.mikoshift.natsu.feature.reader"
}

dependencies {
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)
}
