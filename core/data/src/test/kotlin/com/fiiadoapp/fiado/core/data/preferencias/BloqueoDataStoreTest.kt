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
class BloqueoDataStoreTest {
    @get:Rule
    val carpeta = TemporaryFolder()

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val bloqueo by lazy {
        BloqueoDataStore(PreferenceDataStoreFactory.create(scope = alcance) { File(carpeta.root, "preferencias.preferences_pb") })
    }

    @After
    fun detener() = alcance.cancel()

    @Test
    fun `viene apagado y se puede activar y desactivar`() =
        runBlocking {
            assertFalse(bloqueo.observarActivo().first())

            bloqueo.cambiar(true)
            assertTrue(bloqueo.observarActivo().first())

            bloqueo.cambiar(false)
            assertFalse(bloqueo.observarActivo().first())
        }
}
