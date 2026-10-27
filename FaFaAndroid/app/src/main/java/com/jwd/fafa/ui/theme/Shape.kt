package com.jwd.fafa.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 苹果圆风圆角体系（Pen 令牌：radius-card 28 / button 20 / group 16）。
 */
val FaFaShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

object FaFaRadius {
    val Input = 18.dp
    val Button = 20.dp
    val Card = 24.dp
    val CardLarge = 28.dp
    val Sheet = 32.dp
}
