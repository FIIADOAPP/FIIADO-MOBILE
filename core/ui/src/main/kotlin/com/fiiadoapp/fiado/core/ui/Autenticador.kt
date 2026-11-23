package com.fiiadoapp.fiado.core.ui

import androidx.compose.runtime.staticCompositionLocalOf

/** Pide la huella o el PIN del teléfono (ADR-0006). La actividad principal lo provee. */
interface Autenticador {
    /** `false` si ahora no se puede pedir la huella o el PIN, por cualquier motivo. */
    fun disponible(): Boolean

    /**
     * `true` solo si el teléfono no tiene ningún bloqueo de pantalla configurado: el único caso en que se apaga
     * el bloqueo de la app. Un fallo pasajero del sensor no lo apaga; la libreta sigue bloqueada.
     */
    fun sinBloqueoDePantalla(): Boolean

    fun autenticar(
        titulo: String,
        onExito: () -> Unit,
    )
}

val LocalAutenticador = staticCompositionLocalOf<Autenticador> { error("Falta el autenticador de la actividad") }
