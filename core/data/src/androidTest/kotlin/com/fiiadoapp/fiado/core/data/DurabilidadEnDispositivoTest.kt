package com.fiiadoapp.fiado.core.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

/** Verifica el SQLite real del dispositivo (ADR-0003); Robolectric fuerza un journal en memoria y no sirve para esto. */
@RunWith(AndroidJUnit4::class)
class DurabilidadEnDispositivoTest {
    private val contexto = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val db = FiadoDatabase.construir(contexto, "durabilidad-prueba.db")

    @After
    fun cerrar() {
        db.close()
        contexto.deleteDatabase("durabilidad-prueba.db")
    }

    private fun pragma(nombre: String): String =
        db.openHelper.writableDatabase.query("PRAGMA $nombre").use { cursor ->
            cursor.moveToFirst()
            cursor.getString(0)
        }

    @Test
    fun usaElJournalClasicoYNoWal() {
        assertEquals("truncate", pragma("journal_mode"))
    }

    @Test
    fun cadaEscrituraSeConfirmaEnDisco() {
        assertEquals("2", pragma("synchronous"), "2 es FULL")
    }
}
