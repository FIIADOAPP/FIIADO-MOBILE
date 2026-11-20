package com.fiiadoapp.fiado.core.data

import com.fiiadoapp.fiado.core.data.sincronizacion.OperacionPendienteEntity
import com.fiiadoapp.fiado.domain.MetodoCobro
import com.fiiadoapp.fiado.domain.TipoMovimiento.FIADO
import com.fiiadoapp.fiado.domain.TipoMovimiento.PAGO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ColaDePendientesTest : BaseDeDatosDePrueba() {
    private suspend fun cola(): List<OperacionPendienteEntity> = db.colaDao().primeras(100)

    private fun datos(operacion: OperacionPendienteEntity): JsonObject = Json.parseToJsonElement(operacion.datos).jsonObject

    @Test
    fun `cada cambio deja una operacion pendiente en el orden en que ocurrio`() =
        runTest {
            val rosa = cliente("Doña Rosa", telefono = "987654321")
            val yape = MetodoCobro.YAPE.copy(nombre = "Yape negocio")

            clientes.guardar(rosa)
            metodos.guardar(yape)
            movimientos.guardar(movimiento(rosa, FIADO, 1250, nota = "arroz"))
            movimientos.guardar(movimiento(rosa, PAGO, 500, metodo = yape))

            val operaciones = cola()
            assertEquals(listOf("cliente", "metodo_cobro", "movimiento", "movimiento"), operaciones.map { it.entidad })
            assertEquals(4, operaciones.map { it.id }.distinct().size)
            assertTrue(operaciones.zipWithNext().all { (a, b) -> a.orden < b.orden })
            assertEquals(4, enviosProgramados)
        }

    @Test
    fun `la foto usa el formato de la API con todos los campos`() =
        runTest {
            val rosa = cliente("Doña Rosa", telefono = "987654321")
            clientes.guardar(rosa)
            val pago = movimiento(rosa, PAGO, 500)
            movimientos.guardar(pago)

            val (fotoCliente, fotoPago) = cola().map(::datos)

            assertEquals(
                Json.parseToJsonElement(
                    """{"id":"${rosa.id}","nombre":"Doña Rosa","telefono":"+51987654321",
                    "creado_en":"2026-11-15T15:30:00.123Z","actualizado_en":"2026-11-15T15:30:00.123Z","archivado_en":null}""",
                ),
                fotoCliente,
            )
            assertEquals(
                Json.parseToJsonElement(
                    """{"id":"${pago.id}","cliente_id":"${rosa.id}","tipo":"pago","monto":500,"nota":null,
                    "fecha":"2026-11-15","registrado_en":"2026-11-15T15:30:00.123Z","anulado_en":null,
                    "metodo_cobro_id":"${MetodoCobro.EFECTIVO.id}"}""",
                ),
                fotoPago,
            )
        }

    @Test
    fun `anular deja otra operacion con la foto anulada`() =
        runTest {
            val rosa = cliente("Doña Rosa")
            clientes.guardar(rosa)
            val fiado = movimiento(rosa, FIADO, 1250)
            movimientos.guardar(fiado)

            movimientos.guardar(fiado.anular(ahora.plusSeconds(60)))

            val ultima = datos(cola().last())
            assertEquals("\"2026-11-15T15:31:00.123Z\"", ultima["anulado_en"].toString())
        }

    @Test
    fun `si el registro no se guarda tampoco queda la operacion`() =
        runTest {
            val huerfano = movimiento(cliente("Nadie"), FIADO, 100)

            assertFailsWith<Exception> { movimientos.guardar(huerfano) }

            assertEquals(emptyList(), cola())
            assertNull(movimientos.obtener(huerfano.id))
            assertEquals(0, enviosProgramados)
        }

    @Test
    fun `los metodos iniciales sembrados no entran a la cola`() =
        runTest {
            assertEquals(MetodoCobro.INICIALES.size, metodos.observarTodos().first().size)
            assertEquals(emptyList(), cola())
        }
}
