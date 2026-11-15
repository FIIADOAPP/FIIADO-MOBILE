package com.fiiadoapp.fiado.core.data

import android.database.sqlite.SQLiteConstraintException
import com.fiiadoapp.fiado.domain.TipoMovimiento.FIADO
import com.fiiadoapp.fiado.domain.TipoMovimiento.PAGO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.uuid.Uuid

@RunWith(RobolectricTestRunner::class)
class RepositorioMovimientosRoomTest : BaseDeDatosDePrueba() {
    private val rosa = cliente("Doña Rosa")

    @Test
    fun `guarda y lee un movimiento sin perder datos`() =
        runTest {
            clientes.guardar(rosa)
            val fiado = movimiento(rosa, FIADO, 1250, nota = "2 panes y leche")

            movimientos.guardar(fiado)

            assertEquals(listOf(fiado), movimientos.observarDeCliente(rosa.id).first())
        }

    @Test
    fun `el historial va del mas reciente al mas antiguo`() =
        runTest {
            clientes.guardar(rosa)
            val ayer = movimiento(rosa, FIADO, 100, fecha = hoy.minusDays(1), registradoEn = ahora.plusSeconds(60))
            val hoyTemprano = movimiento(rosa, FIADO, 200, registradoEn = ahora)
            val hoyTarde = movimiento(rosa, PAGO, 300, registradoEn = ahora.plusSeconds(3600))
            listOf(ayer, hoyTemprano, hoyTarde).forEach { movimientos.guardar(it) }

            assertEquals(listOf(hoyTarde, hoyTemprano, ayer), movimientos.observarDeCliente(rosa.id).first())
        }

    @Test
    fun `anular reemplaza el movimiento y lo conserva en el historial`() =
        runTest {
            clientes.guardar(rosa)
            val fiado = movimiento(rosa, FIADO, 1250)
            movimientos.guardar(fiado)

            movimientos.guardar(fiado.anular(ahora.plusSeconds(60)))

            val historial = movimientos.observarDeCliente(rosa.id).first()
            assertEquals(1, historial.size)
            assertEquals(ahora.plusSeconds(60), historial.single().anuladoEn)
        }

    @Test
    fun `observar todos devuelve los movimientos de todos los clientes`() =
        runTest {
            val pedro = cliente("Pedro")
            clientes.guardar(rosa)
            clientes.guardar(pedro)
            val deRosa = movimiento(rosa, FIADO, 100)
            val dePedro = movimiento(pedro, PAGO, 200)
            movimientos.guardar(deRosa)
            movimientos.guardar(dePedro)

            assertEquals(setOf(deRosa, dePedro), movimientos.observarTodos().first().toSet())
        }

    @Test
    fun `no acepta un movimiento de un cliente inexistente`() =
        runTest {
            assertFailsWith<SQLiteConstraintException> { movimientos.guardar(movimiento(rosa, FIADO, 100)) }
        }

    @Test
    fun `obtiene un movimiento por su id`() =
        runTest {
            clientes.guardar(rosa)
            val fiado = movimiento(rosa, FIADO, 1250)
            movimientos.guardar(fiado)

            assertEquals(fiado, movimientos.obtener(fiado.id))
            assertNull(movimientos.obtener(Uuid.random()))
        }
}
