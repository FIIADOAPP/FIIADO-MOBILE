package com.fiiadoapp.fiado.core.data.preferencias

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.fiiadoapp.fiado.domain.RepositorioBloqueo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Singleton

/** El bloqueo viene apagado (ADR-0006). */
class BloqueoDataStore(
    private val dataStore: DataStore<Preferences>,
) : RepositorioBloqueo {
    override fun observarActivo(): Flow<Boolean> = dataStore.data.map { it[ACTIVO] ?: false }

    override suspend fun cambiar(activo: Boolean) {
        dataStore.edit { it[ACTIVO] = activo }
    }

    private companion object {
        val ACTIVO = booleanPreferencesKey("bloqueo_activo")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object BloqueoModule {
    @Provides
    @Singleton
    fun repositorioBloqueo(
        @Preferencias dataStore: DataStore<Preferences>,
    ): RepositorioBloqueo = BloqueoDataStore(dataStore)
}
