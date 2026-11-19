package com.vortix.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VortixDarkColorScheme = darkColorScheme(
    primary = VortixPrimary,
    onPrimary = VortixBackground,
    secondary = VortixSecondary,
    onSecondary = VortixBackground,
    tertiary = VortixTertiary,
    onTertiary = VortixBackground,
    error = VortixError,
    background = VortixBackground,
    onBackground = VortixOnBackground,
    surface = VortixSurface,
    onSurface = VortixOnBackground,
    surfaceVariant = VortixSurfaceVariant,
    onSurfaceVariant = VortixOnSurfaceVariant,
    outline = VortixOutline
)

@Composable
fun VortixTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VortixDarkColorScheme,
        content = content
    )
}
