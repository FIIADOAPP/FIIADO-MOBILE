package com.fiiadoapp.fiado.feature.clientes

import androidx.lifecycle.SavedStateHandle
import com.fiiadoapp.fiado.core.testing.LibretaFalsa
import com.fiiadoapp.fiado.core.testing.ReglaDispatcherPrincipal
import com.fiiadoapp.fiado.domain.Cliente
import com.fiiadoapp.fiado.domain.Dinero
import com.fiiadoapp.fiado.domain.ErrorCliente
import com.fiiadoapp.fiado.domain.Movimiento
import com.fiiadoapp.fiado.domain.Telefono
import com.fiiadoapp.fiado.domain.TipoMovimiento
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class FormularioClienteViewModelTest {
    @get:Rule
    val dispatcher = ReglaDispatcherPrincipal()

    private val creado = Instant.parse("2026-11-10T15:00:00Z")
    private val ahora = Instant.parse("2026-11-15T15:00:00Z")
    private val reloj = Clock.fixed(ahora, ZoneId.of("America/Lima"))
    private val idNuevo = Uuid.random()
    private val libreta = LibretaFalsa()
    private val rosa = Cliente(Uuid.random(), "Doña Rosa", Telefono.desdeTexto("987654321"), creado, creado, null)

    private fun viewModel(clienteId: Uuid? = null) =
        FormularioClienteViewModel(
            SavedStateHandle(listOfNotNull(clienteId?.let { "clienteId" to it.toString() }).toMap()),
            libreta.repositorioClientes,
            libreta.repositorioMovimientos,
            { idNuevo },
            reloj,
        )

    private fun conCliente(vararg movimientos: Movimiento): FormularioClienteViewModel {
        libreta.clientes.value = mapOf(rosa.id to rosa)
        libreta.movimientos.value = movimientos.associateBy { it.id }
        return viewModel(rosa.id)
    }

    private fun fiado(centimos: Long) =
        Movimiento(Uuid.random(), rosa.id, TipoMovimiento.FIADO, Dinero(centimos), null, LocalDate.of(2026, 11, 14), creado, null)

    @Test
    fun `crear guarda el cliente con id nuevo y la hora del reloj`() =
        runTest {
            val viewModel = viewModel()
            viewModel.cambiarNombre("  Pedro  ")
            viewModel.cambiarTelefono("987-654-321")

            viewModel.guardar()

            assertEquals(Cliente(idNuevo, "Pedro", Telefono.desdeTexto("987654321"), ahora, ahora, null), libreta.clientes.value[idNuevo])
            assertTrue(viewModel.estado.value.terminado)
        }

    @Test
    fun `crear sin telefono lo guarda vacio`() =
        runTest {
            val viewModel = viewModel()
            viewModel.cambiarNombre("Pedro")
            viewModel.cambiarTelefono("   ")

            viewModel.guardar()

            assertNull(
                libreta.clientes.value
                    .getValue(idNuevo)
                    .telefono,
            )
        }

    @Test
    fun `un nombre vacio muestra el error y no guarda`() =
        runTest {
            val viewModel = viewModel()

            viewModel.guardar()

            assertEquals(ErrorCliente.NombreVacio, viewModel.estado.value.error)
            assertFalse(viewModel.estado.value.terminado)
            assertTrue(libreta.clientes.value.isEmpty())
        }

    @Test
    fun `escribir de nuevo borra el error del nombre`() =
        runTest {
            val viewModel = viewModel()
            viewModel.guardar()

            viewModel.cambiarNombre("P")

            assertNull(viewModel.estado.value.error)
        }

    @Test
    fun `un telefono invalido muestra el error y no guarda`() =
        runTest {
            val viewModel = viewModel()
            viewModel.cambiarNombre("Pedro")
            viewModel.cambiarTelefono("01 456 7890")

            viewModel.guardar()

            assertTrue(viewModel.estado.value.telefonoInvalido)
            assertTrue(libreta.clientes.value.isEmpty())
        }

    @Test
    fun `editar carga los datos y el saldo del cliente`() =
        runTest {
            val estado = conCliente(fiado(1250)).estado.value

            assertTrue(estado.esEdicion)
            assertFalse(estado.cargando)
            assertEquals("Doña Rosa", estado.nombre)
            assertEquals("987 654 321", estado.telefono)
            assertEquals(Dinero(1250), estado.saldo)
        }

    @Test
    fun `editar conserva id y fecha de creacion`() =
        runTest {
            val viewModel = conCliente()
            viewModel.cambiarNombre("Rosa Quispe")

            viewModel.guardar()

            val guardado = libreta.clientes.value.getValue(rosa.id)
            assertEquals("Rosa Quispe", guardado.nombre)
            assertEquals(creado, guardado.creadoEn)
            assertEquals(ahora, guardado.actualizadoEn)
            assertEquals(1, libreta.clientes.value.size)
        }

    @Test
    fun `archivar con saldo cero lo archiva y cierra`() =
        runTest {
            val viewModel = conCliente()

            viewModel.archivar()

            assertEquals(
                ahora,
                libreta.clientes.value
                    .getValue(rosa.id)
                    .archivadoEn,
            )
            assertTrue(viewModel.estado.value.terminado)
        }

    @Test
    fun `archivar con deuda no lo archiva`() =
        runTest {
            val viewModel = conCliente(fiado(500))

            viewModel.archivar()

            assertEquals(ErrorCliente.TieneSaldo(Dinero(500)), viewModel.estado.value.error)
            assertNull(
                libreta.clientes.value
                    .getValue(rosa.id)
                    .archivadoEn,
            )
            assertFalse(viewModel.estado.value.terminado)
        }
}
