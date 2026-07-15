package io.mikoshift.natsu.core.common.analytics

interface AnalyticsTracker {
    fun track(event: String, params: Map<String, String> = emptyMap())
}
