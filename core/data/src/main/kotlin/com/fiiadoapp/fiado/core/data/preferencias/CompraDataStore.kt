package com.fiiadoapp.fiado.core.data.preferencias

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.fiiadoapp.fiado.domain.RepositorioCompra
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Singleton

/** Último estado conocido de la compra «Quitar anuncios» (ADR-0011); Google Play manda cuando responde. */
class CompraDataStore(
    private val dataStore: DataStore<Preferences>,
) : RepositorioCompra {
    override fun observarAnunciosQuitados(): Flow<Boolean> = dataStore.data.map { it[QUITADOS] ?: false }

    override suspend fun guardarAnunciosQuitados(quitados: Boolean) {
        dataStore.edit { it[QUITADOS] = quitados }
    }

    private companion object {
        val QUITADOS = booleanPreferencesKey("anuncios_quitados")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object CompraModule {
    @Provides
    @Singleton
    fun repositorioCompra(
        @Preferencias dataStore: DataStore<Preferences>,
    ): RepositorioCompra = CompraDataStore(dataStore)
}
