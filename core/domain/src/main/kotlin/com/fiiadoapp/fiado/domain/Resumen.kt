package com.fiiadoapp.fiado.domain

import java.time.LocalDate
import java.time.YearMonth
import kotlin.uuid.Uuid

/** Saldo de un cliente: fiados vigentes menos pagos vigentes. Negativo es saldo a favor. */
fun Iterable<Movimiento>.saldo(): Dinero = filter { it.estaVigente }.map { it.efectoEnSaldo }.sumar()

private fun Iterable<Dinero>.sumar(): Dinero = fold(Dinero.CERO, Dinero::plus)

/**
 * Lo fiado y lo cobrado hoy y en el mes (y lo cobrado por cada método), y la deuda pendiente de todos los clientes.
 * El saldo a favor de un cliente no descuenta lo que deben los demás.
 */
data class Resumen(
    val fiadoHoy: Dinero,
    val cobradoHoy: Dinero,
    val fiadoMes: Dinero,
    val cobradoMes: Dinero,
    val porCobrar: Dinero,
    val cobradoHoyPorMetodo: Map<Uuid, Dinero>,
    val cobradoMesPorMetodo: Map<Uuid, Dinero>,
) {
    companion object {
        fun calcular(
            movimientos: Iterable<Movimiento>,
            hoy: LocalDate,
        ): Resumen {
            val vigentes = movimientos.filter { it.estaVigente }
            val mes = YearMonth.from(hoy)
            val deHoy = vigentes.filter { it.fecha == hoy }
            val delMes = vigentes.filter { YearMonth.from(it.fecha) == mes }
            return Resumen(
                fiadoHoy = deHoy.total(TipoMovimiento.FIADO),
                cobradoHoy = deHoy.total(TipoMovimiento.PAGO),
                fiadoMes = delMes.total(TipoMovimiento.FIADO),
                cobradoMes = delMes.total(TipoMovimiento.PAGO),
                porCobrar =
                    vigentes
                        .groupBy { it.clienteId }
                        .values
                        .map { it.saldo() }
                        .filter { it.esPositivo }
                        .sumar(),
                cobradoHoyPorMetodo = deHoy.cobradoPorMetodo(),
                cobradoMesPorMetodo = delMes.cobradoPorMetodo(),
            )
        }

        private fun List<Movimiento>.total(tipo: TipoMovimiento) = filter { it.tipo == tipo }.map { it.monto }.sumar()

        private fun List<Movimiento>.cobradoPorMetodo(): Map<Uuid, Dinero> =
            filter { it.tipo == TipoMovimiento.PAGO }
                .groupBy { checkNotNull(it.metodoCobroId) }
                .mapValues { (_, pagos) -> pagos.map { it.monto }.sumar() }
    }
}
