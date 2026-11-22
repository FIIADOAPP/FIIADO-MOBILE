package com.fiiadoapp.fiado.feature.clientes

import com.fiiadoapp.fiado.core.testing.LibretaFalsa
import com.fiiadoapp.fiado.core.testing.ReglaDispatcherPrincipal
import com.fiiadoapp.fiado.domain.Cliente
import com.fiiadoapp.fiado.domain.Dinero
import com.fiiadoapp.fiado.domain.Movimiento
import com.fiiadoapp.fiado.domain.NivelPuntualidad
import com.fiiadoapp.fiado.domain.TipoMovimiento
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

class ListaClientesViewModelTest {
    @get:Rule
    val dispatcher = ReglaDispatcherPrincipal()

    private val ahora = Instant.parse("2026-11-15T15:00:00Z")
    private val reloj = Clock.fixed(ahora.plusSeconds(60), ZoneId.of("America/Lima"))
    private val libreta = LibretaFalsa()
    private val rosa = Cliente(Uuid.random(), "Doña Rosa", null, ahora, ahora, null)
    private val pedro = Cliente(Uuid.random(), "Pedro", null, ahora, ahora, null)

    private fun TestScope.viewModelObservado() =
        ListaClientesViewModel(libreta.repositorioClientes, libreta.repositorioMovimientos, libreta.repositorioProrrogas, reloj).also {
            it.lista.launchIn(backgroundScope + UnconfinedTestDispatcher(testScheduler))
            it.porCobrar.launchIn(backgroundScope + UnconfinedTestDispatcher(testScheduler))
            it.puntualidades.launchIn(backgroundScope + UnconfinedTestDispatcher(testScheduler))
        }

    @Test
    fun `muestra los clientes con su saldo`() =
        runTest {
            libreta.clientes.value = mapOf(rosa.id to rosa, pedro.id to pedro)
            val fiado =
                Movimiento(Uuid.random(), rosa.id, TipoMovimiento.FIADO, Dinero(1250), null, LocalDate.of(2026, 11, 15), ahora, null)
            libreta.movimientos.value = mapOf(fiado.id to fiado)

            val viewModel = viewModelObservado()

            assertEquals(listOf(rosa to Dinero(1250), pedro to Dinero.CERO), viewModel.lista.value?.map { it.cliente to it.saldo })
        }

    @Test
    fun `buscar filtra la lista y conserva el texto escrito`() =
        runTest {
            libreta.clientes.value = mapOf(rosa.id to rosa, pedro.id to pedro)
            val viewModel = viewModelObservado()

            viewModel.buscar("ros")

            assertEquals("ros", viewModel.busqueda.value)
            assertEquals(listOf(rosa), viewModel.lista.value?.map { it.cliente })
        }

    @Test
    fun `la lista se actualiza cuando se guarda un cliente`() =
        runTest {
            val viewModel = viewModelObservado()
            assertEquals(emptyList(), viewModel.lista.value)

            libreta.repositorioClientes.guardar(pedro)

            assertEquals(listOf(pedro), viewModel.lista.value?.map { it.cliente })
        }

    private fun fiadoDeRosa() =
        Movimiento(Uuid.random(), rosa.id, TipoMovimiento.FIADO, Dinero(1250), null, LocalDate.of(2026, 11, 15), ahora, null)

    @Test
    fun `avisa el movimiento recien registrado`() =
        runTest {
            val fiado = fiadoDeRosa()
            libreta.clientes.value = mapOf(rosa.id to rosa)
            libreta.movimientos.value = mapOf(fiado.id to fiado)
            val viewModel = viewModelObservado()

            viewModel.avisarRegistro(fiado.id)

            assertEquals(AvisoRegistro(fiado.id, TipoMovimiento.FIADO, Dinero(1250), "Doña Rosa"), viewModel.aviso.value)
        }

    @Test
    fun `deshacer anula el movimiento y el saldo vuelve a cero`() =
        runTest {
            val fiado = fiadoDeRosa()
            libreta.clientes.value = mapOf(rosa.id to rosa)
            libreta.movimientos.value = mapOf(fiado.id to fiado)
            val viewModel = viewModelObservado()
            viewModel.avisarRegistro(fiado.id)

            viewModel.deshacer()

            assertEquals(
                ahora.plusSeconds(60),
                libreta.movimientos.value
                    .getValue(fiado.id)
                    .anuladoEn,
            )
            assertEquals(
                Dinero.CERO,
                viewModel.lista.value
                    ?.single()
                    ?.saldo,
            )
            assertNull(viewModel.aviso.value)
        }

    @Test
    fun `descartar el aviso no toca el movimiento`() =
        runTest {
            val fiado = fiadoDeRosa()
            libreta.clientes.value = mapOf(rosa.id to rosa)
            libreta.movimientos.value = mapOf(fiado.id to fiado)
            val viewModel = viewModelObservado()
            viewModel.avisarRegistro(fiado.id)

            viewModel.descartarAviso()

            assertNull(viewModel.aviso.value)
            assertNull(
                libreta.movimientos.value
                    .getValue(fiado.id)
                    .anuladoEn,
            )
        }

    @Test
    fun `calcula lo pendiente por cobrar de todos los clientes`() =
        runTest {
            val fiado = fiadoDeRosa()
            libreta.clientes.value = mapOf(rosa.id to rosa)
            libreta.movimientos.value = mapOf(fiado.id to fiado)

            assertEquals(Dinero(1250), viewModelObservado().porCobrar.value)
        }

    @Test
    fun `calcula la puntualidad de cada cliente con movimientos`() =
        runTest {
            val hoy = LocalDate.now(reloj)
            val vencido =
                Movimiento(
                    Uuid.random(),
                    rosa.id,
                    TipoMovimiento.FIADO,
                    Dinero(1000),
                    null,
                    hoy.minusDays(10),
                    ahora,
                    null,
                    vence = hoy.minusDays(8),
                )
            libreta.clientes.value = mapOf(rosa.id to rosa, pedro.id to pedro)
            libreta.movimientos.value = mapOf(vencido.id to vencido)

            val puntualidades = viewModelObservado().puntualidades.value

            assertEquals(NivelPuntualidad.CUIDADO, puntualidades[rosa.id]?.nivel)
            assertEquals(8L, puntualidades[rosa.id]?.diasVencido)
            assertNull(puntualidades[pedro.id])
        }
}
