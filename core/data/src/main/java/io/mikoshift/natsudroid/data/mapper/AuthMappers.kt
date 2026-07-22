package io.mikoshift.natsudroid.data.mapper

import io.mikoshift.natsudroid.core.model.AuthSession
import io.mikoshift.natsudroid.core.model.DeviceSession
import io.mikoshift.natsudroid.core.model.User
import io.mikoshift.natsudroid.data.remote.dto.DeviceSessionResponse
import io.mikoshift.natsudroid.data.remote.dto.TokenResponse
import io.mikoshift.natsudroid.data.remote.dto.UserInfoResponse
import io.mikoshift.natsudroid.data.remote.dto.UserResponse

fun TokenResponse.toSession(userInfo: UserInfoResponse): AuthSession = AuthSession(
    accessToken = accessToken,
    refreshToken = refreshToken,
    userId = userInfo.sub.toLong(),
    userName = userInfo.name,
    userEmail = userInfo.email,
)

fun TokenResponse.mergeTokens(existing: AuthSession): AuthSession = existing.copy(
    accessToken = accessToken,
    refreshToken = refreshToken,
)

fun UserInfoResponse.toDomain(): User = User(
    id = sub.toLong(),
    name = name,
    email = email,
    createdAt = null,
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
