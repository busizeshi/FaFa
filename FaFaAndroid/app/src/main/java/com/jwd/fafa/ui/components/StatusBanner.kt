package com.jwd.fafa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class BannerKind { ERROR, INFO, WARN, SUCCESS }

/**
 * 状态横幅：错误 / 信息 / 限流提醒 / 成功。柔和色底 + 图标 + 一句原因。
 * 对应规格 C15（InlineError）：恢复动作由 [FaFaBannerWithAction] 提供。
 */
@Composable
fun FaFaBanner(
    text: String,
    kind: BannerKind,
    modifier: Modifier = Modifier,
) {
    val (container, contentColor, icon) = bannerColors(kind)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
            modifier = Modifier
                .padding(start = 10.dp)
                .weight(1f),
        )
    }
}

/** 带恢复动作的横幅（如"重试"、验证码重发倒计时说明）。 */
@Composable
fun FaFaBannerWithAction(
    text: String,
    kind: BannerKind,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionEnabled: Boolean = true,
) {
    val (container, _, _) = bannerColors(kind)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        FaFaTextButton(
            text = actionText,
            onClick = onAction,
            enabled = actionEnabled,
        )
    }
}

private data class BannerStyle(
    val container: androidx.compose.ui.graphics.Color,
    val content: androidx.compose.ui.graphics.Color,
    val icon: ImageVector,
)

@Composable
private fun bannerColors(kind: BannerKind): BannerStyle = when (kind) {
    BannerKind.ERROR -> BannerStyle(
        MaterialTheme.colorScheme.errorContainer,
        MaterialTheme.colorScheme.onErrorContainer,
        Icons.Rounded.ErrorOutline,
    )
    BannerKind.INFO -> BannerStyle(
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.onSecondaryContainer,
        Icons.Rounded.Info,
    )
    BannerKind.WARN -> BannerStyle(
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.onTertiaryContainer,
        Icons.Rounded.WarningAmber,
    )
    BannerKind.SUCCESS -> BannerStyle(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        MaterialTheme.colorScheme.onPrimaryContainer,
        Icons.Rounded.CheckCircle,
    )
}
