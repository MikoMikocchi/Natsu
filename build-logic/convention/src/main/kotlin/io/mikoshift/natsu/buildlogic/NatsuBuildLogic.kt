package io.mikoshift.natsu.buildlogic

import org.gradle.api.Project

internal fun Project.isKoverEnabled(): Boolean = providers.gradleProperty("enableKover").orNull?.toBooleanStrictOrNull()
    ?: providers.environmentVariable("CI").isPresent
