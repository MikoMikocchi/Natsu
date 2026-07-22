plugins {
    id("natsudroid.jvm.library")
}

dependencies {
    implementation(libs.javax.inject)
    implementation(libs.kotlinx.coroutines.core)
}
