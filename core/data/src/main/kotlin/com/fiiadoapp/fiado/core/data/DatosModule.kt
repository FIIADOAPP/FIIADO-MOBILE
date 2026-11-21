package com.fiiadoapp.fiado.core.data

import android.content.Context
import com.fiiadoapp.fiado.core.data.sincronizacion.ProgramadorDeEnvio
import com.fiiadoapp.fiado.domain.GeneradorDeIds
import com.fiiadoapp.fiado.domain.RepositorioClientes
import com.fiiadoapp.fiado.domain.RepositorioMetodosCobro
import com.fiiadoapp.fiado.domain.RepositorioMovimientos
import com.fiiadoapp.fiado.domain.RepositorioProrrogas
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatosModule {
    @Provides
    @Singleton
    fun baseDeDatos(
        @ApplicationContext contexto: Context,
    ): FiadoDatabase = FiadoDatabase.construir(contexto)

    @Provides
    fun repositorioClientes(
        db: FiadoDatabase,
        ids: GeneradorDeIds,
        programador: ProgramadorDeEnvio,
    ): RepositorioClientes = RepositorioClientesRoom(db, ids, programador)

    @Provides
    fun repositorioMovimientos(
        db: FiadoDatabase,
        ids: GeneradorDeIds,
        programador: ProgramadorDeEnvio,
    ): RepositorioMovimientos = RepositorioMovimientosRoom(db, ids, programador)

    @Provides
    fun repositorioMetodosCobro(
        db: FiadoDatabase,
        ids: GeneradorDeIds,
        programador: ProgramadorDeEnvio,
    ): RepositorioMetodosCobro = RepositorioMetodosCobroRoom(db, ids, programador)

    @Provides
    fun repositorioProrrogas(
        db: FiadoDatabase,
        ids: GeneradorDeIds,
        programador: ProgramadorDeEnvio,
    ): RepositorioProrrogas = RepositorioProrrogasRoom(db, ids, programador)

    @Provides
    fun generadorDeIds(): GeneradorDeIds = GeneradorUuidV7()
}
