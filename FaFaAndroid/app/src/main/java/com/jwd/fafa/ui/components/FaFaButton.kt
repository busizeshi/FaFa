package com.jwd.fafa.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jwd.fafa.ui.theme.FaFaRadius

/** 主按钮：每屏一个；submitting 时禁连点并显示状态。高度 52，圆角 20。 */
@Composable
fun FaFaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    submitting: Boolean = false,
    submittingText: String? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        enabled = enabled && !submitting,
        shape = RoundedCornerShape(FaFaRadius.Button),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (submitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                if (submittingText != null) {
                    Text(
                        text = " $submittingText",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            } else {
                Text(text, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** 次按钮：玻璃底、描边。 */
@Composable
fun FaFaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(FaFaRadius.Button),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
        ),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** 文字按钮（次级动作：忘记密码、修改邮箱等）。 */
@Composable
fun FaFaTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    TextButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}
