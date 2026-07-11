package io.mikoshift.natsu.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Note: the backend serializes JSON with snake_case property naming
// (spring.jackson.property-naming-strategy: SNAKE_CASE), so multi-word
// fields need an explicit @SerialName to match the wire format even
// though the Kotlin property names stay camelCase.

@Serializable
data class UserResponse(
    val id: Long,
    val name: String,
    val email: String,
    // ISO-8601 timestamp string from the backend; kept as String until a datetime
    // library is wired up (a later subtask can parse it if needed).
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class AuthResponse(
    val token: String,
    @SerialName("refresh_token") val refreshToken: String,
    val user: UserResponse,
    @SerialName("server_time_ms") val serverTimeMs: Long,
)

@Serializable
data class UserShowResponse(
    val user: UserResponse,
    @SerialName("server_time_ms") val serverTimeMs: Long,
)

@Serializable
data class MessageResponse(
    val message: String,
)

@Serializable
data class DeviceSessionResponse(
    val id: Long,
    val name: String,
    @SerialName("created_at") val createdAt: String,
    val current: Boolean,
)
