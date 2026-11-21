package com.fiiadoapp.fiado.feature.movimientos

import androidx.lifecycle.SavedStateHandle
import com.fiiadoapp.fiado.core.testing.LibretaFalsa
import com.fiiadoapp.fiado.core.testing.ReglaDispatcherPrincipal
import com.fiiadoapp.fiado.domain.AtajoDePlazo
import com.fiiadoapp.fiado.domain.Cliente
import com.fiiadoapp.fiado.domain.Dinero
import com.fiiadoapp.fiado.domain.ErrorMovimiento
import com.fiiadoapp.fiado.domain.MetodoCobro
import com.fiiadoapp.fiado.domain.Movimiento
import com.fiiadoapp.fiado.domain.TipoMovimiento
import com.fiiadoapp.fiado.domain.TipoMovimiento.FIADO
import com.fiiadoapp.fiado.domain.TipoMovimiento.PAGO
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

class RegistrarMovimientoViewModelTest {
    @get:Rule
    val dispatcher = ReglaDispatcherPrincipal()

    /** 15 de noviembre a las 23:30 en Lima, que en UTC ya es 16 de noviembre. */
    private val ahora = Instant.parse("2026-11-16T04:30:00Z")
    private val hoyEnLima = LocalDate.of(2026, 11, 15)
    private val reloj = Clock.fixed(ahora, ZoneId.of("America/Lima"))
    private val idNuevo = Uuid.random()
    private val libreta = LibretaFalsa()
    private val rosa = Cliente(Uuid.random(), "Doña Rosa", null, ahora, ahora, null)

    private fun viewModel(
        tipo: TipoMovimiento = FIADO,
        cliente: Cliente = rosa,
        vararg previos: Movimiento,
    ): RegistrarMovimientoViewModel {
        libreta.clientes.value = mapOf(cliente.id to cliente)
        libreta.movimientos.value = previos.associateBy { it.id }
        return RegistrarMovimientoViewModel(
            SavedStateHandle(mapOf("clienteId" to cliente.id.toString(), "tipo" to tipo.name)),
            libreta.repositorioClientes,
            libreta.repositorioMovimientos,
            libreta.repositorioMetodosCobro,
            { idNuevo },
            reloj,
        )
    }

    private fun registrado() = libreta.movimientos.value[idNuevo]

    @Test
    fun `carga el cliente, su saldo y la fecha de hoy en Lima`() =
        runTest {
            val previo = Movimiento(Uuid.random(), rosa.id, FIADO, Dinero(800), null, hoyEnLima, ahora, null)

            val estado = viewModel(PAGO, rosa, previo).estado.value

            assertEquals(PAGO, estado.tipo)
            assertEquals("Doña Rosa", estado.nombreCliente)
            assertEquals(Dinero(800), estado.saldo)
            assertEquals(hoyEnLima, estado.fecha)
            assertFalse(estado.cargando)
        }

    @Test
    fun `con el monto y un toque registra el fiado de hoy`() =
        runTest {
            val viewModel = viewModel()
            viewModel.cambiarMonto("12,5")

            viewModel.guardar()

            assertEquals(Movimiento(idNuevo, rosa.id, FIADO, Dinero(1250), null, hoyEnLima, ahora, null), registrado())
            assertEquals(idNuevo, viewModel.estado.value.registrado)
        }

    @Test
    fun `registra un pago con nota y fecha pasada`() =
        runTest {
            val viewModel = viewModel(PAGO)
            viewModel.cambiarMonto("20")
            viewModel.cambiarNota("  pagó con yape ")
            viewModel.cambiarFecha(hoyEnLima.minusDays(2))

            viewModel.guardar()

            val pago = registrado()
            assertEquals(PAGO, pago?.tipo)
            assertEquals(MetodoCobro.EFECTIVO.id, pago?.metodoCobroId)
            assertEquals("pagó con yape", pago?.nota)
            assertEquals(hoyEnLima.minusDays(2), pago?.fecha)
        }

    @Test
    fun `un monto ilegible o cero no se guarda`() =
        runTest {
            listOf("", "abc", "0", "0.00").forEach { texto ->
                val viewModel = viewModel()
                viewModel.cambiarMonto(texto)

                viewModel.guardar()

                assertTrue(viewModel.estado.value.montoInvalido, "monto '$texto'")
                assertNull(registrado())
            }
        }

    @Test
    fun `escribir de nuevo el monto borra el error`() =
        runTest {
            val viewModel = viewModel()
            viewModel.guardar()

            viewModel.cambiarMonto("5")

            assertFalse(viewModel.estado.value.montoInvalido)
        }

    @Test
    fun `una nota demasiado larga no se guarda`() =
        runTest {
            val viewModel = viewModel()
            viewModel.cambiarMonto("5")
            viewModel.cambiarNota("a".repeat(141))

            viewModel.guardar()

            assertEquals(ErrorMovimiento.NotaMuyLarga, viewModel.estado.value.error)
            assertNull(registrado())
        }

    @Test
    fun `no se registra para un cliente archivado`() =
        runTest {
            val viewModel = viewModel(cliente = rosa.copy(archivadoEn = ahora))
            viewModel.cambiarMonto("5")

            viewModel.guardar()

            assertEquals(ErrorMovimiento.ClienteArchivado, viewModel.estado.value.error)
            assertNull(registrado())
        }

    private fun pagoPrevio(
        metodo: MetodoCobro,
        segundos: Long,
    ) = Movimiento(Uuid.random(), rosa.id, PAGO, Dinero(100), null, hoyEnLima, ahora.plusSeconds(segundos), null, metodo.id)

    @Test
    fun `un pago ofrece los metodos activos y preselecciona el ultimo usado`() =
        runTest {
            val estado = viewModel(PAGO, rosa, pagoPrevio(MetodoCobro.PLIN, 0), pagoPrevio(MetodoCobro.YAPE, 60)).estado.value

            assertEquals(MetodoCobro.INICIALES, estado.metodos)
            assertEquals(MetodoCobro.YAPE.id, estado.metodoElegido)
        }

    @Test
    fun `sin pagos previos preselecciona el primer metodo`() =
        runTest {
            assertEquals(MetodoCobro.EFECTIVO.id, viewModel(PAGO).estado.value.metodoElegido)
        }

    @Test
    fun `si el ultimo metodo usado se archivo preselecciona el primero activo`() =
        runTest {
            libreta.metodos.value += MetodoCobro.YAPE.id to MetodoCobro.YAPE.copy(archivadoEn = ahora)

            val estado = viewModel(PAGO, rosa, pagoPrevio(MetodoCobro.YAPE, 0)).estado.value

            assertEquals(listOf(MetodoCobro.EFECTIVO, MetodoCobro.PLIN), estado.metodos)
            assertEquals(MetodoCobro.EFECTIVO.id, estado.metodoElegido)
        }

    @Test
    fun `el pago se guarda con el metodo elegido`() =
        runTest {
            val viewModel = viewModel(PAGO)
            viewModel.cambiarMonto("15")
            viewModel.elegirMetodo(MetodoCobro.PLIN.id)

            viewModel.guardar()

            assertEquals(MetodoCobro.PLIN.id, registrado()?.metodoCobroId)
        }

    @Test
    fun `un fiado no ofrece metodos ni guarda uno`() =
        runTest {
            val viewModel = viewModel(FIADO)
            viewModel.cambiarMonto("15")

            viewModel.guardar()

            assertEquals(emptyList(), viewModel.estado.value.metodos)
            assertNull(registrado()?.metodoCobroId)
        }

    @Test
    fun `un fiado se guarda con el plazo del atajo, contado desde su fecha`() =
        runTest {
            val viewModel = viewModel(FIADO)
            viewModel.cambiarMonto("10")
            viewModel.cambiarFecha(
                viewModel.estado.value.hoy
                    .minusDays(1),
            )

            viewModel.elegirAtajo(AtajoDePlazo.TRES_DIAS)
            viewModel.guardar()

            val guardado = checkNotNull(registrado())
            assertEquals(
                viewModel.estado.value.hoy
                    .plusDays(2),
                guardado.vence,
            )
        }

    @Test
    fun `otra fecha reemplaza al atajo y sin plazo lo quita`() =
        runTest {
            val viewModel = viewModel(FIADO)
            val hoy = viewModel.estado.value.hoy

            viewModel.elegirAtajo(AtajoDePlazo.MANANA)
            viewModel.elegirOtraFecha(hoy.plusDays(10))
            assertEquals(hoy.plusDays(10), viewModel.estado.value.vence)

            viewModel.elegirAtajo(null)
            assertNull(viewModel.estado.value.vence)
        }
}
