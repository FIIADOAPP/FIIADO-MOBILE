package com.fiiadoapp.fiado.feature.movimientos

import com.fiiadoapp.fiado.core.testing.LibretaFalsa
import com.fiiadoapp.fiado.core.testing.ReglaDispatcherPrincipal
import com.fiiadoapp.fiado.domain.ErrorMetodoCobro
import com.fiiadoapp.fiado.domain.MetodoCobro
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class MetodosCobroViewModelTest {
    @get:Rule
    val dispatcher = ReglaDispatcherPrincipal()

    private val ahora = Instant.parse("2026-11-15T15:00:00Z")
    private val idNuevo = Uuid.random()
    private val libreta = LibretaFalsa()

    /** Diferido: el ViewModel debe crearse después de que la regla fije el dispatcher principal. */
    private val viewModel by lazy {
        MetodosCobroViewModel(libreta.repositorioMetodosCobro, { idNuevo }, Clock.fixed(ahora, ZoneId.of("America/Lima")))
    }

    private fun guardado(id: Uuid) = libreta.metodos.value.getValue(id)

    @Test
    fun `crea un metodo nuevo y cierra el dialogo`() =
        runTest {
            viewModel.nuevo()
            viewModel.cambiarNombre(" Transferencia  BCP ")

            viewModel.guardar()

            assertEquals(MetodoCobro(idNuevo, "Transferencia BCP", ahora, ahora, null), guardado(idNuevo))
            assertNull(viewModel.edicion.value)
        }

    @Test
    fun `un nombre repetido muestra el error y no guarda`() =
        runTest {
            viewModel.nuevo()
            viewModel.cambiarNombre("YAPE")

            viewModel.guardar()

            assertEquals(ErrorMetodoCobro.NombreRepetido, viewModel.edicion.value?.error)
            assertEquals(3, libreta.metodos.value.size)
        }

    @Test
    fun `renombra un metodo conservando su id`() =
        runTest {
            viewModel.editar(MetodoCobro.YAPE)
            viewModel.cambiarNombre("Yape del negocio")

            viewModel.guardar()

            assertEquals("Yape del negocio", guardado(MetodoCobro.YAPE.id).nombre)
        }

    @Test
    fun `archiva un metodo`() =
        runTest {
            viewModel.editar(MetodoCobro.PLIN)

            viewModel.archivar()

            assertEquals(ahora, guardado(MetodoCobro.PLIN.id).archivadoEn)
            assertNull(viewModel.edicion.value)
        }

    @Test
    fun `no archiva el ultimo metodo activo`() =
        runTest {
            libreta.metodos.value = mapOf(MetodoCobro.EFECTIVO.id to MetodoCobro.EFECTIVO)
            viewModel.editar(MetodoCobro.EFECTIVO)

            viewModel.archivar()

            assertEquals(ErrorMetodoCobro.UltimoActivo, viewModel.edicion.value?.error)
            assertNull(guardado(MetodoCobro.EFECTIVO.id).archivadoEn)
        }

    @Test
    fun `cancelar cierra el dialogo sin guardar`() =
        runTest {
            viewModel.nuevo()
            viewModel.cambiarNombre("Tarjeta")

            viewModel.cancelar()

            assertNull(viewModel.edicion.value)
            assertEquals(3, libreta.metodos.value.size)
        }
}
