package com.fiiadoapp.fiado.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.fiiadoapp.fiado.core.data.sincronizacion.EnvioDePendientesWorker
import com.fiiadoapp.fiado.core.data.sincronizacion.ProgramadorWorkManager
import com.fiiadoapp.fiado.core.data.sincronizacion.RespaldoRoom
import com.fiiadoapp.fiado.core.data.sincronizacion.ResultadoEnvio
import com.fiiadoapp.fiado.domain.EstadoRespaldo
import com.fiiadoapp.fiado.domain.TipoMovimiento.FIADO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class RespaldoRoomTest : BaseDeDatosDePrueba() {
    private val contexto: Context = ApplicationProvider.getApplicationContext()

    // Cada envío en segundo plano termina como si el servidor hubiera rechazado un lote.
    init {
        val rechaza =
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ) = EnvioDePendientesWorker(appContext, workerParameters) { ResultadoEnvio.RECHAZADO }
            }
        WorkManagerTestInitHelper.initializeTestWorkManager(
            contexto,
            Configuration
                .Builder()
                .setWorkerFactory(rechaza)
                .setExecutor(SynchronousExecutor())
                .build(),
        )
    }

    private val respaldo = RespaldoRoom(db.colaDao(), contexto)

    @Test
    fun `cuenta los cambios por subir`() =
        runBlocking {
            assertEquals(EstadoRespaldo(pendientes = 0, detenido = false), respaldo.observar().first())

            val rosa = cliente("Doña Rosa")
            clientes.guardar(rosa)
            movimientos.guardar(movimiento(rosa, FIADO, 100))

            assertEquals(EstadoRespaldo(pendientes = 2, detenido = false), respaldo.observar().first())
        }

    @Test
    fun `un envio rechazado deja el respaldo detenido`() =
        runBlocking {
            ProgramadorWorkManager(contexto).programar()
            val trabajo =
                WorkManager
                    .getInstance(contexto)
                    .getWorkInfosForUniqueWork(ProgramadorWorkManager.NOMBRE)
                    .get()
                    .single()
            WorkManagerTestInitHelper.getTestDriver(contexto)!!.setAllConstraintsMet(trabajo.id)

            assertEquals(true, respaldo.observar().first { it.detenido }.detenido)
        }
}
