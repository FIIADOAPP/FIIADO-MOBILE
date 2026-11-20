package com.fiiadoapp.fiado.core.data

import com.fiiadoapp.fiado.core.data.cuenta.AlmacenSesion
import com.fiiadoapp.fiado.core.data.cuenta.SesionGuardada
import com.fiiadoapp.fiado.core.data.cuenta.almacenDePrueba
import com.fiiadoapp.fiado.core.data.cuenta.crearRetrofitConSesion
import com.fiiadoapp.fiado.core.data.sincronizacion.ApiSincronizacion
import com.fiiadoapp.fiado.core.data.sincronizacion.Restaurador
import com.fiiadoapp.fiado.core.data.sincronizacion.ResultadoEnvio
import com.fiiadoapp.fiado.core.data.sincronizacion.Sincronizador
import com.fiiadoapp.fiado.core.data.sincronizacion.VinculoEntity
import com.fiiadoapp.fiado.domain.TipoMovimiento.FIADO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import mockwebserver3.SocketEffect
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** El sincronizador real contra un servidor simulado que responde como FIIADO-API. */
@RunWith(RobolectricTestRunner::class)
class SincronizadorTest : BaseDeDatosDePrueba() {
    @get:Rule
    val carpeta = TemporaryFolder()

    private val servidor = MockWebServer()
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var almacen: AlmacenSesion
    private lateinit var sincronizador: Sincronizador

    /** Ids de las operaciones que el servidor simulado ya aplicó, en el orden en que llegaron. */
    private val aplicadas = mutableListOf<String>()

    @Before
    fun preparar() {
        servidor.start()
        almacen = almacenDePrueba(carpeta.root, alcance)
        val api = crearRetrofitConSesion(servidor.url("/").toString(), almacen).create(ApiSincronizacion::class.java)
        sincronizador = Sincronizador(db.colaDao(), api, almacen, Restaurador(db, api, GeneradorUuidV7()) {})
    }

    @After
    fun detener() {
        servidor.close()
        alcance.cancel()
    }

    /** Con sesión y la libreta ya restaurada: aquí se prueba solo el envío. */
    private fun conSesion() =
        runBlocking {
            almacen.guardar(SesionGuardada("acceso", "refresco", "rosa@bodega.pe", "Bodega"))
            db.restauracionDao().guardarVinculo(VinculoEntity(correo = "rosa@bodega.pe", cursor = null, restaurada = true, primera = false))
        }

    private fun lote(peticion: RecordedRequest): List<JsonObject> =
        Json
            .parseToJsonElement(checkNotNull(peticion.body).utf8())
            .jsonObject["operaciones"]!!
            .jsonArray
            .map { it.jsonObject }

    /** Aplica y confirma todo lo que llega, como la API. */
    private fun servidorQueConfirma() {
        servidor.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val ids = lote(request).map { it["id"]!!.jsonPrimitive.content }
                    aplicadas += ids
                    return respuesta(200, """{"aplicadas":${ids.joinToString(",", "[", "]") { "\"$it\"" }}}""")
                }
            }
    }

    private fun respuesta(
        codigo: Int,
        cuerpo: String,
    ) = MockResponse
        .Builder()
        .code(codigo)
        .addHeader("Content-Type", "application/json")
        .body(cuerpo)
        .build()

    private suspend fun registrarFiados(cantidad: Int) {
        val rosa = cliente("Doña Rosa")
        clientes.guardar(rosa)
        repeat(cantidad) { movimientos.guardar(movimiento(rosa, FIADO, 100L + it)) }
    }

    private suspend fun pendientes() = db.colaDao().primeras(1_000)

    @Test
    fun `sin sesion no envia nada y conserva la cola`() =
        runBlocking {
            registrarFiados(1)

            assertEquals(ResultadoEnvio.SIN_SESION, sincronizador.enviar())

            assertEquals(0, servidor.requestCount)
            assertEquals(2, pendientes().size)
        }

    @Test
    fun `envia toda la cola en lotes de 100 en orden y la vacia`() =
        runBlocking {
            conSesion()
            registrarFiados(149)
            val enOrden = pendientes().map { it.id }
            servidorQueConfirma()

            assertEquals(ResultadoEnvio.COMPLETO, sincronizador.enviar())

            assertEquals(2, servidor.requestCount)
            assertEquals(enOrden, aplicadas)
            assertEquals(emptyList(), pendientes())
        }

    @Test
    fun `cada operacion lleva su id, su entidad, la foto y el token`() =
        runBlocking {
            conSesion()
            registrarFiados(1)
            servidorQueConfirma()

            sincronizador.enviar()

            val peticion = servidor.takeRequest()
            assertEquals("/sincronizacion/operaciones", peticion.target)
            assertEquals("Bearer acceso", peticion.headers["Authorization"])
            assertNull(peticion.headers["Fiado-Requiere-Sesion"])
            val (cliente, fiado) = lote(peticion)
            assertEquals("cliente", cliente["entidad"]!!.jsonPrimitive.content)
            assertEquals("movimiento", fiado["entidad"]!!.jsonPrimitive.content)
            assertEquals("fiado", fiado["datos"]!!.jsonObject["tipo"]!!.jsonPrimitive.content)
            assertTrue(fiado["datos"]!!.jsonObject.containsKey("anulado_en"))
        }

    @Test
    fun `si el servidor falla o se corta la red, la cola queda intacta para reintentar`() =
        runBlocking {
            conSesion()
            registrarFiados(1)
            servidor.enqueue(respuesta(503, """{"codigo":"no_disponible","mensaje":"..."}"""))
            servidor.enqueue(
                MockResponse
                    .Builder()
                    .onResponseStart(SocketEffect.CloseSocket())
                    .build(),
            )

            val caido = sincronizador.enviar()
            val cortado = sincronizador.enviar()

            assertEquals(ResultadoEnvio.REINTENTAR, caido)
            assertEquals(ResultadoEnvio.REINTENTAR, cortado)
            assertEquals(2, pendientes().size)
        }

    @Test
    fun `un lote aplicado cuya respuesta se perdio se reenvia y el servidor lo confirma`() =
        runBlocking {
            conSesion()
            registrarFiados(1)
            val ids = pendientes().map { it.id }
            // El servidor aplica, pero la respuesta no llega.
            servidor.enqueue(
                MockResponse
                    .Builder()
                    .onResponseStart(SocketEffect.CloseSocket())
                    .build(),
            )

            assertEquals(ResultadoEnvio.REINTENTAR, sincronizador.enviar())
            servidorQueConfirma()
            assertEquals(ResultadoEnvio.COMPLETO, sincronizador.enviar())

            assertEquals(ids, aplicadas)
            assertEquals(emptyList(), pendientes())
        }

    @Test
    fun `un lote rechazado detiene el envio sin perder nada`() =
        runBlocking {
            conSesion()
            registrarFiados(1)
            servidor.enqueue(respuesta(422, """{"codigo":"referencia_inexistente","mensaje":"...","operacion":"x"}"""))

            assertEquals(ResultadoEnvio.RECHAZADO, sincronizador.enviar())

            assertEquals(2, pendientes().size)
        }

    @Test
    fun `si otro telefono tomo la sesion, deja de enviar y conserva la cola`() =
        runBlocking {
            conSesion()
            registrarFiados(1)
            servidor.enqueue(respuesta(401, """{"codigo":"sesion_reemplazada","mensaje":"..."}"""))

            assertEquals(ResultadoEnvio.SIN_SESION, sincronizador.enviar())

            assertEquals(2, pendientes().size)
            assertNull(almacen.actual())
            assertTrue(almacen.terminadaDesdeFuera.first())
        }

    @Test
    fun `una confirmacion que no incluye lo enviado no avanza la cola`() =
        runBlocking {
            conSesion()
            registrarFiados(1)
            servidor.enqueue(respuesta(200, """{"aplicadas":["otra"]}"""))

            assertEquals(ResultadoEnvio.REINTENTAR, sincronizador.enviar())

            assertEquals(2, pendientes().size)
        }

    @Test
    fun `lo registrado mientras se envia tambien sale`() =
        runBlocking {
            conSesion()
            registrarFiados(1)
            var primera = true
            servidor.dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse {
                        val ids = lote(request).map { it["id"]!!.jsonPrimitive.content }
                        aplicadas += ids
                        if (primera) {
                            primera = false
                            runBlocking { registrarFiados(1) }
                        }
                        return respuesta(200, """{"aplicadas":${ids.joinToString(",", "[", "]") { "\"$it\"" }}}""")
                    }
                }

            assertEquals(ResultadoEnvio.COMPLETO, sincronizador.enviar())

            assertEquals(4, aplicadas.size)
            assertEquals(emptyList(), pendientes())
        }
}
