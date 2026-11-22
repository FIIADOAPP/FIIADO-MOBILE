package com.fiiadoapp.fiado.feature.movimientos

import com.fiiadoapp.fiado.core.testing.CuentaFalsa
import com.fiiadoapp.fiado.core.testing.PlantillaFalsa
import com.fiiadoapp.fiado.core.testing.ReglaDispatcherPrincipal
import com.fiiadoapp.fiado.domain.CuentaAbierta
import com.fiiadoapp.fiado.domain.ErrorPlantilla
import com.fiiadoapp.fiado.domain.PlantillaRecordatorio
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MensajeDeCobroViewModelTest {
    @get:Rule
    val dispatcher = ReglaDispatcherPrincipal()

    private val plantillas = PlantillaFalsa()
    private val cuenta = CuentaFalsa().apply { cuenta.value = CuentaAbierta("rosa@bodega.pe", "Bodega Doña Rosa") }

    @Test
    fun `muestra la plantilla guardada con una vista previa`() =
        runTest {
            val viewModel = MensajeDeCobroViewModel(plantillas, cuenta)

            assertEquals(PlantillaRecordatorio.POR_DEFECTO.texto, viewModel.estado.value.texto)
            assertEquals(
                "Hola Juana, te saluda Bodega Doña Rosa.\nTu saldo pendiente es de S/ 12.50.\n¡Gracias!",
                viewModel.estado.value.vistaPrevia,
            )
        }

    @Test
    fun `guarda un texto propio y avisa que termino`() =
        runTest {
            val viewModel = MensajeDeCobroViewModel(plantillas, cuenta)

            viewModel.cambiar("{cliente}, debes {saldo}.")
            viewModel.guardar()

            assertEquals("{cliente}, debes {saldo}.", plantillas.plantilla.value.texto)
            assertTrue(viewModel.estado.value.guardada)
        }

    @Test
    fun `sin el saldo no guarda y explica por que`() =
        runTest {
            val viewModel = MensajeDeCobroViewModel(plantillas, cuenta)

            viewModel.cambiar("Hola {cliente}")
            viewModel.guardar()

            assertNull(viewModel.estado.value.vistaPrevia)
            assertEquals(ErrorPlantilla.SIN_SALDO, viewModel.estado.value.error)
            assertFalse(viewModel.estado.value.guardada)
            assertEquals(PlantillaRecordatorio.POR_DEFECTO, plantillas.plantilla.value)
        }

    @Test
    fun `restablecer vuelve al texto por defecto`() =
        runTest {
            val viewModel = MensajeDeCobroViewModel(plantillas, cuenta)
            viewModel.cambiar("{cliente}, debes {saldo}.")

            viewModel.restablecer()

            assertEquals(PlantillaRecordatorio.POR_DEFECTO.texto, viewModel.estado.value.texto)
        }
}
