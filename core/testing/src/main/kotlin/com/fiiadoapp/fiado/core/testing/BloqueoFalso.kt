package com.fiiadoapp.fiado.core.testing

import com.fiiadoapp.fiado.domain.RepositorioBloqueo
import kotlinx.coroutines.flow.MutableStateFlow

/** Preferencia del bloqueo en memoria. */
class BloqueoFalso : RepositorioBloqueo {
    val activo = MutableStateFlow(false)

    override fun observarActivo() = activo

    override suspend fun cambiar(activo: Boolean) {
        this.activo.value = activo
    }
}
