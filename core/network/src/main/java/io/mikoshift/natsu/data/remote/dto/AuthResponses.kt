package io.mikoshift.natsu.data.remote.dto

import java.time.Instant
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
    @Serializable(with = InstantIso8601Serializer::class)
    @SerialName("created_at") val createdAt: Instant,
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Long,
)

@Serializable
data class RegisterResponse(
    val user: UserResponse,
    @SerialName("server_time_ms") val serverTimeMs: Long,
)

@Serializable
data class UserInfoResponse(
    val sub: String,
    val email: String,
    val name: String,
)

@Serializable
data class MessageResponse(
    val message: String,
)

@Serializable
data class DeviceSessionResponse(
    val id: String,
    val name: String,
    @Serializable(with = InstantIso8601Serializer::class)
    @SerialName("created_at") val createdAt: Instant,
    val current: Boolean,
)
