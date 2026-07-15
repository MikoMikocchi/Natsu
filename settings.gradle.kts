pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Natsu"
include(":app")
include(":core:model")
include(":core:common")
include(":core:domain")
include(":core:testing")
include(":core:network")
include(":core:database")
include(":core:data")
include(":core:ui")
include(":core:navigation")
include(":feature:auth")
include(":feature:library")
include(":feature:profile")
