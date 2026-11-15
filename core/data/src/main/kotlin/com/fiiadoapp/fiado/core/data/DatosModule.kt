package com.fiiadoapp.fiado.core.data

import android.content.Context
import androidx.room.Room
import com.fiiadoapp.fiado.domain.GeneradorDeIds
import com.fiiadoapp.fiado.domain.RepositorioClientes
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
    ): FiadoDatabase = Room.databaseBuilder(contexto, FiadoDatabase::class.java, "fiado.db").build()

    @Provides
    fun repositorioClientes(db: FiadoDatabase): RepositorioClientes = RepositorioClientesRoom(db.clienteDao())

    @Provides
    fun repositorioMovimientos(db: FiadoDatabase): RepositorioMovimientos = RepositorioMovimientosRoom(db.movimientoDao())

    @Provides
    fun generadorDeIds(): GeneradorDeIds = GeneradorUuidV7()
}
