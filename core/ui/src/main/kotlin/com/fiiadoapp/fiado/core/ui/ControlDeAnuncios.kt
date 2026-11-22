package com.fiiadoapp.fiado.core.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.fiiadoapp.fiado.domain.EstadoQuitarAnuncios
import kotlinx.coroutines.flow.StateFlow

/** La compra que quita los anuncios y las opciones de privacidad (ADR-0011). La actividad principal lo provee. */
interface ControlDeAnuncios {
    val estado: StateFlow<EstadoQuitarAnuncios>

    /** `true` donde la ley exige poder cambiar el consentimiento de anuncios después. */
    val opcionesDePrivacidadRequeridas: StateFlow<Boolean>

    fun comprar()

    /** Vuelve a consultar a Google Play, por ejemplo tras reinstalar. */
    fun restaurar()

    fun abrirOpcionesDePrivacidad()
}

val LocalControlDeAnuncios = staticCompositionLocalOf<ControlDeAnuncios> { error("Falta el control de anuncios de la actividad") }
