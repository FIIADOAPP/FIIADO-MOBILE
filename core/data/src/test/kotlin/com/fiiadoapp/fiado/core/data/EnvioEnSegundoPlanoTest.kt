package com.fiiadoapp.fiado.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.fiiadoapp.fiado.core.data.sincronizacion.EnvioDePendientesWorker
import com.fiiadoapp.fiado.core.data.sincronizacion.ProgramadorWorkManager
import com.fiiadoapp.fiado.core.data.sincronizacion.ResultadoEnvio
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class EnvioEnSegundoPlanoTest {
    private val contexto: Context = ApplicationProvider.getApplicationContext()

    private fun trabajoQueDevuelve(resultado: ResultadoEnvio): EnvioDePendientesWorker =
        TestListenableWorkerBuilder<EnvioDePendientesWorker>(contexto)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ) = EnvioDePendientesWorker(appContext, workerParameters) { resultado }
                },
            ).build()

    @Test
    fun `el trabajo termina, reintenta o falla segun el envio`() =
        runBlocking {
            val esperados =
                mapOf(
                    ResultadoEnvio.COMPLETO to ListenableWorker.Result.success(),
                    ResultadoEnvio.SIN_SESION to ListenableWorker.Result.success(),
                    ResultadoEnvio.REINTENTAR to ListenableWorker.Result.retry(),
                    ResultadoEnvio.RECHAZADO to ListenableWorker.Result.failure(),
                )

            esperados.forEach { (envio, trabajo) -> assertEquals(trabajo, trabajoQueDevuelve(envio).doWork()) }
        }

    @Test
    fun `programar varias veces deja un solo envio esperando red`() {
        WorkManagerTestInitHelper.initializeTestWorkManager(contexto, Configuration.Builder().build())
        val programador = ProgramadorWorkManager(contexto)

        repeat(3) { programador.programar() }

        val envios = WorkManager.getInstance(contexto).getWorkInfosForUniqueWork(ProgramadorWorkManager.NOMBRE).get()
        val esperando = envios.filter { it.state == WorkInfo.State.ENQUEUED }
        assertEquals(1, esperando.size)
        assertEquals(NetworkType.CONNECTED, esperando.single().constraints.requiredNetworkType)
    }
}
