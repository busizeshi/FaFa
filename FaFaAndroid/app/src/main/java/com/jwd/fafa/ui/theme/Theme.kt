package com.jwd.fafa.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = RoseBrand,
    onPrimary = RoseWhite,
    primaryContainer = RosePink,
    onPrimaryContainer = RosePrimaryContainerInk,
    secondary = RoseSecondaryGrey,
    onSecondary = RoseWhite,
    secondaryContainer = RoseBlue,
    onSecondaryContainer = RoseInk,
    tertiary = RoseAttention,
    onTertiary = RoseWhite,
    tertiaryContainer = RoseYellow,
    onTertiaryContainer = RoseAttention,
    background = RoseBg,
    onBackground = RoseInk,
    surface = RoseWhite,
    onSurface = RoseInk,
    surfaceVariant = RoseSoft,
    onSurfaceVariant = RoseMuted,
    outline = RoseLine,
    outlineVariant = RoseLine,
    error = RoseDanger,
    onError = RoseWhite,
    errorContainer = RoseDangerSoft,
    onErrorContainer = RoseErrorInk,
)

private val DarkColors = darkColorScheme(
    primary = RoseBrandDark,
    onPrimary = RoseBrandDarkOn,
    primaryContainer = RosePrimaryContainerInk,
    onPrimaryContainer = RosePink,
    secondary = RoseMutedDark,
    onSecondary = RoseBrandDarkOn,
    secondaryContainer = RoseSurface2Dark,
    onSecondaryContainer = RoseTextDark,
    tertiary = RoseAttentionDark,
    onTertiary = RoseTertiaryDarkOn,
    tertiaryContainer = RoseTertiaryDarkContainer,
    onTertiaryContainer = RoseAttentionDark,
    background = RoseBgDark,
    onBackground = RoseTextDark,
    surface = RoseSurfaceDark,
    onSurface = RoseTextDark,
    surfaceVariant = RoseSurface2Dark,
    onSurfaceVariant = RoseMutedDark,
    outline = RoseLineDark,
    outlineVariant = RoseLineDark,
    error = RoseDangerDark,
    onError = RoseErrorDarkOn,
    errorContainer = RoseErrorDarkContainer,
    onErrorContainer = RoseDangerDark,
)

/** FaFa 主题：soft26-rose 双色方案；不启用 Material You 动态取色（品牌色优先）。 */
@Composable
fun FaFaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = FaFaTypography,
        shapes = FaFaShapes,
        content = content,
    )
}
