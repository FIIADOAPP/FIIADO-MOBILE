package com.fiiadoapp.fiado.core.data.cuenta

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.fiiadoapp.fiado.domain.CuentaAbierta
import com.fiiadoapp.fiado.domain.ErrorCuenta
import com.fiiadoapp.fiado.domain.Resultado
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Prueba el cliente real (Retrofit, OkHttp, renovación) contra un servidor simulado con las respuestas de la API. */
@RunWith(RobolectricTestRunner::class)
class RepositorioCuentaRemotoTest {
    @get:Rule
    val carpeta = TemporaryFolder()

    private val servidor = MockWebServer()
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var almacen: AlmacenSesion
    private lateinit var repositorio: RepositorioCuentaRemoto

    /** Invierte los bytes: suficiente para comprobar que nada se guarda en claro. */
    private class CifradorDePrueba : Cifrador {
        override fun cifrar(datos: ByteArray) = datos.reversedArray()

        override fun descifrar(datos: ByteArray) = datos.reversedArray()
    }

    @Before
    fun preparar() {
        servidor.start()
        val dataStore = PreferenceDataStoreFactory.create(scope = alcance) { File(carpeta.root, "sesion.preferences_pb") }
        almacen =
            AlmacenSesion(
                dataStore,
                CifradorDePrueba(),
                aTexto = { Base64.getEncoder().encodeToString(it) },
                aBytes = { Base64.getDecoder().decode(it) },
            )
        repositorio = crearRepositorioCuenta(servidor.url("/").toString(), almacen)
    }

    @After
    fun cerrar() {
        servidor.close()
        alcance.cancel()
    }

    private fun responder(
        codigo: Int,
        cuerpo: String = "",
    ) = servidor.enqueue(
        MockResponse
            .Builder()
            .code(codigo)
            .body(cuerpo)
            .build(),
    )

    private fun error(codigo: String) = """{"codigo":"$codigo","mensaje":"Mensaje de la API"}"""

    private fun sesion(
        acceso: String,
        refresco: String,
    ) = """{"token_acceso":"$acceso","token_refresco":"$refresco","acceso_expira_en":"2026-11-16T15:15:00Z",""" +
        """"cuenta":{"id":"7d1f2a40-0000-4000-8000-000000000001","correo":"rosa@bodega.pe","nombre_negocio":"Bodega Doña Rosa"}}"""

    private fun tokens(
        acceso: String,
        refresco: String,
    ) = """{"token_acceso":"$acceso","token_refresco":"$refresco","acceso_expira_en":"2026-11-16T15:30:00Z"}"""

    private fun abrirSesion() {
        responder(201, sesion("acceso-1", "refresco-1"))
        runBlocking { repositorio.iniciarSesion("rosa@bodega.pe", "secreto123", ceder = false) }
        servidor.takeRequest()
    }

    @Test
    fun `iniciar sesion envia los datos en snake_case y guarda la cuenta`() =
        runBlocking {
            responder(201, sesion("acceso-1", "refresco-1"))

            val resultado = repositorio.iniciarSesion("rosa@bodega.pe", "secreto123", ceder = true)

            val pedido = servidor.takeRequest()
            assertEquals("/sesiones", pedido.url.encodedPath)
            assertEquals("""{"correo":"rosa@bodega.pe","contrasena":"secreto123","ceder":true}""", pedido.body?.utf8())
            assertEquals(Resultado.Exito(CuentaAbierta("rosa@bodega.pe", "Bodega Doña Rosa")), resultado)
            assertEquals(CuentaAbierta("rosa@bodega.pe", "Bodega Doña Rosa"), repositorio.observar().first())
            assertEquals("refresco-1", almacen.actual()?.tokenRefresco)
        }

    @Test
    fun `los tokens no se guardan en claro`() {
        abrirSesion()

        val archivo = File(carpeta.root, "sesion.preferences_pb").readBytes().decodeToString()

        assertFalse("acceso-1" in archivo)
        assertFalse("refresco-1" in archivo)
    }

    @Test
    fun `un error de la api llega con su codigo y su mensaje`() =
        runBlocking {
            responder(409, error("sesion_activa_en_otro_dispositivo"))

            val resultado = repositorio.iniciarSesion("rosa@bodega.pe", "secreto123", ceder = false)

            val fallo = (resultado as Resultado.Fallo).error as ErrorCuenta.DeLaApi
            assertTrue(fallo.pideCederElDispositivo)
            assertEquals("Mensaje de la API", fallo.mensaje)
            assertNull(repositorio.observar().first())
        }

    @Test
    fun `sin servidor responde sin conexion`() =
        runBlocking {
            servidor.close()

            assertEquals(Resultado.Fallo(ErrorCuenta.SinConexion), repositorio.registrar("rosa@bodega.pe", "secreto123", "Bodega"))
        }

    @Test
    fun `credenciales incorrectas no cierran la sesion ya abierta`() =
        runBlocking {
            abrirSesion()
            responder(401, error("credenciales_invalidas"))

            val resultado = repositorio.iniciarSesion("rosa@bodega.pe", "otra", ceder = false)

            assertEquals("credenciales_invalidas", ((resultado as Resultado.Fallo).error as ErrorCuenta.DeLaApi).codigo)
            assertEquals("acceso-1", almacen.actual()?.tokenAcceso)
            assertFalse(repositorio.observarSesionTerminada().first())
        }

    @Test
    fun `un token vencido se renueva y la peticion se repite`() =
        runBlocking {
            abrirSesion()
            responder(401, error("token_vencido"))
            responder(200, tokens("acceso-2", "refresco-2"))
            responder(204)

            repositorio.cerrarSesion()

            assertEquals("Bearer acceso-1", servidor.takeRequest().headers["Authorization"])
            val renovacion = servidor.takeRequest()
            assertEquals("/sesiones/refresco", renovacion.url.encodedPath)
            assertEquals("""{"token_refresco":"refresco-1"}""", renovacion.body?.utf8())
            assertEquals("Bearer acceso-2", servidor.takeRequest().headers["Authorization"])
        }

    @Test
    fun `varias peticiones vencidas renuevan una sola vez`() {
        abrirSesion()
        val vencido =
            MockResponse
                .Builder()
                .code(401)
                .body(error("token_vencido"))
                .build()
        val renovado =
            MockResponse
                .Builder()
                .code(200)
                .body(tokens("acceso-2", "refresco-2"))
                .build()
        servidor.dispatcher =
            object : mockwebserver3.Dispatcher() {
                override fun dispatch(request: mockwebserver3.RecordedRequest): MockResponse =
                    when {
                        request.url.encodedPath == "/sesiones/refresco" -> renovado
                        request.headers["Authorization"] == "Bearer acceso-1" -> vencido
                        else -> MockResponse.Builder().code(204).build()
                    }
            }

        runBlocking { List(4) { async(Dispatchers.IO) { repositorio.cerrarSesion() } }.awaitAll() }

        val rutas = List(servidor.requestCount - 1) { servidor.takeRequest().url.encodedPath }
        assertEquals(1, rutas.count { it == "/sesiones/refresco" })
    }

    @Test
    fun `si la api reemplazo la sesion se borra del telefono y se avisa`() =
        runBlocking {
            abrirSesion()
            responder(401, error("sesion_reemplazada"))

            val resultado = repositorio.comprobarSesion()

            assertEquals("sesion_reemplazada", ((resultado as Resultado.Fallo).error as ErrorCuenta.DeLaApi).codigo)
            assertNull(repositorio.observar().first())
            assertTrue(repositorio.observarSesionTerminada().first())
            repositorio.descartarAvisoSesionTerminada()
            assertFalse(repositorio.observarSesionTerminada().first())
        }

    @Test
    fun `si la renovacion falla la sesion termina con aviso`() =
        runBlocking {
            abrirSesion()
            responder(401, error("token_vencido"))
            responder(401, error("sesion_reemplazada"))

            repositorio.comprobarSesion()

            assertNull(almacen.actual())
            assertTrue(repositorio.observarSesionTerminada().first())
        }

    @Test
    fun `cerrar sesion la borra del telefono aunque la api no responda`() =
        runBlocking {
            abrirSesion()
            servidor.close()

            repositorio.cerrarSesion()

            assertNull(repositorio.observar().first())
            assertFalse(repositorio.observarSesionTerminada().first())
        }

    @Test
    fun `comprobar la sesion actualiza el nombre del negocio`() =
        runBlocking {
            abrirSesion()
            responder(200, """{"id":"7d1f2a40-0000-4000-8000-000000000001","correo":"rosa@bodega.pe","nombre_negocio":"Bodega Rosa SAC"}""")

            repositorio.comprobarSesion()

            assertEquals("Bearer acceso-1", servidor.takeRequest().headers["Authorization"])
            assertEquals("Bodega Rosa SAC", repositorio.observar().first()?.nombreNegocio)
        }
}
