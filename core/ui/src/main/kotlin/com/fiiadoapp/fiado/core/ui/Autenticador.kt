package com.fiiadoapp.fiado.core.ui

import androidx.compose.runtime.staticCompositionLocalOf

/** Pide la huella o el PIN del teléfono (ADR-0006). La actividad principal lo provee. */
interface Autenticador {
    /** `false` si el teléfono no tiene un bloqueo de pantalla configurado. */
    fun disponible(): Boolean

    fun autenticar(
        titulo: String,
        onExito: () -> Unit,
    )
}

val LocalAutenticador = staticCompositionLocalOf<Autenticador> { error("Falta el autenticador de la actividad") }
