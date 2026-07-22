package io.mikoshift.natsu.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class NatsuKoverConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            if (isKoverEnabled()) {
                pluginManager.apply("org.jetbrains.kotlinx.kover")
            }
        }
    }
}
