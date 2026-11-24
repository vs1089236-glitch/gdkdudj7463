package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

private val LightColors = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = DeepTeal,
    secondary = GoldAccent,
    onSecondary = Color.White,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = DeepTeal,
    onTertiary = Color.White,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = CardLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = Color(0xFFBFD1CD)
)

private val DarkColors = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = DeepTealDark,
    primaryContainer = TealContainerDark,
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = GoldAccentDark,
    onSecondary = Color(0xFF3F2000),
    secondaryContainer = GoldContainerDark,
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = Color(0xFF2DD4BF),
    onTertiary = DeepTealDark,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = CardDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = Color(0xFF264D4F)
)

val HoyoShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun HoyoPayTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = HoyoTypography,
        shapes = HoyoShapes,
        content = content
    )
}
