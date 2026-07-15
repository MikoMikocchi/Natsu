package io.mikoshift.natsu.core.model

data class DeviceSession(
    val id: Long,
    val name: String,
    val createdAt: String,
    val current: Boolean,
)
