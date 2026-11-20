package com.fiiadoapp.fiado.core.data.sincronizacion

import com.fiiadoapp.fiado.core.data.FiadoDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
abstract class SincronizacionModule {
    @Binds
    abstract fun envioDePendientes(sincronizador: Sincronizador): EnvioDePendientes

    @Binds
    abstract fun programadorDeEnvio(programador: ProgramadorWorkManager): ProgramadorDeEnvio

    @Binds
    abstract fun vinculacion(restaurador: Restaurador): Vinculacion

    companion object {
        @Provides
        fun colaDao(db: FiadoDatabase): ColaDao = db.colaDao()

        @Provides
        fun apiSincronizacion(retrofit: Retrofit): ApiSincronizacion = retrofit.create(ApiSincronizacion::class.java)
    }
}
