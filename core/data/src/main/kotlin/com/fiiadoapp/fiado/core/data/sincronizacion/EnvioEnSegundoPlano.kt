package com.fiiadoapp.fiado.core.data.sincronizacion

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Pide subir la cola en cuanto haya red. Se llama después de cada cambio y al abrir la app. */
fun interface ProgramadorDeEnvio {
    fun programar()
}

/** Sube la cola con WorkManager: sobrevive a que se cierre la app y espera a tener conexión. */
class ProgramadorWorkManager
    @Inject
    constructor(
        @param:ApplicationContext private val contexto: Context,
    ) : ProgramadorDeEnvio {
        /**
         * Reemplaza el envío programado en lugar de encadenar otro: sin red, mil cambios dejan un solo
         * envío esperando. Si reemplaza uno en curso, lo ya aplicado se confirma al reenviarse.
         */
        override fun programar() {
            val envio =
                OneTimeWorkRequestBuilder<EnvioDePendientesWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, ESPERA_INICIAL_S, TimeUnit.SECONDS)
                    .build()
            WorkManager.getInstance(contexto).enqueueUniqueWork(NOMBRE, ExistingWorkPolicy.REPLACE, envio)
        }

        companion object {
            const val NOMBRE = "envio-de-pendientes"
            private const val ESPERA_INICIAL_S = 30L
        }
    }

@HiltWorker
class EnvioDePendientesWorker
    @AssistedInject
    constructor(
        @Assisted contexto: Context,
        @Assisted parametros: WorkerParameters,
        private val envio: EnvioDePendientes,
    ) : CoroutineWorker(contexto, parametros) {
        override suspend fun doWork(): Result =
            when (envio.enviar()) {
                ResultadoEnvio.COMPLETO, ResultadoEnvio.SIN_SESION -> Result.success()
                ResultadoEnvio.REINTENTAR -> Result.retry()
                ResultadoEnvio.RECHAZADO -> Result.failure()
            }
    }
