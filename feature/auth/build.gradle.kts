plugins {
    id("natsu.android.feature")
}

android {
    namespace = "io.mikoshift.natsu.feature.auth"
}

dependencies {
    implementation(libs.androidx.compose.material.icons.extended)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
