package com.fiiadoapp.fiado.core.data

import android.content.Context
import com.fiiadoapp.fiado.domain.GeneradorDeIds
import com.fiiadoapp.fiado.domain.RepositorioClientes
import com.fiiadoapp.fiado.domain.RepositorioMetodosCobro
import com.fiiadoapp.fiado.domain.RepositorioMovimientos
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
    ): RepositorioClientes = RepositorioClientesRoom(db, ids)

    @Provides
    fun repositorioMovimientos(
        db: FiadoDatabase,
        ids: GeneradorDeIds,
    ): RepositorioMovimientos = RepositorioMovimientosRoom(db, ids)

    @Provides
    fun repositorioMetodosCobro(
        db: FiadoDatabase,
        ids: GeneradorDeIds,
    ): RepositorioMetodosCobro = RepositorioMetodosCobroRoom(db, ids)

    @Provides
    fun generadorDeIds(): GeneradorDeIds = GeneradorUuidV7()
}
