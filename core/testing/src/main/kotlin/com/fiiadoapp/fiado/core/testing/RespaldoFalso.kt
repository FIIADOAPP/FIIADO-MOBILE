package com.fiiadoapp.fiado.core.testing

import com.fiiadoapp.fiado.domain.EstadoRespaldo
import com.fiiadoapp.fiado.domain.RepositorioRespaldo
import kotlinx.coroutines.flow.MutableStateFlow

/** Estado del respaldo que la prueba fija a mano. */
class RespaldoFalso : RepositorioRespaldo {
    val estado = MutableStateFlow(EstadoRespaldo(pendientes = 0, detenido = false))

    override fun observar() = estado
}
