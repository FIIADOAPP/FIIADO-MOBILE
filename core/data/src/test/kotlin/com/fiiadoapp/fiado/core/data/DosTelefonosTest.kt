package com.fiiadoapp.fiado.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fiiadoapp.fiado.core.data.cuenta.SesionGuardada
import com.fiiadoapp.fiado.core.data.cuenta.almacenDePrueba
import com.fiiadoapp.fiado.core.data.cuenta.crearRetrofitConSesion
import com.fiiadoapp.fiado.core.data.sincronizacion.ApiSincronizacion
import com.fiiadoapp.fiado.core.data.sincronizacion.ProgramadorDeEnvio
import com.fiiadoapp.fiado.core.data.sincronizacion.Restaurador
import com.fiiadoapp.fiado.core.data.sincronizacion.ResultadoEnvio
import com.fiiadoapp.fiado.core.data.sincronizacion.Sincronizador
import com.fiiadoapp.fiado.domain.Cliente
import com.fiiadoapp.fiado.domain.Dinero
import com.fiiadoapp.fiado.domain.MetodoCobro
import com.fiiadoapp.fiado.domain.Movimiento
import com.fiiadoapp.fiado.domain.TipoMovimiento
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import mockwebserver3.SocketEffect
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

/**
 * Los casos difíciles de la sincronización (ADR-0003) en una sola historia con dos teléfonos reales
 * (Room, sesión cifrada, Retrofit, restaurador y sincronizador) contra una API simulada con las
 * mismas reglas que FIIADO-API: un solo dispositivo activo, operaciones idempotentes, anulación que
 * solo avanza y restauración por páginas.
 */
@RunWith(RobolectricTestRunner::class)
class DosTelefonosTest {
    @get:Rule
    val carpeta = TemporaryFolder()

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val api = ApiSimulada()
    private val servidor = MockWebServer().apply { dispatcher = api }
    private val telefonos = mutableListOf<Telefono>()

    @After
    fun detener() {
        telefonos.forEach { it.db.close() }
        servidor.close()
        alcance.cancel()
    }

    @Test
    fun `nada se pierde ni se duplica entre cortes, reenvios y un telefono desplazado`() =
        runBlocking {
            servidor.start()
            val a = telefono("a")

            // 1. A se usa sin cuenta y luego inicia sesión. El servidor aplica, pero las respuestas se pierden
            //    (OkHttp reintenta por su cuenta y también se pierden); la app reenvía después.
            val juana = Cliente(Uuid.random(), "Juana", null, ahora, ahora, null)
            a.clientes.guardar(juana)
            a.movimientos.guardar(movimiento(juana, TipoMovimiento.FIADO, 1000))
            a.iniciarSesion()
            api.perderRespuestas = true
            assertEquals(ResultadoEnvio.REINTENTAR, a.sincronizador.enviar())
            api.perderRespuestas = false
            assertEquals(ResultadoEnvio.COMPLETO, a.sincronizador.enviar())
            assertEquals(5, api.cantidad())

            // 2. Sin conexión, A cobra. No lo envía todavía.
            a.movimientos.guardar(movimiento(juana, TipoMovimiento.PAGO, 400))

            // 3. B es un teléfono nuevo: toma la cuenta y su restauración se corta a la mitad.
            val b = telefono("b")
            b.iniciarSesion()
            api.fallarPagina = 2
            assertEquals(ResultadoEnvio.REINTENTAR, b.sincronizador.enviar())
            assertEquals(ResultadoEnvio.COMPLETO, b.sincronizador.enviar())
            assertEquals(Dinero(1000), b.saldo(juana))
            b.movimientos.guardar(movimiento(juana, TipoMovimiento.FIADO, 300))
            assertEquals(ResultadoEnvio.COMPLETO, b.sincronizador.enviar())

            // 4. A intenta enviar: su sesión fue reemplazada. Deja de enviar y conserva el cobro.
            assertEquals(ResultadoEnvio.SIN_SESION, a.sincronizador.enviar())
            assertEquals(1, a.pendientes())

            // 5. A vuelve: restaura lo de B y sube su cobro.
            a.iniciarSesion()
            assertEquals(ResultadoEnvio.COMPLETO, a.sincronizador.enviar())
            assertEquals(Dinero(900), a.saldo(juana))
            assertEquals(listOf(-400L, 300L, 1000L), api.montos())
            assertEquals(1, api.clientes())

            // 6. B vuelve y queda igual que A, sin duplicados.
            b.iniciarSesion()
            assertEquals(ResultadoEnvio.COMPLETO, b.sincronizador.enviar())
            assertEquals(Dinero(900), b.saldo(juana))
            assertEquals(
                1,
                b.clientes
                    .observarActivos("")
                    .first()
                    .size,
            )
            assertEquals(
                3,
                b.movimientos
                    .observarDeCliente(juana.id)
                    .first()
                    .size,
            )
            assertEquals(0, b.pendientes())
        }

    private val ahora: Instant = Instant.parse("2026-11-20T15:00:00.123Z")

    private fun movimiento(
        cliente: Cliente,
        tipo: TipoMovimiento,
        centimos: Long,
    ) = Movimiento(
        Uuid.random(),
        cliente.id,
        tipo,
        Dinero(centimos),
        null,
        LocalDate.of(2026, 11, 20),
        ahora,
        null,
        MetodoCobro.EFECTIVO.id.takeIf { tipo == TipoMovimiento.PAGO },
    )

    private fun telefono(nombre: String): Telefono = Telefono(nombre).also { telefonos += it }

    /** Un teléfono completo: su base, su sesión y su sincronización, como en la app. */
    private inner class Telefono(
        private val nombre: String,
    ) {
        val db: FiadoDatabase =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), FiadoDatabase::class.java)
                .allowMainThreadQueries()
                .addCallback(SembrarMetodosIniciales)
                .build()
        private val almacen = almacenDePrueba(File(carpeta.root, nombre).apply { mkdirs() }, alcance)
        private val sinProgramar = ProgramadorDeEnvio { }
        private val apiDelTelefono =
            crearRetrofitConSesion(servidor.url("/").toString(), almacen).create(ApiSincronizacion::class.java)
        private val restaurador = Restaurador(db, apiDelTelefono, GeneradorUuidV7(), sinProgramar)
        val sincronizador = Sincronizador(db.colaDao(), apiDelTelefono, almacen, restaurador)
        val clientes = RepositorioClientesRoom(db, GeneradorUuidV7(), sinProgramar)
        val movimientos = RepositorioMovimientosRoom(db, GeneradorUuidV7(), sinProgramar)

        /** Lo que hace la app al iniciar sesión cediendo el dispositivo. */
        suspend fun iniciarSesion() {
            val token = "token-$nombre-${Uuid.random()}"
            api.sesionActiva = token
            almacen.guardar(SesionGuardada(token, "refresco", CORREO, "Bodega"))
            restaurador.prepararRestauracion(CORREO)
        }

        suspend fun saldo(cliente: Cliente): Dinero =
            clientes
                .observarActivos("")
                .first()
                .single { it.cliente.id == cliente.id }
                .saldo

        suspend fun pendientes(): Int = db.colaDao().primeras(1_000).size
    }

    /** FIIADO-API en memoria, con las reglas del ADR-0003. Páginas de 2 para forzar varias. */
    private class ApiSimulada : Dispatcher() {
        @Volatile var sesionActiva: String? = null

        @Volatile var perderRespuestas = false

        /** Número de página de restauración que fallará una vez con 503. */
        @Volatile var fallarPagina: Int? = null

        private val aplicadas = mutableSetOf<String>()
        private val registros = sortedMapOf<Pair<Int, String>, JsonObject>(compareBy({ it.first }, { it.second }))
        private var paginasEntregadas = 0
        private val orden = listOf("metodo_cobro", "cliente", "movimiento")

        @Synchronized
        override fun dispatch(request: RecordedRequest): MockResponse {
            if (request.headers["Authorization"] != "Bearer $sesionActiva") {
                return json(401, """{"codigo":"sesion_reemplazada","mensaje":"..."}""")
            }
            return if (request.method == "POST") subir(request) else restaurar(request)
        }

        private fun subir(request: RecordedRequest): MockResponse {
            val operaciones =
                Json
                    .parseToJsonElement(checkNotNull(request.body).utf8())
                    .jsonObject["operaciones"]!!
                    .jsonArray
                    .map { it.jsonObject }
            for (operacion in operaciones) {
                val id = operacion.texto("id")
                if (!aplicadas.add(id)) continue
                val entidad = orden.indexOf(operacion.texto("entidad"))
                val datos = operacion["datos"]!!.jsonObject
                val clave = entidad to datos.texto("id")
                val anterior = registros[clave]
                registros[clave] =
                    if (anterior != null && anterior["anulado_en"] !is JsonNull && anterior.containsKey("anulado_en")) {
                        anterior
                    } else {
                        datos
                    }
            }
            val ids = operaciones.joinToString(",", "[", "]") { "\"${it.texto("id")}\"" }
            if (perderRespuestas) {
                return MockResponse
                    .Builder()
                    .onResponseStart(SocketEffect.CloseSocket())
                    .build()
            }
            return json(200, """{"aplicadas":$ids}""")
        }

        private fun restaurar(request: RecordedRequest): MockResponse {
            paginasEntregadas++
            if (fallarPagina == paginasEntregadas) {
                fallarPagina = null
                return json(503, """{"codigo":"no_disponible","mensaje":"..."}""")
            }
            val cursor =
                request.url
                    .queryParameter("cursor")
                    ?.split("|")
                    ?.let { (e, i) -> e.toInt() to i }
            val siguientes =
                registros.entries
                    .filter { cursor == null || compareValuesBy(it.key, cursor, { k -> k.first }, { k -> k.second }) > 0 }
                    .take(TAMANO_PAGINA)
            val hayMas = siguientes.size == TAMANO_PAGINA
            val lista: List<JsonElement> =
                siguientes.map { (clave, datos) ->
                    buildJsonObject {
                        put("entidad", orden[clave.first])
                        put("datos", datos)
                    }
                }
            val pagina =
                buildJsonObject {
                    put("registros", kotlinx.serialization.json.JsonArray(lista))
                    put("cursor", if (hayMas) JsonPrimitive(siguientes.last().key.let { "${it.first}|${it.second}" }) else JsonNull)
                }
            return json(200, pagina.toString())
        }

        @Synchronized
        fun cantidad(): Int = registros.size

        @Synchronized
        fun clientes(): Int = registros.keys.count { it.first == 1 }

        /** Montos vigentes con signo (pagos en negativo), ordenados. */
        @Synchronized
        fun montos(): List<Long> =
            registros
                .filterKeys { it.first == 2 }
                .values
                .filter { it["anulado_en"] is JsonNull }
                .map { m -> m["monto"]!!.jsonPrimitive.content.toLong() * if (m.texto("tipo") == "pago") -1 else 1 }
                .sorted()

        private fun JsonObject.texto(campo: String) = this[campo]!!.jsonPrimitive.content

        private fun json(
            codigo: Int,
            cuerpo: String,
        ) = MockResponse
            .Builder()
            .code(codigo)
            .addHeader("Content-Type", "application/json")
            .body(cuerpo)
            .build()

        private companion object {
            const val TAMANO_PAGINA = 2
        }
    }

    private companion object {
        const val CORREO = "rosa@bodega.pe"
    }
}
