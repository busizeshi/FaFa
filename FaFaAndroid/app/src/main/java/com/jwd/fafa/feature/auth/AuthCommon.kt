package com.jwd.fafa.feature.auth

import com.jwd.fafa.core.common.parseServerTime
import com.jwd.fafa.core.network.ApiError
import com.jwd.fafa.core.network.ChallengeDto
import com.jwd.fafa.core.network.ErrorCode
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/** UI 层使用的挑战数据（把 expiresAt 转毫秒，避免页面解析时间）。 */
data class ChallengeUi(
    val challenge: ChallengeDto,
    val expiresAtEpochMs: Long,
    val idempotencyKey: String? = null,
)

internal fun ChallengeDto.toUi(idempotencyKey: String? = null): ChallengeUi {
    val expiryMs: Long = expiresAt?.let { text -> parseServerTime(text).toInstant().toEpochMilli() }
        ?: (System.currentTimeMillis() + DEFAULT_CODE_TTL_MS)
    return ChallengeUi(challenge = this, expiresAtEpochMs = expiryMs, idempotencyKey = idempotencyKey)
}

/** 业务错误 → 用户可读文案（各页面特定 code 由 VM 覆写）。 */
internal fun ApiError.Biz.authMessage(): String = when (code) {
    ErrorCode.BAD_REQUEST -> fieldErrors.values.firstOrNull() ?: displayMessage
    ErrorCode.SERVER_BUSY -> "系统繁忙，请稍后重试"
    else -> displayMessage
}

/** 服务端返回的 UTC 时间 → 展示文本（设备时区）。 */
internal fun displayTime(isoUtc: String?): String {
    if (isoUtc.isNullOrBlank()) return "-"
    return try {
        val odt: OffsetDateTime = parseServerTime(isoUtc)
        odt.atZoneSameInstant(java.time.ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
    } catch (_: Exception) {
        isoUtc
    }
}

internal const val DEFAULT_RESEND_COOLDOWN_MS: Long = 60_000L
internal const val DEFAULT_CODE_TTL_MS: Long = 10 * 60_000L
