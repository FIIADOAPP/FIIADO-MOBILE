package com.fiiadoapp.fiado.feature.clientes

import com.fiiadoapp.fiado.domain.Cliente
import com.fiiadoapp.fiado.domain.Dinero
import com.fiiadoapp.fiado.domain.Movimiento
import com.fiiadoapp.fiado.domain.TipoMovimiento
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class ListaClientesViewModelTest {
    @get:Rule
    val dispatcher = ReglaDispatcherPrincipal()

    private val ahora = Instant.parse("2026-11-15T15:00:00Z")
    private val libreta = LibretaFalsa()
    private val rosa = Cliente(Uuid.random(), "Doña Rosa", null, ahora, ahora, null)
    private val pedro = Cliente(Uuid.random(), "Pedro", null, ahora, ahora, null)

    private fun TestScope.viewModelObservado() =
        ListaClientesViewModel(libreta.repositorioClientes).also {
            it.clientes.launchIn(backgroundScope + UnconfinedTestDispatcher(testScheduler))
        }

    @Test
    fun `muestra los clientes con su saldo`() =
        runTest {
            libreta.clientes.value = mapOf(rosa.id to rosa, pedro.id to pedro)
            val fiado =
                Movimiento(Uuid.random(), rosa.id, TipoMovimiento.FIADO, Dinero(1250), null, LocalDate.of(2026, 11, 15), ahora, null)
            libreta.movimientos.value = mapOf(fiado.id to fiado)

            val viewModel = viewModelObservado()

            assertEquals(listOf(rosa to Dinero(1250), pedro to Dinero.CERO), viewModel.clientes.value?.map { it.cliente to it.saldo })
        }

    @Test
    fun `buscar filtra la lista y conserva el texto escrito`() =
        runTest {
            libreta.clientes.value = mapOf(rosa.id to rosa, pedro.id to pedro)
            val viewModel = viewModelObservado()

            viewModel.buscar("ros")

            assertEquals("ros", viewModel.busqueda.value)
            assertEquals(listOf(rosa), viewModel.clientes.value?.map { it.cliente })
        }

    @Test
    fun `la lista se actualiza cuando se guarda un cliente`() =
        runTest {
            val viewModel = viewModelObservado()
            assertEquals(emptyList(), viewModel.clientes.value)

            libreta.repositorioClientes.guardar(pedro)

            assertEquals(listOf(pedro), viewModel.clientes.value?.map { it.cliente })
        }
}
