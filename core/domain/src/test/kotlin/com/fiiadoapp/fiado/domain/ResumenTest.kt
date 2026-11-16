package com.fiiadoapp.fiado.domain

import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class ResumenTest {
    private val hoy = LocalDate.of(2026, 11, 15)
    private val ahora = Instant.parse("2026-11-15T15:00:00Z")
    private val rosa = Uuid.random()
    private val pedro = Uuid.random()

    private fun movimiento(
        cliente: Uuid,
        tipo: TipoMovimiento,
        centimos: Long,
        fecha: LocalDate = hoy,
        metodo: MetodoCobro = MetodoCobro.EFECTIVO,
    ) = Movimiento(
        Uuid.random(),
        cliente,
        tipo,
        Dinero(centimos),
        null,
        fecha,
        ahora,
        anuladoEn = null,
        metodoCobroId = metodo.id.takeIf { tipo == TipoMovimiento.PAGO },
    )

    private fun fiado(
        cliente: Uuid,
        centimos: Long,
        fecha: LocalDate = hoy,
    ) = movimiento(cliente, TipoMovimiento.FIADO, centimos, fecha)

    private fun pago(
        cliente: Uuid,
        centimos: Long,
        fecha: LocalDate = hoy,
        metodo: MetodoCobro = MetodoCobro.EFECTIVO,
    ) = movimiento(cliente, TipoMovimiento.PAGO, centimos, fecha, metodo)

    @Test
    fun `el saldo es fiados menos pagos`() {
        assertEquals(Dinero(700), listOf(fiado(rosa, 1000), fiado(rosa, 200), pago(rosa, 500)).saldo())
    }

    @Test
    fun `el saldo ignora los movimientos anulados`() {
        val movimientos = listOf(fiado(rosa, 1000), fiado(rosa, 300).anular(ahora), pago(rosa, 200).anular(ahora))

        assertEquals(Dinero(1000), movimientos.saldo())
    }

    @Test
    fun `pagar de mas deja saldo a favor`() {
        assertEquals(Dinero(-300), listOf(fiado(rosa, 1000), pago(rosa, 1300)).saldo())
    }

    @Test
    fun `sin movimientos el saldo es cero`() {
        assertEquals(Dinero.CERO, emptyList<Movimiento>().saldo())
    }

    @Test
    fun `separa lo de hoy, lo del mes y lo de meses anteriores`() {
        val movimientos =
            listOf(
                fiado(rosa, 1000),
                pago(rosa, 400),
                fiado(rosa, 2000, fecha = LocalDate.of(2026, 11, 1)),
                pago(pedro, 300, fecha = LocalDate.of(2026, 11, 14)),
                fiado(pedro, 5000, fecha = LocalDate.of(2026, 10, 31)),
            )

        val resumen = Resumen.calcular(movimientos, hoy)

        assertEquals(Dinero(1000), resumen.fiadoHoy)
        assertEquals(Dinero(400), resumen.cobradoHoy)
        assertEquals(Dinero(3000), resumen.fiadoMes)
        assertEquals(Dinero(700), resumen.cobradoMes)
        assertEquals(Dinero(2600 + 4700), resumen.porCobrar)
    }

    @Test
    fun `el saldo a favor de un cliente no descuenta la deuda de otro`() {
        val movimientos = listOf(fiado(rosa, 1000), fiado(pedro, 500), pago(pedro, 800))

        assertEquals(Dinero(1000), Resumen.calcular(movimientos, hoy).porCobrar)
    }

    @Test
    fun `los anulados no cuentan en ningun total`() {
        val resumen = Resumen.calcular(listOf(fiado(rosa, 1000).anular(ahora), pago(rosa, 200).anular(ahora)), hoy)

        assertEquals(Resumen(Dinero.CERO, Dinero.CERO, Dinero.CERO, Dinero.CERO, Dinero.CERO, emptyMap(), emptyMap()), resumen)
    }

    @Test
    fun `desglosa lo cobrado por metodo hoy y en el mes`() {
        val movimientos =
            listOf(
                pago(rosa, 1000, metodo = MetodoCobro.YAPE),
                pago(pedro, 500, metodo = MetodoCobro.YAPE),
                pago(rosa, 300),
                pago(pedro, 2000, fecha = LocalDate.of(2026, 11, 2), metodo = MetodoCobro.PLIN),
                pago(rosa, 700, metodo = MetodoCobro.YAPE).anular(ahora),
                fiado(rosa, 9000),
            )

        val resumen = Resumen.calcular(movimientos, hoy)

        assertEquals(mapOf(MetodoCobro.YAPE.id to Dinero(1500), MetodoCobro.EFECTIVO.id to Dinero(300)), resumen.cobradoHoyPorMetodo)
        assertEquals(
            mapOf(MetodoCobro.YAPE.id to Dinero(1500), MetodoCobro.EFECTIVO.id to Dinero(300), MetodoCobro.PLIN.id to Dinero(2000)),
            resumen.cobradoMesPorMetodo,
        )
    }
}
