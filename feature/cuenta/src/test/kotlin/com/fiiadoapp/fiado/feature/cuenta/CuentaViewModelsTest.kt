package com.fiiadoapp.fiado.feature.cuenta

import androidx.lifecycle.SavedStateHandle
import com.fiiadoapp.fiado.core.testing.CuentaFalsa
import com.fiiadoapp.fiado.core.testing.ReglaDispatcherPrincipal
import com.fiiadoapp.fiado.core.testing.RespaldoFalso
import com.fiiadoapp.fiado.domain.CuentaAbierta
import com.fiiadoapp.fiado.domain.ErrorCuenta
import com.fiiadoapp.fiado.domain.EstadoRespaldo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CuentaViewModelsTest {
    @get:Rule
    val dispatcher = ReglaDispatcherPrincipal()

    private val cuenta = CuentaFalsa()
    private val respaldo = RespaldoFalso()
    private val alDia = EstadoRespaldo(pendientes = 0, detenido = false)
    private val rosa = CuentaAbierta("rosa@bodega.pe", "Bodega Doña Rosa")
    private val ceder = ErrorCuenta.DeLaApi("sesion_activa_en_otro_dispositivo", "Tu cuenta está abierta en otro teléfono.")

    private fun conCorreo(correo: String) = SavedStateHandle(mapOf("correo" to correo))

    @Test
    fun `sin cuenta la pantalla ofrece crearla o iniciar sesion`() =
        runTest {
            val viewModel = CuentaViewModel(cuenta, respaldo)
            viewModel.estado.launchIn(backgroundScope + UnconfinedTestDispatcher(testScheduler))

            assertEquals(EstadoCuenta.SinCuenta, viewModel.estado.value)
            assertFalse("comprobarSesion" in cuenta.llamadas)
        }

    @Test
    fun `con cuenta la muestra y comprueba con la api que la sesion siga abierta`() =
        runTest {
            cuenta.cuenta.value = rosa
            val viewModel = CuentaViewModel(cuenta, respaldo)
            viewModel.estado.launchIn(backgroundScope + UnconfinedTestDispatcher(testScheduler))

            assertEquals(EstadoCuenta.Abierta(rosa, alDia), viewModel.estado.value)
            assertTrue("comprobarSesion" in cuenta.llamadas)
        }

    @Test
    fun `con cuenta muestra los cambios por respaldar y si el envio se detuvo`() =
        runTest {
            cuenta.cuenta.value = rosa
            val viewModel = CuentaViewModel(cuenta, respaldo)
            viewModel.estado.launchIn(backgroundScope + UnconfinedTestDispatcher(testScheduler))

            respaldo.estado.value = EstadoRespaldo(pendientes = 3, detenido = false)
            assertEquals(EstadoCuenta.Abierta(rosa, EstadoRespaldo(3, false)), viewModel.estado.value)

            respaldo.estado.value = EstadoRespaldo(pendientes = 3, detenido = true)
            assertEquals(EstadoCuenta.Abierta(rosa, EstadoRespaldo(3, true)), viewModel.estado.value)
        }

    @Test
    fun `cerrar sesion vuelve al modo sin cuenta`() =
        runTest {
            cuenta.cuenta.value = rosa
            val viewModel = CuentaViewModel(cuenta, respaldo)
            viewModel.estado.launchIn(backgroundScope + UnconfinedTestDispatcher(testScheduler))

            viewModel.cerrarSesion()

            assertEquals(EstadoCuenta.SinCuenta, viewModel.estado.value)
        }

    @Test
    fun `crear cuenta registra y pasa a verificar el correo`() =
        runTest {
            val viewModel = CrearCuentaViewModel(cuenta)
            viewModel.cambiarNombreNegocio("Bodega Doña Rosa")
            viewModel.cambiarCorreo(" rosa@bodega.pe ")
            viewModel.cambiarContrasena("secreto123")

            viewModel.crear()

            assertEquals("registrar rosa@bodega.pe Bodega Doña Rosa", cuenta.llamadas.single())
            assertEquals("rosa@bodega.pe", viewModel.estado.value.creada)
        }

    @Test
    fun `un error al crear la cuenta se muestra y se borra al escribir`() =
        runTest {
            val viewModel = CrearCuentaViewModel(cuenta)
            cuenta.proximoError = ErrorCuenta.SinConexion

            viewModel.crear()
            assertEquals(ErrorCuenta.SinConexion, viewModel.estado.value.error)
            viewModel.cambiarCorreo("rosa@bodega.pe")

            assertNull(viewModel.estado.value.error)
            assertNull(viewModel.estado.value.creada)
        }

    @Test
    fun `el codigo solo acepta seis digitos y verifica el correo`() =
        runTest {
            val viewModel = VerificarCorreoViewModel(conCorreo("rosa@bodega.pe"), cuenta)

            viewModel.cambiarCodigo("12a 34-5678")
            viewModel.verificar()

            assertEquals("verificar rosa@bodega.pe 123456", cuenta.llamadas.single())
            assertTrue(viewModel.estado.value.verificado)
        }

    @Test
    fun `reenviar el codigo espera 60 segundos`() =
        runTest {
            val viewModel = VerificarCorreoViewModel(conCorreo("rosa@bodega.pe"), cuenta)

            viewModel.reenviar()
            assertTrue(cuenta.llamadas.isEmpty())
            advanceTimeBy(60_001)
            viewModel.reenviar()

            assertEquals(listOf("reenviar rosa@bodega.pe"), cuenta.llamadas)
            assertTrue(viewModel.estado.value.reenviado)
        }

    @Test
    fun `iniciar sesion con otro telefono activo pide ceder y al aceptar cede`() =
        runTest {
            val viewModel = IniciarSesionViewModel(conCorreo("rosa@bodega.pe"), cuenta)
            viewModel.cambiarContrasena("secreto123")
            cuenta.proximoError = ceder

            viewModel.iniciar()
            assertTrue(viewModel.estado.value.pidiendoCeder)
            viewModel.cederElOtroTelefono()

            assertEquals(listOf("iniciar rosa@bodega.pe ceder=false", "iniciar rosa@bodega.pe ceder=true"), cuenta.llamadas)
            assertTrue(viewModel.estado.value.iniciada)
        }

    @Test
    fun `cancelar ceder no inicia sesion`() =
        runTest {
            val viewModel = IniciarSesionViewModel(conCorreo("rosa@bodega.pe"), cuenta)
            cuenta.proximoError = ceder
            viewModel.iniciar()

            viewModel.cancelarCeder()

            assertFalse(viewModel.estado.value.pidiendoCeder)
            assertFalse(viewModel.estado.value.iniciada)
            assertNull(cuenta.cuenta.value)
        }

    @Test
    fun `un correo sin verificar lleva a verificarlo`() =
        runTest {
            val viewModel = IniciarSesionViewModel(conCorreo("rosa@bodega.pe"), cuenta)
            cuenta.proximoError = ErrorCuenta.DeLaApi("correo_sin_verificar", "Primero verifica tu correo.")

            viewModel.iniciar()

            assertTrue(viewModel.estado.value.faltaVerificar)
            assertNull(viewModel.estado.value.error)
        }

    @Test
    fun `credenciales incorrectas muestran el mensaje de la api`() =
        runTest {
            val viewModel = IniciarSesionViewModel(SavedStateHandle(), cuenta)
            val incorrectas = ErrorCuenta.DeLaApi("credenciales_invalidas", "Correo o contraseña incorrectos.")
            cuenta.proximoError = incorrectas

            viewModel.iniciar()

            assertEquals(incorrectas, viewModel.estado.value.error)
            assertEquals("", viewModel.estado.value.correo)
        }

    @Test
    fun `recuperar pide el codigo y luego restablece la contrasena`() =
        runTest {
            val viewModel = RecuperarContrasenaViewModel(conCorreo("rosa@bodega.pe"), cuenta)

            viewModel.pedirCodigo()
            assertTrue(viewModel.estado.value.codigoEnviado)
            viewModel.cambiarCodigo("654321")
            viewModel.cambiarContrasenaNueva("nueva-clave-1")
            viewModel.restablecer()

            assertEquals(listOf("solicitarRecuperacion rosa@bodega.pe", "restablecer rosa@bodega.pe 654321"), cuenta.llamadas)
            assertTrue(viewModel.estado.value.restablecida)
        }

    @Test
    fun `el aviso de sesion terminada se muestra hasta entenderlo`() =
        runTest {
            cuenta.sesionTerminada.value = true
            val viewModel = AvisoSesionViewModel(cuenta)
            viewModel.mostrar.launchIn(backgroundScope + UnconfinedTestDispatcher(testScheduler))
            assertTrue(viewModel.mostrar.value)

            viewModel.entendido()

            assertFalse(viewModel.mostrar.value)
        }
}
