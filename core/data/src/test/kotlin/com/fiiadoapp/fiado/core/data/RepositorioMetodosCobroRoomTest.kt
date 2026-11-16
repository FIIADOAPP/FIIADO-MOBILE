package com.fiiadoapp.fiado.core.data

import com.fiiadoapp.fiado.domain.MetodoCobro
import com.fiiadoapp.fiado.domain.TipoMovimiento.FIADO
import com.fiiadoapp.fiado.domain.TipoMovimiento.PAGO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

@RunWith(RobolectricTestRunner::class)
class RepositorioMetodosCobroRoomTest : BaseDeDatosDePrueba() {
    private val rosa = cliente("Doña Rosa")

    @Test
    fun `una base nueva empieza con Efectivo, Yape y Plin`() =
        runTest {
            assertEquals(MetodoCobro.INICIALES, metodos.observarActivos().first())
        }

    @Test
    fun `guarda un metodo nuevo despues de los iniciales y lo renombra`() =
        runTest {
            val transferencia = MetodoCobro(Uuid.random(), "Transferencia", ahora, ahora, null)
            metodos.guardar(transferencia)
            metodos.guardar(transferencia.copy(nombre = "Transferencia BCP"))

            assertEquals(listOf("Efectivo", "Yape", "Plin", "Transferencia BCP"), metodos.observarActivos().first().map { it.nombre })
        }

    @Test
    fun `los archivados no aparecen entre los activos`() =
        runTest {
            metodos.guardar(MetodoCobro.PLIN.copy(archivadoEn = ahora))

            assertEquals(listOf(MetodoCobro.EFECTIVO, MetodoCobro.YAPE), metodos.observarActivos().first())
        }

    @Test
    fun `un pago guarda y lee su metodo`() =
        runTest {
            clientes.guardar(rosa)
            val pago = movimiento(rosa, PAGO, 500, metodo = MetodoCobro.YAPE)

            movimientos.guardar(pago)

            assertEquals(pago, movimientos.obtener(pago.id))
        }

    @Test
    fun `el ultimo metodo usado es el del pago vigente mas reciente`() =
        runTest {
            clientes.guardar(rosa)
            assertNull(movimientos.ultimoMetodoCobroUsado())
            movimientos.guardar(movimiento(rosa, PAGO, 100, metodo = MetodoCobro.YAPE, registradoEn = ahora))
            movimientos.guardar(movimiento(rosa, PAGO, 100, metodo = MetodoCobro.PLIN, registradoEn = ahora.plusSeconds(60)).anular(ahora))
            movimientos.guardar(movimiento(rosa, FIADO, 100, registradoEn = ahora.plusSeconds(120)))

            assertEquals(MetodoCobro.YAPE.id, movimientos.ultimoMetodoCobroUsado())
        }
}
