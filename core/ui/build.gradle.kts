plugins {
    id("natsu.android.library")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.mikoshift.natsu.core.ui"
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.core)
}
