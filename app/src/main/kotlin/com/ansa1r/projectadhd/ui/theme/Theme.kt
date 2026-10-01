package com.ansa1r.projectadhd.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

private val colors = darkColorScheme(
    primary = BrandColors.Primary, onPrimary = BrandColors.Background,
    primaryContainer = BrandColors.PurpleSurface, onPrimaryContainer = BrandColors.Text,
    secondary = BrandColors.Secondary, onSecondary = BrandColors.Background, secondaryContainer = BrandColors.Green,
    onSecondaryContainer = BrandColors.Text, tertiary = BrandColors.Tertiary,
    tertiaryContainer = BrandColors.Blue, onTertiaryContainer = BrandColors.Text,
    background = BrandColors.Background, onBackground = BrandColors.Text,
    surface = BrandColors.Surface, onSurface = BrandColors.Text,
    surfaceVariant = BrandColors.SurfaceVariant, onSurfaceVariant = BrandColors.Muted,
    outline = BrandColors.Primary, outlineVariant = BrandColors.PurpleOutline,
    error = BrandColors.Error
)

@Composable
fun ProjectADHDTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = Typography,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp), extraLarge = RoundedCornerShape(32.dp)), content = content)
}
