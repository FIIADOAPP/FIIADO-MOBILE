package com.fiiadoapp.fiado.core.data.recordatorio

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fiiadoapp.fiado.core.data.preferencias.Preferencias
import com.fiiadoapp.fiado.domain.PlantillaRecordatorio
import com.fiiadoapp.fiado.domain.RepositorioPlantillaRecordatorio
import com.fiiadoapp.fiado.domain.Resultado
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Singleton

/** Sin plantilla guardada, o si la guardada ya no es válida, se usa la de por defecto. */
class PlantillaDataStore(
    private val dataStore: DataStore<Preferences>,
) : RepositorioPlantillaRecordatorio {
    override fun observar(): Flow<PlantillaRecordatorio> =
        dataStore.data.map { preferencias ->
            preferencias[PLANTILLA]
                ?.let { (PlantillaRecordatorio.desdeTexto(it) as? Resultado.Exito)?.valor }
                ?: PlantillaRecordatorio.POR_DEFECTO
        }

    override suspend fun guardar(plantilla: PlantillaRecordatorio) {
        dataStore.edit { it[PLANTILLA] = plantilla.texto }
    }

    override suspend fun restablecer() {
        dataStore.edit { it.remove(PLANTILLA) }
    }

    private companion object {
        val PLANTILLA = stringPreferencesKey("plantilla_recordatorio")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object RecordatorioModule {
    @Provides
    @Singleton
    fun repositorioPlantilla(
        @Preferencias dataStore: DataStore<Preferences>,
    ): RepositorioPlantillaRecordatorio = PlantillaDataStore(dataStore)
}
