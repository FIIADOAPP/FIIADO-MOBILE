package com.fiiadoapp.fiado.domain

import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class ProrrogaTest {
    private val ahora = Instant.parse("2026-11-21T15:00:00Z")
    private val hoy = LocalDate.of(2026, 11, 21)
    private val clienteId = Uuid.random()
    private val fiado =
        Movimiento(Uuid.random(), clienteId, TipoMovimiento.FIADO, Dinero(1000), null, hoy, ahora, null, vence = hoy.plusDays(1))

    private fun dar(
        vence: LocalDate,
        movimiento: Movimiento = fiado,
        previas: List<Prorroga> = emptyList(),
    ) = Prorroga.dar(Uuid.random(), movimiento, previas, vence, ahora)

    @Test
    fun `alarga el plazo de un fiado con plazo`() {
        val resultado = dar(hoy.plusDays(5)) as Resultado.Exito

        assertEquals(fiado.id, resultado.valor.movimientoId)
        assertEquals(hoy.plusDays(5), plazoVigente(fiado, listOf(resultado.valor)))
    }

    @Test
    fun `solo hacia adelante desde el plazo vigente`() {
        val primera = (dar(hoy.plusDays(5)) as Resultado.Exito).valor

        assertEquals(Resultado.Fallo(ErrorProrroga.NoAlarga), dar(hoy.plusDays(1)))
        assertEquals(Resultado.Fallo(ErrorProrroga.NoAlarga), dar(hoy.plusDays(5), previas = listOf(primera)))
        assertEquals(hoy.plusDays(9), ((dar(hoy.plusDays(9), previas = listOf(primera))) as Resultado.Exito).valor.vence)
    }

    @Test
    fun `no aplica a un fiado sin plazo, a un pago ni a un anulado`() {
        val pago = Movimiento(Uuid.random(), clienteId, TipoMovimiento.PAGO, Dinero(1), null, hoy, ahora, null, MetodoCobro.EFECTIVO.id)

        assertEquals(Resultado.Fallo(ErrorProrroga.SinPlazo), dar(hoy.plusDays(5), fiado.copy(vence = null)))
        assertEquals(Resultado.Fallo(ErrorProrroga.SinPlazo), dar(hoy.plusDays(5), pago))
        assertEquals(Resultado.Fallo(ErrorProrroga.Anulado), dar(hoy.plusDays(5), fiado.anular(ahora)))
    }

    @Test
    fun `el plazo vigente ignora las prorrogas de otros fiados`() {
        val ajena = Prorroga(Uuid.random(), Uuid.random(), hoy.plusDays(30), ahora)

        assertEquals(hoy.plusDays(1), plazoVigente(fiado, listOf(ajena)))
        assertNull(plazoVigente(fiado.copy(vence = null), listOf(ajena)))
    }
}
