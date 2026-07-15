package io.mikoshift.natsu.data.mapper

import io.mikoshift.natsu.core.model.AuthSession
import io.mikoshift.natsu.core.model.DeviceSession
import io.mikoshift.natsu.core.model.User
import io.mikoshift.natsu.data.remote.dto.AuthResponse
import io.mikoshift.natsu.data.remote.dto.DeviceSessionResponse
import io.mikoshift.natsu.data.remote.dto.UserResponse

fun AuthResponse.toSession(): AuthSession = AuthSession(
    accessToken = token,
    refreshToken = refreshToken,
    userId = user.id,
    userName = user.name,
    userEmail = user.email,
)

fun UserResponse.toDomain(): User = User(
    id = id,
    name = name,
    email = email,
    createdAt = createdAt,
)

fun DeviceSessionResponse.toDomain(): DeviceSession = DeviceSession(
    id = id,
    name = name,
    createdAt = createdAt,
    current = current,
)
