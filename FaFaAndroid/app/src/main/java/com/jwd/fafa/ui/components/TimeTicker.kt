package com.jwd.fafa.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/** 每约 500ms 刷新一次的当前时间（倒计时展示用）。 */
@Composable
fun rememberNowMs(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            now = System.currentTimeMillis()
        }
    }
    return now
}

fun formatMmss(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0)
    return "%02d:%02d".format(s / 60, s % 60)
}
