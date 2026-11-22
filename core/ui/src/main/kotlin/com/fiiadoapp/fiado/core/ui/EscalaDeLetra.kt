package com.fiiadoapp.fiado.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Pone un tope al agrandado de letra del teléfono dentro de [contenido]. Solo para piezas de tamaño fijo
 * (las iniciales del avatar, la etiqueta del anuncio): el resto de la app respeta
 * siempre el tamaño de letra que eligió la persona.
 */
@Composable
fun ConEscalaDeLetraMaxima(
    maxima: Float,
    contenido: @Composable () -> Unit,
) {
    val densidad = LocalDensity.current
    if (densidad.fontScale <= maxima) {
        contenido()
    } else {
        CompositionLocalProvider(LocalDensity provides Density(densidad.density, maxima), content = contenido)
    }
}
