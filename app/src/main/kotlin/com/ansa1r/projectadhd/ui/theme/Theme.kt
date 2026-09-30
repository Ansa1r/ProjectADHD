package com.ansa1r.projectadhd.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val colors = darkColorScheme(
    primary = BrandColors.Primary, onPrimary = BrandColors.Background,
    primaryContainer = BrandColors.Green, onPrimaryContainer = BrandColors.Text,
    secondary = BrandColors.Secondary, secondaryContainer = BrandColors.Blue,
    onSecondaryContainer = BrandColors.Text, tertiary = BrandColors.Tertiary,
    tertiaryContainer = BrandColors.Purple, onTertiaryContainer = BrandColors.Background,
    background = BrandColors.Background, onBackground = BrandColors.Text,
    surface = BrandColors.Surface, onSurface = BrandColors.Text,
    surfaceVariant = BrandColors.SurfaceVariant, onSurfaceVariant = BrandColors.Muted
)

@Composable
fun ProjectADHDTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = Typography, content = content)
}
