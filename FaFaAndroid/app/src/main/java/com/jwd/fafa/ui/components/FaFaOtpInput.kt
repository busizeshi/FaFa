package com.jwd.fafa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 6 位验证码输入：单 BasicTextField 驱动 6 个玻璃圆角格子，数字键盘，自动聚焦。
 */
@Composable
fun FaFaOtpInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    digitCount: Int = 6,
    enabled: Boolean = true,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier = modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = { next ->
                val filtered = next.filter { it.isDigit() }.take(digitCount)
                if (filtered != value) onValueChange(filtered)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .focusRequester(focusRequester),
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = VisualTransformation.None,
            cursorBrush = SolidColor(androidx.compose.ui.graphics.Color.Transparent),
            decorationBox = { inner ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    repeat(digitCount) { index ->
                        val char = value.getOrNull(index)
                        val isCurrent = index == value.length && enabled
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                                    RoundedCornerShape(16.dp),
                                )
                                .border(
                                    1.dp,
                                    when {
                                        isCurrent -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                                    },
                                    RoundedCornerShape(16.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = char?.toString() ?: "",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                inner()
            },
        )
    }
}
