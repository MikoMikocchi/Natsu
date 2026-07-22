package io.mikoshift.natsudroid.data.analytics

import io.mikoshift.natsudroid.core.common.analytics.AnalyticsTracker
import io.mikoshift.natsudroid.core.common.logging.AppLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoggingAnalyticsTracker
@Inject
constructor(private val logger: AppLogger) : AnalyticsTracker {
    override fun track(event: String, params: Map<String, String>) {
        val paramsSuffix =
            if (params.isEmpty()) {
                ""
            } else {
                " ${params.entries.joinToString { "${it.key}=${it.value}" }}"
            }
        logger.d(TAG, "event=$event$paramsSuffix")
    }

    private companion object {
        const val TAG = "Analytics"
    }
}
