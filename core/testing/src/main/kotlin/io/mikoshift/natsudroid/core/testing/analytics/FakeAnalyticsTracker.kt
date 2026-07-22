package io.mikoshift.natsudroid.core.testing.analytics

import io.mikoshift.natsudroid.core.common.analytics.AnalyticsTracker

class FakeAnalyticsTracker : AnalyticsTracker {
    val events = mutableListOf<Pair<String, Map<String, String>>>()

    override fun track(event: String, params: Map<String, String>) {
        events += event to params
    }
}
