package com.fiiadoapp.fiado.feature.movimientos

import androidx.lifecycle.SavedStateHandle
import com.fiiadoapp.fiado.core.testing.LibretaFalsa
import com.fiiadoapp.fiado.core.testing.ReglaDispatcherPrincipal
import com.fiiadoapp.fiado.domain.Cliente
import com.fiiadoapp.fiado.domain.Dinero
import com.fiiadoapp.fiado.domain.MetodoCobro
import com.fiiadoapp.fiado.domain.Movimiento
import com.fiiadoapp.fiado.domain.TipoMovimiento.FIADO
import com.fiiadoapp.fiado.domain.TipoMovimiento.PAGO
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class HistorialViewModelTest {
    @get:Rule
    val dispatcher = ReglaDispatcherPrincipal()

    private val ahora = Instant.parse("2026-11-15T15:00:00Z")
    private val hoy = LocalDate.of(2026, 11, 15)
    private val libreta = LibretaFalsa()
    private val rosa = Cliente(Uuid.random(), "Doña Rosa", null, ahora, ahora, null)
    private val fiado = Movimiento(Uuid.random(), rosa.id, FIADO, Dinero(1250), "pan", hoy, ahora, null)
    private val pago = Movimiento(Uuid.random(), rosa.id, PAGO, Dinero(500), null, hoy, ahora.plusSeconds(60), null, MetodoCobro.YAPE.id)

    private fun TestScope.viewModelObservado(): HistorialViewModel {
        libreta.clientes.value = mapOf(rosa.id to rosa)
        libreta.movimientos.value = listOf(fiado, pago).associateBy { it.id }
        return HistorialViewModel(
            SavedStateHandle(mapOf("clienteId" to rosa.id.toString())),
            libreta.repositorioClientes,
            libreta.repositorioMovimientos,
            libreta.repositorioMetodosCobro,
            Clock.fixed(ahora.plusSeconds(120), ZoneId.of("America/Lima")),
        ).also { it.estado.launchIn(backgroundScope + UnconfinedTestDispatcher(testScheduler)) }
    }

    @Test
    fun `muestra el cliente, su saldo y el metodo de cada pago`() =
        runTest {
            val estado = viewModelObservado().estado.value

            assertEquals(rosa, estado?.cliente)
            assertEquals(Dinero(750), estado?.saldo)
            assertEquals(setOf(FilaHistorial(fiado, null), FilaHistorial(pago, "Yape")), estado?.filas?.toSet())
        }

    @Test
    fun `un pago conserva el nombre de su metodo aunque se haya archivado`() =
        runTest {
            libreta.metodos.value += MetodoCobro.YAPE.id to MetodoCobro.YAPE.copy(archivadoEn = ahora)

            val fila =
                viewModelObservado()
                    .estado.value
                    ?.filas
                    ?.single { it.movimiento.id == pago.id }

            assertEquals("Yape", fila?.nombreMetodo)
        }

    @Test
    fun `anular pide confirmacion y luego deja el movimiento tachado y fuera del saldo`() =
        runTest {
            val viewModel = viewModelObservado()

            viewModel.pedirAnulacion(fiado)
            assertEquals(fiado, viewModel.porAnular.value)
            viewModel.confirmarAnulacion()

            assertNull(viewModel.porAnular.value)
            assertEquals(
                ahora.plusSeconds(120),
                libreta.movimientos.value
                    .getValue(fiado.id)
                    .anuladoEn,
            )
            assertEquals(Dinero(-500), viewModel.estado.value?.saldo)
        }

    @Test
    fun `cancelar no anula nada`() =
        runTest {
            val viewModel = viewModelObservado()
            viewModel.pedirAnulacion(fiado)

            viewModel.cancelarAnulacion()

            assertNull(viewModel.porAnular.value)
            assertNull(
                libreta.movimientos.value
                    .getValue(fiado.id)
                    .anuladoEn,
            )
        }

    @Test
    fun `un movimiento ya anulado no se vuelve a ofrecer para anular`() =
        runTest {
            val viewModel = viewModelObservado()

            viewModel.pedirAnulacion(fiado.anular(ahora))

            assertNull(viewModel.porAnular.value)
        }
}
