package com.fiiadoapp.fiado.core.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.fiiadoapp.fiado.domain.MetodoCobro
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class MigracionesTest {
    @get:Rule
    val ayudante = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), FiadoDatabase::class.java)

    @Test
    fun `la version 2 siembra los metodos iniciales y asigna Efectivo a los pagos existentes`() {
        ayudante.createDatabase(base, 1).use { db ->
            db.execSQL("INSERT INTO clientes VALUES ('c1', 'Rosa', 'rosa', NULL, 0, 0, NULL)")
            db.execSQL("INSERT INTO movimientos VALUES ('m1', 'c1', 'FIADO', 1250, NULL, 20407, 0, NULL)")
            db.execSQL("INSERT INTO movimientos VALUES ('m2', 'c1', 'PAGO', 500, 'yape', 20407, 1, NULL)")
        }

        val db = ayudante.runMigrationsAndValidate(base, 2, true, MIGRACION_1_2)

        val metodos =
            db.query("SELECT id, nombre FROM metodos_cobro ORDER BY id").use { c ->
                List(c.count) {
                    c.moveToPosition(it)
                    c.getString(0) to
                        c.getString(1)
                }
            }
        assertEquals(MetodoCobro.INICIALES.map { it.id.toString() to it.nombre }, metodos)
        val asignados =
            db.query("SELECT id, metodoCobroId, nota FROM movimientos ORDER BY id").use { c ->
                List(c.count) {
                    c.moveToPosition(it)
                    Triple(c.getString(0), c.getString(1), c.getString(2))
                }
            }
        assertEquals(listOf(Triple("m1", null, null), Triple("m2", MetodoCobro.EFECTIVO.id.toString(), "yape")), asignados)
        db.close()
    }

    @Test
    fun `la version 3 agrega la cola vacia y conserva la libreta`() {
        ayudante.createDatabase(base, 2).use { db ->
            db.execSQL("INSERT INTO clientes VALUES ('c1', 'Rosa', 'rosa', NULL, 0, 0, NULL)")
            db.execSQL("INSERT INTO movimientos VALUES ('m1', 'c1', 'FIADO', 1250, NULL, 20407, 0, NULL, NULL)")
        }

        val db = ayudante.runMigrationsAndValidate(base, 3, true, MIGRACION_2_3)

        db.query("SELECT COUNT(*) FROM operaciones_pendientes").use { c ->
            c.moveToFirst()
            assertEquals(0, c.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM movimientos").use { c ->
            c.moveToFirst()
            assertEquals(1, c.getInt(0))
        }
        db.close()
    }

    @Test
    fun `la version 4 agrega el vinculo sin tocar la cola`() {
        ayudante.createDatabase(base, 3).use { db ->
            db.execSQL("INSERT INTO operaciones_pendientes (id, entidad, datos) VALUES ('o1', 'cliente', '{}')")
        }

        val db = ayudante.runMigrationsAndValidate(base, 4, true, MIGRACION_3_4)

        db.query("SELECT COUNT(*) FROM vinculo").use { c ->
            c.moveToFirst()
            assertEquals(0, c.getInt(0))
        }
        db.query("SELECT id FROM operaciones_pendientes").use { c ->
            c.moveToFirst()
            assertEquals("o1", c.getString(0))
        }
        db.close()
    }

    @Test
    fun `la version 5 agrega el plazo y deja sin plazo lo registrado antes`() {
        ayudante.createDatabase(base, 4).use { db ->
            db.execSQL("INSERT INTO clientes VALUES ('c1', 'Rosa', 'rosa', NULL, 0, 0, NULL)")
            db.execSQL("INSERT INTO movimientos VALUES ('m1', 'c1', 'FIADO', 1250, NULL, 20407, 0, NULL, NULL)")
        }

        val db = ayudante.runMigrationsAndValidate(base, 5, true, MIGRACION_4_5)

        db.query("SELECT vence FROM movimientos WHERE id = 'm1'").use { c ->
            c.moveToFirst()
            assertEquals(true, c.isNull(0))
        }
        db.close()
    }

    @Test
    fun `la version 6 agrega las prorrogas`() {
        ayudante.createDatabase(base, 5).close()

        val db = ayudante.runMigrationsAndValidate(base, 6, true, MIGRACION_5_6)

        db.query("SELECT COUNT(*) FROM prorrogas").use { c ->
            c.moveToFirst()
            assertEquals(0, c.getInt(0))
        }
        db.close()
    }

    /** Ruta absoluta: con un nombre relativo, el ayudante abre y valida rutas distintas. */
    private val base =
        InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .getDatabasePath("migracion-prueba.db")
            .absolutePath
}
