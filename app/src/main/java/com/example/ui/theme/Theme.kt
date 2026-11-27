package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val KiranaLightColorScheme = lightColorScheme(
    primary = KiranaGreen,
    onPrimary = KiranaSurfaceWhite,
    primaryContainer = KiranaGreenLight,
    onPrimaryContainer = KiranaGreenDark,
    secondary = KiranaAmber,
    onSecondary = KiranaSurfaceWhite,
    secondaryContainer = KiranaAmberLight,
    onSecondaryContainer = KiranaTextPrimary,
    background = KiranaSurfaceWhite,
    onBackground = KiranaTextPrimary,
    surface = KiranaSurfaceWhite,
    onSurface = KiranaTextPrimary,
    surfaceVariant = KiranaSurfaceTint,
    onSurfaceVariant = KiranaTextSecondary,
    outline = KiranaOutline,
    error = KiranaRed,
    onError = KiranaSurfaceWhite,
    errorContainer = KiranaRedLight,
    onErrorContainer = KiranaRed
)

private val KiranaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun GraminKiranaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KiranaLightColorScheme,
        typography = Typography,
        shapes = KiranaShapes,
        content = content
    )
}
