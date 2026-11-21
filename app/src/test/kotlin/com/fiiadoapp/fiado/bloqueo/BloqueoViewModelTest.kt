package com.fiiadoapp.fiado.bloqueo

import com.fiiadoapp.fiado.core.testing.BloqueoFalso
import com.fiiadoapp.fiado.core.testing.ReglaDispatcherPrincipal
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BloqueoViewModelTest {
    @get:Rule
    val dispatcher = ReglaDispatcherPrincipal()

    private val bloqueo = BloqueoFalso()

    @Test
    fun `apagado, la libreta abre directo y no se vuelve a bloquear`() =
        runTest {
            val viewModel = BloqueoViewModel(bloqueo)

            viewModel.alSalir(0)
            viewModel.alVolver(10 * 60_000)

            assertEquals(EstadoBloqueo.ABIERTA, viewModel.estado.value)
        }

    @Test
    fun `activo, pide la huella al abrir y tras un minuto en segundo plano`() =
        runTest {
            bloqueo.activo.value = true
            val viewModel = BloqueoViewModel(bloqueo)
            assertEquals(EstadoBloqueo.BLOQUEADA, viewModel.estado.value)

            viewModel.desbloquear()
            viewModel.alSalir(1_000)
            viewModel.alVolver(1_000 + 30_000)
            assertEquals(EstadoBloqueo.ABIERTA, viewModel.estado.value)

            viewModel.alSalir(100_000)
            viewModel.alVolver(100_000 + 60_000)
            assertEquals(EstadoBloqueo.BLOQUEADA, viewModel.estado.value)
        }

    @Test
    fun `salir mientras se pide el PIN no vuelve a bloquear al regresar`() =
        runTest {
            bloqueo.activo.value = true
            val viewModel = BloqueoViewModel(bloqueo)

            viewModel.alSalir(0)
            viewModel.desbloquear()
            viewModel.alVolver(5 * 60_000)

            assertEquals(EstadoBloqueo.ABIERTA, viewModel.estado.value)
        }

    @Test
    fun `si el telefono ya no tiene bloqueo de pantalla, se apaga y avisa`() =
        runTest {
            bloqueo.activo.value = true
            val viewModel = BloqueoViewModel(bloqueo)

            viewModel.desactivarPorElTelefono()

            assertFalse(bloqueo.activo.value)
            assertTrue(viewModel.desactivadoPorElTelefono.value)
            assertEquals(EstadoBloqueo.ABIERTA, viewModel.estado.value)
            viewModel.descartarAviso()
            assertFalse(viewModel.desactivadoPorElTelefono.value)
        }
}
