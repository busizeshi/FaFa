package com.jwd.fafa.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

/**
 * 协议同意行：默认未勾选；《用户协议》《隐私政策》可点进只读查看页（AUTH-02）。
 * 协议未同意不得提交（手册 M00 自测）。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConsentRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onOpenDocument: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(32.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
        FlowRow(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        ) {
            Text(
                text = "我已阅读并同意",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "《用户协议》",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .clickable { onOpenDocument("USER_AGREEMENT") },
            )
            Text(
                text = "和",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "《隐私政策》",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .clickable { onOpenDocument("PRIVACY_POLICY") },
            )
        }
    }
}
