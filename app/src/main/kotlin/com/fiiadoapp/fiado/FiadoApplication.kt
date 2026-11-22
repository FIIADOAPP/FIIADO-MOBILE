package com.fiiadoapp.fiado

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.fiiadoapp.fiado.core.data.sincronizacion.ModoSinCuenta
import com.fiiadoapp.fiado.core.data.sincronizacion.ProgramadorDeEnvio
import com.fiiadoapp.fiado.domain.Funciones
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FiadoApplication :
    Application(),
    Configuration.Provider {
    @Inject lateinit var fabricaDeTrabajos: HiltWorkerFactory

    @Inject lateinit var programador: ProgramadorDeEnvio

    @Inject lateinit var funciones: Funciones

    @Inject lateinit var modoSinCuenta: ModoSinCuenta

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(fabricaDeTrabajos).build()

    override fun onCreate() {
        super.onCreate()
        if (funciones.cuentaDisponible) {
            // Lo que quedó pendiente la última vez (sin red, app cerrada) se sube en cuanto haya conexión.
            programador.programar()
        } else {
            MainScope().launch { modoSinCuenta.vaciarColaSinVinculo() }
        }
    }
}
