package com.jwd.fafa

import com.jwd.fafa.core.common.parseServerTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class ServerTimeParseTest {

    @Test
    fun parsesIsoWithOffset() {
        val odt = parseServerTime("2026-10-26T08:59:42.789+08:00")
        assertEquals(2026, odt.year)
        assertEquals(8, odt.hour)
    }

    @Test
    fun parsesIsoWithZ() {
        val odt = parseServerTime("2026-10-26T08:59:42.789Z")
        assertEquals(ZoneOffset.UTC, odt.offset)
    }

    @Test
    fun fallsBackToUtcWhenOffsetMissing() {
        // 接口文档示例返回 "2026-10-26T08:59:42.789"（无偏移），按 UTC 兜底
        val odt = parseServerTime("2026-10-26T08:59:42.789")
        assertEquals(ZoneOffset.UTC, odt.offset)
        assertEquals(8, odt.hour)
    }
}
