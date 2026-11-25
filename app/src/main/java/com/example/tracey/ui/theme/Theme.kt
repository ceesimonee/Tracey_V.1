package com.example.tracey.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PersianGreyColorScheme = lightColorScheme(
    primary = CharcoalPrimary,
    onPrimary = Color.White,
    primaryContainer = PersianCoatGrey,
    onPrimaryContainer = DeepGraphite,
    secondary = MutedFurGrey,
    onSecondary = Color.White,
    secondaryContainer = PersianCoatGrey,
    onSecondaryContainer = DeepGraphite,
    background = PersianMistBackground,
    onBackground = DeepGraphite,
    surface = PureWhiteSurface,
    onSurface = DeepGraphite,
    surfaceVariant = PersianCoatGrey,
    onSurfaceVariant = MutedFurGrey,
    outline = SilverBorder,
    error = SoftRoseDelete
)

@Composable
fun TraceyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PersianGreyColorScheme,
        typography = Typography,
        content = content
    )
}