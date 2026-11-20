package com.fiiadoapp.fiado.core.data.recordatorio

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fiiadoapp.fiado.domain.PlantillaRecordatorio
import com.fiiadoapp.fiado.domain.Resultado
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.test.assertEquals

/** Con Robolectric, como las demás pruebas de DataStore: en Windows, fuera de él, renombrar el archivo falla. */
@RunWith(RobolectricTestRunner::class)
class PlantillaDataStoreTest {
    @get:Rule
    val carpeta = TemporaryFolder()

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dataStore by lazy {
        PreferenceDataStoreFactory.create(scope = alcance) { File(carpeta.root, "preferencias.preferences_pb") }
    }
    private val repositorio by lazy { PlantillaDataStore(dataStore) }

    @After
    fun detener() = alcance.cancel()

    private fun plantilla(texto: String) = (PlantillaRecordatorio.desdeTexto(texto) as Resultado.Exito).valor

    @Test
    fun `sin nada guardado usa la plantilla por defecto`() =
        runBlocking {
            assertEquals(PlantillaRecordatorio.POR_DEFECTO, repositorio.observar().first())
        }

    @Test
    fun `guarda la plantilla propia y la restablece`() =
        runBlocking {
            val propia = plantilla("{cliente}, debes {saldo}.")

            repositorio.guardar(propia)
            assertEquals(propia, repositorio.observar().first())

            repositorio.restablecer()
            assertEquals(PlantillaRecordatorio.POR_DEFECTO, repositorio.observar().first())
        }

    @Test
    fun `una plantilla guardada que ya no es valida vuelve a la de por defecto`() =
        runBlocking {
            dataStore.edit { it[stringPreferencesKey("plantilla_recordatorio")] = "sin monto" }

            assertEquals(PlantillaRecordatorio.POR_DEFECTO, repositorio.observar().first())
        }
}
