package com.fiiadoapp.fiado.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Paleta verde completa, para que ningún rol quede con el color por defecto de Material. */
private val EsquemaClaro =
    lightColorScheme(
        primary = Color(0xFF1B6B3A),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFA4F4BB),
        onPrimaryContainer = Color(0xFF00210C),
        secondary = Color(0xFF4F6353),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD2E8D4),
        onSecondaryContainer = Color(0xFF0D1F12),
        background = Color(0xFFF6FBF3),
        onBackground = Color(0xFF181D18),
        surface = Color(0xFFF6FBF3),
        onSurface = Color(0xFF181D18),
        surfaceVariant = Color(0xFFDDE5DA),
        onSurfaceVariant = Color(0xFF414941),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF0F5ED),
        surfaceContainer = Color(0xFFEAEFE7),
        surfaceContainerHigh = Color(0xFFE5EAE2),
        surfaceContainerHighest = Color(0xFFDFE4DC),
        outline = Color(0xFF717970),
        outlineVariant = Color(0xFFC1C9BF),
    )

private val EsquemaOscuro =
    darkColorScheme(
        primary = Color(0xFF89D7A0),
        onPrimary = Color(0xFF00391A),
        primaryContainer = Color(0xFF005228),
        onPrimaryContainer = Color(0xFFA4F4BB),
        secondary = Color(0xFFB6CCB8),
        onSecondary = Color(0xFF223527),
        secondaryContainer = Color(0xFF384B3C),
        onSecondaryContainer = Color(0xFFD2E8D4),
        background = Color(0xFF0F150F),
        onBackground = Color(0xFFDFE4DC),
        surface = Color(0xFF0F150F),
        onSurface = Color(0xFFDFE4DC),
        surfaceVariant = Color(0xFF414941),
        onSurfaceVariant = Color(0xFFC1C9BF),
        surfaceContainerLowest = Color(0xFF0A0F0A),
        surfaceContainerLow = Color(0xFF181D18),
        surfaceContainer = Color(0xFF1C211C),
        surfaceContainerHigh = Color(0xFF262B26),
        surfaceContainerHighest = Color(0xFF313631),
        outline = Color(0xFF8B9389),
        outlineVariant = Color(0xFF414941),
    )

@Composable
fun FiadoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) EsquemaOscuro else EsquemaClaro,
        content = content,
    )
}
