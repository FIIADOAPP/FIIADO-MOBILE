package com.fiiadoapp.fiado.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VerdeFiado = Color(0xFF1B6B3A)
private val VerdeFiadoClaro = Color(0xFF8BD8A1)

private val EsquemaClaro = lightColorScheme(primary = VerdeFiado)
private val EsquemaOscuro = darkColorScheme(primary = VerdeFiadoClaro)

@Composable
fun FiadoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) EsquemaOscuro else EsquemaClaro,
        content = content,
    )
}
