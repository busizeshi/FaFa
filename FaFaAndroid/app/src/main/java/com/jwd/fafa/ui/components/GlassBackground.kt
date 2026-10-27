package com.jwd.fafa.ui.components

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 液态玻璃背景：柔和高斯模糊光斑（粉/蓝/黄）铺在页面底色上。
 * Android 12+ 使用 RenderEffect 模糊；低版本光斑本身为径向渐变，天然柔和。
 */
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val bg = MaterialTheme.colorScheme.background
    val dark = MaterialTheme.colorScheme.onBackground.luminance() > 0.5f
    val blobAlpha = if (dark) 0.20f else 0.55f
    val blur: Dp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 70.dp else 0.dp

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize().then(if (blur > 0.dp) Modifier.blur(blur) else Modifier)) {
            drawRect(bg)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFF3CFDC).copy(alpha = blobAlpha), Color.Transparent),
                ),
                radius = size.width * 0.62f,
                center = Offset(size.width * 0.92f, size.height * 0.06f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFCBE1F2).copy(alpha = blobAlpha), Color.Transparent),
                ),
                radius = size.width * 0.58f,
                center = Offset(size.width * 0.02f, size.height * 0.34f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFF7ECC4).copy(alpha = blobAlpha * 0.7f), Color.Transparent),
                ),
                radius = size.width * 0.52f,
                center = Offset(size.width * 0.85f, size.height * 0.72f),
            )
        }
        content()
    }
}

private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue
