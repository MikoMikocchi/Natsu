plugins {
    id("natsu.architecture.test")
}

android {
    namespace = "io.mikoshift.natsu.architecture"
}

dependencies {
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    testImplementation(project(":core:model"))
    testImplementation(project(":core:common"))
    testImplementation(project(":core:domain"))
    testImplementation(project(":core:network"))
    testImplementation(project(":core:database"))
    testImplementation(project(":core:data"))
    testImplementation(project(":core:ui"))
    testImplementation(project(":core:navigation"))
    rootProject.subprojects
        .filter { it.path.startsWith(":feature:") }
        .forEach { testImplementation(project(it.path)) }
}
