package io.mikoshift.natsu.data.remote

import io.mikoshift.natsu.BuildConfig

/**
 * Central network configuration for the app's Retrofit clients.
 */
object NetworkConfig {

    /** Base URL for the active build flavor (dev / staging / prod). */
    val BASE_URL: String = BuildConfig.BASE_URL
}
