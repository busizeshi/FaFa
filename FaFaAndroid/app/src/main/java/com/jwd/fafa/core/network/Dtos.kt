package com.jwd.fafa.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BootstrapDto(
    val featureFlags: FeatureFlagsDto = FeatureFlagsDto(),
    val legalVersions: Map<String, String> = emptyMap(),
    val uploadCapabilities: UploadCapabilitiesDto? = null,
    val dictionaryVersions: Map<String, Int> = emptyMap(),
    val serverTime: String? = null,
)

@Serializable
data class FeatureFlagsDto(
    val community: Boolean = false,
    val ai: Boolean = false,
    val foodInventory: Boolean = false,
    val reportExport: Boolean = false,
)

@Serializable
data class UploadCapabilitiesDto(
    val maxImageBytes: Long = 0,
    val maxVideoBytes: Long = 0,
    val imageFormats: List<String> = emptyList(),
)

@Serializable
data class LegalAcceptDto(
    val documentType: String,
    val documentVersion: String,
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val acceptedLegal: List<LegalAcceptDto>,
)

/** 注册/找回密码发码共用响应：challengeId + 脱敏邮箱 + 过期时间。 */
@Serializable
data class ChallengeDto(
    val challengeId: Long,
    val email: String,
    val expiresAt: String? = null,
)

@Serializable
data class VerifyRequest(val code: String)

/** challenges/{id}/verify 响应：注册校验返回 session，找回密码返回 resetCredential（二选一）。 */
@Serializable
data class VerifyResultDto(
    val session: SessionDto? = null,
    val resetCredential: String? = null,
)

@Serializable
data class SessionDto(
    val tokenName: String = "fafa-token",
    val tokenValue: String,
    val user: UserDto? = null,
)

@Serializable
data class UserDto(
    val userId: Long = 0,
    val email: String = "",
    val displayName: String? = null,
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class EmailOnlyRequest(val email: String)

@Serializable
data class ResetCredentialDto(val resetCredential: String)

@Serializable
data class ResetPasswordRequest(
    val resetCredential: String,
    val newPassword: String,
)

/** 会话列表项（多设备管理）。 */
@Serializable
data class SessionInfoDto(
    val sessionId: String,
    val device: String? = null,
    val lastActiveAt: String? = null,
    val current: Boolean = false,
)
