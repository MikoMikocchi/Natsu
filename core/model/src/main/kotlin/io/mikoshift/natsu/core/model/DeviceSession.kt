package io.mikoshift.natsu.core.model

import java.time.Instant

data class DeviceSession(
    val id: Long,
    val name: String,
    val createdAt: Instant,
    val current: Boolean,
)
