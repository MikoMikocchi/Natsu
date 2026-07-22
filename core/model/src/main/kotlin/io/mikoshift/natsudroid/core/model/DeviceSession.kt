package io.mikoshift.natsudroid.core.model

import java.time.Instant

data class DeviceSession(val id: String, val name: String, val createdAt: Instant, val current: Boolean)
