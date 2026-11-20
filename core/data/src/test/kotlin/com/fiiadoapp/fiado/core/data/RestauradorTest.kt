package com.fiiadoapp.fiado.core.data

import com.fiiadoapp.fiado.core.data.cuenta.AlmacenSesion
import com.fiiadoapp.fiado.core.data.cuenta.SesionGuardada
import com.fiiadoapp.fiado.core.data.cuenta.almacenDePrueba
import com.fiiadoapp.fiado.core.data.cuenta.crearRetrofitConSesion
import com.fiiadoapp.fiado.core.data.sincronizacion.ApiSincronizacion
import com.fiiadoapp.fiado.core.data.sincronizacion.Restaurador
import com.fiiadoapp.fiado.core.data.sincronizacion.ResultadoEnvio
import com.fiiadoapp.fiado.core.data.sincronizacion.Sincronizador
import com.fiiadoapp.fiado.domain.Cliente
import com.fiiadoapp.fiado.domain.MetodoCobro
import com.fiiadoapp.fiado.domain.Movimiento
import com.fiiadoapp.fiado.domain.Telefono
import com.fiiadoapp.fiado.domain.TipoMovimiento.FIADO
import com.fiiadoapp.fiado.domain.TipoMovimiento.PAGO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** La restauración real (Retrofit y Room) contra un servidor simulado que responde como FIIADO-API. */
@RunWith(RobolectricTestRunner::class)
class RestauradorTest : BaseDeDatosDePrueba() {
    @get:Rule
    val carpeta = TemporaryFolder()

    private val servidor = MockWebServer()
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var almacen: AlmacenSesion
    private lateinit var restaurador: Restaurador
    private lateinit var sincronizador: Sincronizador
    private var envios = 0

    @Before
    fun preparar() {
        servidor.start()
        almacen = almacenDePrueba(carpeta.root, alcance)
        runBlocking { almacen.guardar(SesionGuardada("acceso", "refresco", CORREO, "Bodega")) }
        val api = crearRetrofitConSesion(servidor.url("/").toString(), almacen).create(ApiSincronizacion::class.java)
        restaurador = Restaurador(db, api, GeneradorUuidV7()) { envios++ }
        sincronizador = Sincronizador(db.colaDao(), api, almacen, restaurador)
    }

    @After
    fun detener() {
        servidor.close()
        alcance.cancel()
    }

    private fun pagina(
        cursor: String?,
        vararg registros: String,
    ) = servidor.enqueue(
        MockResponse
            .Builder()
            .code(200)
            .addHeader("Content-Type", "application/json")
            .body("""{"registros":[${registros.joinToString(",")}],"cursor":${cursor?.let { "\"$it\"" } ?: "null"}}""")
            .build(),
    )

    private fun error(codigo: Int) =
        servidor.enqueue(
            MockResponse
                .Builder()
                .code(codigo)
                .body("""{"codigo":"x","mensaje":"x"}""")
                .build(),
        )

    private fun metodoJson(
        metodo: MetodoCobro,
        nombre: String = metodo.nombre,
    ) = """{"entidad":"metodo_cobro","datos":{"id":"${metodo.id}","nombre":"$nombre",
        "creado_en":"2026-11-01T10:00:00+00:00","actualizado_en":"2026-11-02T10:00:00.250000Z","archivado_en":null}}"""

    private fun clienteJson(
        cliente: Cliente,
        nombre: String = cliente.nombre,
    ) = """{"entidad":"cliente","datos":{"id":"${cliente.id}","nombre":"$nombre","telefono":"+51987654321",
        "creado_en":"2026-11-01T10:00:00Z","actualizado_en":"2026-11-01T10:00:00Z","archivado_en":null}}"""

    private fun movimientoJson(
        movimiento: Movimiento,
        anuladoEn: String? = null,
    ) = """{"entidad":"movimiento","datos":{"id":"${movimiento.id}","cliente_id":"${movimiento.clienteId}",
        "tipo":"${movimiento.tipo.name.lowercase()}","monto":${movimiento.monto.centimos},"nota":${movimiento.nota?.let {
        "\"$it\""
    } ?: "null"},
        "fecha":"${movimiento.fecha}","registrado_en":"${movimiento.registradoEn}","anulado_en":${anuladoEn?.let { "\"$it\"" } ?: "null"},
        "metodo_cobro_id":${movimiento.metodoCobroId?.let { "\"$it\"" } ?: "null"}}}"""

    private suspend fun cola() = db.colaDao().primeras(1_000)

    private suspend fun entidadesEnCola() =
        cola().map {
            val datos = Json.parseToJsonElement(it.datos).jsonObject
            it.entidad to datos["id"]!!.jsonPrimitive.content
        }

    @Test
    fun `trae la libreta del servidor por paginas y la deja como alla`() =
        runBlocking {
            val juana = cliente("Juana")
            val deuda = movimiento(juana, FIADO, 1250, nota = "arroz")
            pagina("c1", metodoJson(MetodoCobro.EFECTIVO, "Caja"), clienteJson(juana))
            pagina(null, movimientoJson(deuda))

            assertNull(restaurador.restaurarSiFalta(CORREO))

            assertEquals("/sincronizacion/restauracion?limite=500", servidor.takeRequest().target)
            assertEquals("/sincronizacion/restauracion?cursor=c1&limite=500", servidor.takeRequest().target)
            assertEquals(
                "Caja",
                metodos
                    .observarTodos()
                    .first()
                    .first { it.id == MetodoCobro.EFECTIVO.id }
                    .nombre,
            )
            assertEquals(
                juana.copy(telefono = Telefono.desdeTexto("987654321"), creadoEn = HACE, actualizadoEn = HACE),
                clientes.observar(juana.id).first(),
            )
            assertEquals(deuda, movimientos.obtener(deuda.id))
            assertTrue(db.restauracionDao().vinculo()!!.restaurada)
        }

    @Test
    fun `la primera vez reemplaza la cola por lo que solo existe en el telefono`() =
        runBlocking {
            val delServidor = cliente("Del servidor")
            val local = cliente("Solo aquí")
            clientes.guardar(local)
            clientes.guardar(local.copy(nombre = "Solo aquí, editado"))
            val fiadoLocal = movimiento(local, FIADO, 300)
            movimientos.guardar(fiadoLocal)
            pagina(null, metodoJson(MetodoCobro.EFECTIVO), clienteJson(delServidor))

            assertNull(restaurador.restaurarSiFalta(CORREO))

            assertEquals(
                listOf(
                    "metodo_cobro" to MetodoCobro.YAPE.id.toString(),
                    "metodo_cobro" to MetodoCobro.PLIN.id.toString(),
                    "cliente" to local.id.toString(),
                    "movimiento" to fiadoLocal.id.toString(),
                ),
                entidadesEnCola(),
            )
            assertEquals("Solo aquí, editado", clientes.observar(local.id).first()?.nombre)
        }

    @Test
    fun `al volver a vincular conserva lo pendiente y agrega lo que falte`() =
        runBlocking {
            restaurador.prepararRestauracion(CORREO)
            pagina(null)
            restaurador.restaurarSiFalta(CORREO)
            val juana = cliente("Juana")
            clientes.guardar(juana)
            val antes = cola().map { it.id }
            restaurador.prepararRestauracion(CORREO)
            pagina(null, metodoJson(MetodoCobro.EFECTIVO), metodoJson(MetodoCobro.YAPE), metodoJson(MetodoCobro.PLIN))

            assertNull(restaurador.restaurarSiFalta(CORREO))

            val despues = cola()
            assertEquals(antes, despues.take(antes.size).map { it.id })
            assertEquals(listOf("cliente" to juana.id.toString()), entidadesEnCola().drop(antes.size))
        }

    @Test
    fun `la anulacion se conserva venga de donde venga`() =
        runBlocking {
            val juana = cliente("Juana")
            clientes.guardar(juana)
            val anuladoAqui = movimiento(juana, FIADO, 100)
            val anuladoAlla = movimiento(juana, PAGO, 50)
            movimientos.guardar(anuladoAqui.anular(ahora.plusSeconds(60)))
            movimientos.guardar(anuladoAlla)
            pagina(
                null,
                metodoJson(MetodoCobro.EFECTIVO),
                clienteJson(juana),
                movimientoJson(anuladoAqui),
                movimientoJson(anuladoAlla, anuladoEn = "2026-11-15T16:00:00Z"),
            )

            restaurador.restaurarSiFalta(CORREO)

            assertEquals(ahora.plusSeconds(60), movimientos.obtener(anuladoAqui.id)?.anuladoEn)
            assertEquals(Instant.parse("2026-11-15T16:00:00Z"), movimientos.obtener(anuladoAlla.id)?.anuladoEn)
        }

    @Test
    fun `si se corta a mitad continua desde la ultima pagina guardada`() =
        runBlocking {
            val juana = cliente("Juana")
            val pedro = cliente("Pedro")
            pagina("c1", metodoJson(MetodoCobro.EFECTIVO), clienteJson(juana))
            error(503)

            assertEquals(ResultadoEnvio.REINTENTAR, restaurador.restaurarSiFalta(CORREO))
            assertEquals("c1", db.restauracionDao().vinculo()?.cursor)
            assertEquals("Juana", clientes.observar(juana.id).first()?.nombre)

            pagina(null, clienteJson(pedro))
            assertNull(restaurador.restaurarSiFalta(CORREO))

            servidor.takeRequest()
            servidor.takeRequest()
            assertEquals("/sincronizacion/restauracion?cursor=c1&limite=500", servidor.takeRequest().target)
            assertEquals(
                listOf("metodo_cobro" to MetodoCobro.YAPE.id.toString(), "metodo_cobro" to MetodoCobro.PLIN.id.toString()),
                entidadesEnCola(),
            )
        }

    @Test
    fun `sin red, sin sesion o con datos invalidos no termina y no pierde nada`() =
        runBlocking {
            error(401)
            pagina(null, """{"entidad":"desconocida","datos":{}}""")

            val sinSesion = restaurador.restaurarSiFalta(CORREO)
            val invalido = restaurador.restaurarSiFalta(CORREO)

            assertEquals(ResultadoEnvio.SIN_SESION, sinSesion)
            assertEquals(ResultadoEnvio.RECHAZADO, invalido)
            assertEquals(false, db.restauracionDao().vinculo()?.restaurada)
        }

    @Test
    fun `el envio restaura antes de subir y la restauracion no se repite`() =
        runBlocking {
            val juana = cliente("Juana")
            clientes.guardar(juana)
            pagina(null, metodoJson(MetodoCobro.EFECTIVO), metodoJson(MetodoCobro.YAPE), metodoJson(MetodoCobro.PLIN))
            servidor.enqueue(
                MockResponse
                    .Builder()
                    .code(200)
                    .addHeader("Content-Type", "application/json")
                    .body("""{"aplicadas":[]}""")
                    .build(),
            )

            sincronizador.enviar()

            assertEquals("/sincronizacion/restauracion?limite=500", servidor.takeRequest().target)
            val envio = servidor.takeRequest()
            assertEquals("/sincronizacion/operaciones", envio.target)
            assertTrue(checkNotNull(envio.body).utf8().contains(juana.id.toString()))
        }

    @Test
    fun `preparar la restauracion la reinicia y pide un envio`() =
        runBlocking {
            restaurador.prepararRestauracion(CORREO)

            assertEquals(CORREO, restaurador.correoVinculado())
            assertEquals(1, envios)
            val vinculo = db.restauracionDao().vinculo()!!
            assertEquals(false, vinculo.restaurada)
            assertTrue(vinculo.primera)
        }

    private companion object {
        const val CORREO = "rosa@bodega.pe"
        val HACE: Instant = Instant.parse("2026-11-01T10:00:00Z")
    }
}
