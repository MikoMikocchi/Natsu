plugins {
    id("natsu.android.feature")
}

android {
    namespace = "io.mikoshift.natsu.feature.library"
}

dependencies {
    implementation(libs.androidx.compose.material.icons.extended)
}
