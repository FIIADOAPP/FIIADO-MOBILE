package com.fiiadoapp.fiado.core.data

import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse

/**
 * Verifica la configuración de Room (ADR-0003). Los pragmas reales se prueban en dispositivo con
 * `DurabilidadEnDispositivoTest`, porque el SQLite de Robolectric fuerza un journal en memoria.
 */
@RunWith(RobolectricTestRunner::class)
class DurabilidadTest {
    private val db = FiadoDatabase.construir(ApplicationProvider.getApplicationContext(), "durabilidad.db")

    @After
    fun cerrar() = db.close()

    @Test
    fun `la base de datos no usa WAL`() {
        assertFalse(db.openHelper.writableDatabase.isWriteAheadLoggingEnabled)
    }
}
