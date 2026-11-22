package com.fiiadoapp.fiado.core.data.sincronizacion

import com.fiiadoapp.fiado.core.data.BuildConfig
import com.fiiadoapp.fiado.core.data.FiadoDatabase
import com.fiiadoapp.fiado.domain.Funciones
import com.fiiadoapp.fiado.domain.RepositorioRespaldo
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SincronizacionModule {
    @Binds
    abstract fun envioDePendientes(sincronizador: Sincronizador): EnvioDePendientes

    @Binds
    abstract fun vinculacion(restaurador: Restaurador): Vinculacion

    @Binds
    abstract fun repositorioRespaldo(respaldo: RespaldoRoom): RepositorioRespaldo

    companion object {
        @Provides
        fun colaDao(db: FiadoDatabase): ColaDao = db.colaDao()

        @Provides
        fun restauracionDao(db: FiadoDatabase): RestauracionDao = db.restauracionDao()

        @Provides
        @Singleton
        fun funciones(): Funciones = Funciones(cuentaDisponible = BuildConfig.CUENTA_DISPONIBLE)

        /** Sin cuenta no hay a dónde subir: no se programa ningún envío (ADR-0010). */
        @Provides
        fun programadorDeEnvio(
            funciones: Funciones,
            workManager: ProgramadorWorkManager,
        ): ProgramadorDeEnvio = if (funciones.cuentaDisponible) workManager else ProgramadorDeEnvio {}

        @Provides
        fun apiSincronizacion(retrofit: Retrofit): ApiSincronizacion = retrofit.create(ApiSincronizacion::class.java)
    }
}
