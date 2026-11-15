package com.fiiadoapp.fiado.domain

import java.time.LocalDate
import java.time.YearMonth

/** Saldo de un cliente: fiados vigentes menos pagos vigentes. Negativo es saldo a favor. */
fun Iterable<Movimiento>.saldo(): Dinero = filter { it.estaVigente }.map { it.efectoEnSaldo }.sumar()

private fun Iterable<Dinero>.sumar(): Dinero = fold(Dinero.CERO, Dinero::plus)

/**
 * Lo fiado y lo cobrado hoy y en el mes, y la deuda pendiente de todos los clientes.
 * El saldo a favor de un cliente no descuenta lo que deben los demás.
 */
data class Resumen(
    val fiadoHoy: Dinero,
    val cobradoHoy: Dinero,
    val fiadoMes: Dinero,
    val cobradoMes: Dinero,
    val porCobrar: Dinero,
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
            )
        }

        private fun List<Movimiento>.total(tipo: TipoMovimiento) = filter { it.tipo == tipo }.map { it.monto }.sumar()
    }
}
