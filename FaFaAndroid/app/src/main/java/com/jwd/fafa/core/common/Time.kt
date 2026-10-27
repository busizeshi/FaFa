package com.jwd.fafa.core.common

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

/**
 * 服务端时间解析：契约要求 ISO 8601 UTC；对无时区偏移的返回值（如 2026-10-26T08:59:42.789）
 * 按 UTC 兜底解析（联调纪要已登记该口径差异）。
 */
fun parseServerTime(text: String): OffsetDateTime = try {
    OffsetDateTime.parse(text)
} catch (_: DateTimeParseException) {
    try {
        Instant.parse(text).atOffset(ZoneOffset.UTC)
    } catch (_: DateTimeParseException) {
        LocalDateTime.parse(text).atOffset(ZoneOffset.UTC)
    }
}
