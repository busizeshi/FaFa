package com.jwd.fafa.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 页面脚手架：玻璃背景 + 顶部返回（44dp 热区）+ 大标题 + 副标题。
 * rightSlot 放右上角动作（如设备管理入口）。
 */
@Composable
fun FaFaPage(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    topBarRight: (@Composable () -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    GlassBackground(modifier = modifier) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                } else {
                    Box(Modifier.size(44.dp))
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    topBarRight?.invoke() ?: Box(Modifier.size(44.dp))
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Start,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                content = content,
            )
        }
    }
}

/** 品牌标：渐变圆 + 爪印（欢迎页/AUTH-05 用）。 */
@Composable
fun FaFaBrandMark(modifier: Modifier = Modifier, size: Int = 72) {
    Surface(
        modifier = modifier.size(size.dp),
        shape = CircleShape,
        color = Color.Transparent,
    ) {
        Box(
            Modifier
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Pets,
                contentDescription = "FaFa",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size((size * 0.45f).dp),
            )
        }
    }
}

/** 三段白色圆角分页线（AUTH-01）。 */
@Composable
fun FaFaPageIndicator(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(count) { index ->
            Box(
                Modifier
                    .width(if (index == current) 28.dp else 14.dp)
                    .height(6.dp)
                    .background(
                        if (index == current) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        },
                        RoundedCornerShape(3.dp),
                    ),
            )
        }
    }
}
