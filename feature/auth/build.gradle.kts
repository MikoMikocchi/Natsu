plugins {
    id("natsudroid.android.feature")
}

android {
    namespace = "io.mikoshift.natsudroid.feature.auth"
}

dependencies {
    implementation(libs.androidx.compose.material.icons.extended)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
