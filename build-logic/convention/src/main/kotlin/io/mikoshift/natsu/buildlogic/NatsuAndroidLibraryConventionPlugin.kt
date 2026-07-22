package io.mikoshift.natsu.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class NatsuAndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("natsu.test")
            pluginManager.apply("natsu.detekt")
            if (isKoverEnabled()) {
                pluginManager.apply("natsu.kover")
            }
        }
    }
}
