package com.fiiadoapp.fiado.core.data

import com.fiiadoapp.fiado.domain.ClienteConSaldo
import com.fiiadoapp.fiado.domain.Dinero
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
class RepositorioClientesRoomTest : BaseDeDatosDePrueba() {
    private suspend fun activos(busqueda: String = "") = clientes.observarActivos(busqueda).first()

    @Test
    fun `guarda y lee un cliente sin perder datos`() =
        runTest {
            val rosa = cliente("Doña Rosa", telefono = "987654321")

            clientes.guardar(rosa)

            assertEquals(rosa, clientes.observar(rosa.id).first())
        }

    @Test
    fun `un cliente inexistente se observa como nulo`() =
        runTest {
            assertNull(clientes.observar(Uuid.random()).first())
        }

    @Test
    fun `guardar de nuevo reemplaza al cliente`() =
        runTest {
            val rosa = cliente("Doña Rosa")
            clientes.guardar(rosa)

            clientes.guardar(rosa.copy(nombre = "Rosa Quispe"))

            assertEquals(listOf("Rosa Quispe"), activos().map { it.cliente.nombre })
        }

    @Test
    fun `el saldo suma fiados y resta pagos vigentes`() =
        runTest {
            val rosa = cliente("Doña Rosa")
            val pedro = cliente("Pedro")
            clientes.guardar(rosa)
            clientes.guardar(pedro)
            movimientos.guardar(movimiento(rosa, FIADO, 1000))
            movimientos.guardar(movimiento(rosa, PAGO, 300))
            movimientos.guardar(movimiento(rosa, FIADO, 5000).anular(ahora))
            movimientos.guardar(movimiento(pedro, FIADO, 500))
            movimientos.guardar(movimiento(pedro, PAGO, 800))

            assertEquals(
                listOf(ClienteConSaldo(rosa, Dinero(700)), ClienteConSaldo(pedro, Dinero(-300))),
                activos(),
            )
        }

    @Test
    fun `un cliente sin movimientos tiene saldo cero`() =
        runTest {
            clientes.guardar(cliente("Pedro"))

            assertEquals(Dinero.CERO, activos().single().saldo)
        }

    @Test
    fun `los archivados no aparecen entre los activos`() =
        runTest {
            clientes.guardar(cliente("Pedro"))
            clientes.guardar(cliente("Juana", archivadoEn = ahora))

            assertEquals(listOf("Pedro"), activos().map { it.cliente.nombre })
        }

    @Test
    fun `ordena por nombre sin importar mayusculas ni tildes`() =
        runTest {
            listOf("Óscar", "beto", "Ana", "Ñato").forEach { clientes.guardar(cliente(it)) }

            assertEquals(listOf("Ana", "beto", "Ñato", "Óscar"), activos().map { it.cliente.nombre })
        }

    @Test
    fun `busca sin distinguir mayusculas ni tildes`() =
        runTest {
            listOf("Doña Rosa", "Pedro Núñez", "Rosalía").forEach { clientes.guardar(cliente(it)) }

            assertEquals(listOf("Doña Rosa", "Rosalía"), activos("ROSA").map { it.cliente.nombre })
            assertEquals(listOf("Doña Rosa"), activos("dona").map { it.cliente.nombre })
            assertEquals(listOf("Pedro Núñez"), activos("  nunez ").map { it.cliente.nombre })
        }

    @Test
    fun `los comodines del usuario se buscan literalmente`() =
        runTest {
            listOf("Ana", "100% Pepe", "Juan_Carlos").forEach { clientes.guardar(cliente(it)) }

            assertEquals(listOf("100% Pepe"), activos("%").map { it.cliente.nombre })
            assertEquals(listOf("Juan_Carlos"), activos("_").map { it.cliente.nombre })
        }

    @Test
    fun `la lista se actualiza al registrar un movimiento`() =
        runTest {
            val rosa = cliente("Doña Rosa")
            clientes.guardar(rosa)
            val flujo = clientes.observarActivos("")
            assertEquals(Dinero.CERO, flujo.first().single().saldo)

            movimientos.guardar(movimiento(rosa, FIADO, 1250))

            assertEquals(Dinero(1250), flujo.first().single().saldo)
        }
}
