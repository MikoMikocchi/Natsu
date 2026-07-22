plugins {
    id("natsudroid.jvm.library")
}

dependencies {
    api(project(":core:domain"))
    api(project(":core:model"))
    api(project(":core:common"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.test)
    implementation(libs.junit)
    implementation(libs.turbine)
}
