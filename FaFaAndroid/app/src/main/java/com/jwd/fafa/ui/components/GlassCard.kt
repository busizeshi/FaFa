package com.jwd.fafa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jwd.fafa.ui.theme.FaFaRadius
import com.jwd.fafa.ui.theme.GlassSheen

/**
 * 玻璃拟态卡片：半透明表面 + 顶部高光渐变 + 发丝描边（苹果圆风，radius 24）。
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(FaFaRadius.Card)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(containerColor)
            .background(Brush.verticalGradient(listOf(GlassSheen, Color.Transparent)))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                shape = shape,
            ),
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}
