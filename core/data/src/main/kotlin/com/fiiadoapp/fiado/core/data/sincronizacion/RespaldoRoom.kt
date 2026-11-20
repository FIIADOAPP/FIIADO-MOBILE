package com.fiiadoapp.fiado.core.data.sincronizacion

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.fiiadoapp.fiado.domain.EstadoRespaldo
import com.fiiadoapp.fiado.domain.RepositorioRespaldo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Los pendientes salen de la cola; el envío está detenido si el último trabajo terminó rechazado. */
class RespaldoRoom
    @Inject
    constructor(
        private val cola: ColaDao,
        @param:ApplicationContext private val contexto: Context,
    ) : RepositorioRespaldo {
        override fun observar(): Flow<EstadoRespaldo> {
            val detenido =
                WorkManager
                    .getInstance(contexto)
                    .getWorkInfosForUniqueWorkFlow(ProgramadorWorkManager.NOMBRE)
                    .map { trabajos -> trabajos.any { it.state == WorkInfo.State.FAILED } }
            return combine(cola.observarCantidad(), detenido) { pendientes, rechazado ->
                EstadoRespaldo(pendientes, rechazado)
            }.distinctUntilChanged()
        }
    }
