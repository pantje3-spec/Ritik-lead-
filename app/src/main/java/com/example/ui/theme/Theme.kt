package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SmmIndigo,
    onPrimary = Color.White,
    secondary = SmmTeal,
    onSecondary = Color.Black,
    tertiary = SmmGreen,
    background = SlateBgDark,
    surface = SlateSurfaceDark,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    surfaceVariant = SlateBorderDark,
    onSurfaceVariant = DarkTextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = SmmIndigo,
    onPrimary = Color.White,
    secondary = SmmTeal,
    onSecondary = Color.White,
    tertiary = SmmGreen,
    background = OffWhiteBg,
    surface = PureWhiteSurface,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    surfaceVariant = LightBorder,
    onSurfaceVariant = LightTextSecondary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Elite dark mode SaaS look by default
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
