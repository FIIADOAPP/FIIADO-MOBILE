package com.fiiadoapp.fiado.core.data.preferencias

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Con Robolectric, como las demás pruebas de DataStore: en Windows, fuera de él, renombrar el archivo falla. */
@RunWith(RobolectricTestRunner::class)
class CompraDataStoreTest {
    @get:Rule
    val carpeta = TemporaryFolder()

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val compra by lazy {
        CompraDataStore(PreferenceDataStoreFactory.create(scope = alcance) { File(carpeta.root, "preferencias.preferences_pb") })
    }

    @After
    fun detener() = alcance.cancel()

    @Test
    fun `sin datos muestra anuncios y recuerda la compra`() =
        runBlocking {
            assertFalse(compra.observarAnunciosQuitados().first())

            compra.guardarAnunciosQuitados(true)
            assertTrue(compra.observarAnunciosQuitados().first())

            compra.guardarAnunciosQuitados(false)
            assertFalse(compra.observarAnunciosQuitados().first())
        }
}
