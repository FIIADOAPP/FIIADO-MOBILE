package com.fiiadoapp.fiado.domain

import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class PuntualidadTest {
    private val lima: ZoneId = ZoneId.of("America/Lima")
    private val clienteId = Uuid.random()
    private val dia1: LocalDate = LocalDate.of(2026, 11, 1)
    private var orden = 0L

    private fun dia(n: Int): LocalDate = dia1.plusDays(n - 1L)

    /** Instante a media mañana de ese día en Lima; el contador mantiene el orden de registro. */
    private fun momento(fecha: LocalDate): Instant =
        fecha
            .atTime(10, 0)
            .atZone(lima)
            .toInstant()
            .plusMillis(orden++)

    private fun fiado(
        dia: Int,
        soles: Long,
        vence: Int? = null,
    ) = Movimiento(
        Uuid.random(),
        clienteId,
        TipoMovimiento.FIADO,
        Dinero(soles * 100),
        null,
        dia(dia),
        momento(dia(dia)),
        null,
        vence = vence?.let(::dia),
    )

    private fun pago(
        dia: Int,
        soles: Long,
    ) = Movimiento(
        Uuid.random(),
        clienteId,
        TipoMovimiento.PAGO,
        Dinero(soles * 100),
        null,
        dia(dia),
        momento(dia(dia)),
        null,
        MetodoCobro.EFECTIVO.id,
    )

    private fun prorroga(
        fiado: Movimiento,
        dadaEl: Int,
        vence: Int,
    ) = Prorroga(Uuid.random(), fiado.id, dia(vence), momento(dia(dadaEl)))

    private fun calcular(
        vararg movimientos: Movimiento,
        prorrogas: List<Prorroga> = emptyList(),
        hoy: Int = 30,
    ) = Puntualidad.calcular(movimientos.toList(), prorrogas, dia(hoy), lima)

    @Test
    fun `cada pago cubre primero el fiado mas antiguo`() {
        val primero = fiado(1, 10)
        val segundo = fiado(2, 10)

        val resultado = calcular(primero, segundo, pago(5, 15))

        assertEquals(dia(5), resultado.estadoDe(primero.id)?.pagadoEl)
        assertEquals(Dinero(500), resultado.estadoDe(segundo.id)?.pendiente)
        assertNull(resultado.estadoDe(segundo.id)?.pagadoEl)
    }

    @Test
    fun `lo que sobra queda a favor y paga el siguiente fiado el dia que se fia`() {
        val antes = fiado(1, 10)
        val despues = fiado(8, 5)

        val resultado = calcular(antes, pago(3, 15), despues)

        assertEquals(dia(3), resultado.estadoDe(antes.id)?.pagadoEl)
        assertEquals(dia(8), resultado.estadoDe(despues.id)?.pagadoEl)
    }

    @Test
    fun `un movimiento anulado no cuenta`() {
        val deuda = fiado(1, 10)
        val anulado = pago(2, 10).anular(momento(dia(2)))

        assertNull(calcular(deuda, anulado).estadoDe(deuda.id)?.pagadoEl)
    }

    @Test
    fun `el atraso se cuenta desde el plazo hasta el pago y un dia es tolerado`() {
        val aTiempo = fiado(1, 10, vence = 2)
        val unDia = fiado(3, 10, vence = 4)
        val tresDias = fiado(6, 10, vence = 7)

        val resultado = calcular(aTiempo, pago(2, 10), unDia, pago(5, 10), tresDias, pago(10, 10))

        assertEquals(listOf(0L, 1L, 3L), listOf(aTiempo, unDia, tresDias).map { resultado.estadoDe(it.id)?.atraso })
        assertEquals(66, resultado.porcentajeATiempo)
        assertEquals(4.0 / 3, resultado.atrasoPromedio)
        assertEquals(NivelPuntualidad.PUNTUAL, resultado.nivel)
    }

    @Test
    fun `prorrogar un fiado ya vencido no borra el atraso que llevaba`() {
        val deuda = fiado(1, 10, vence = 2)
        // Vencía el 2; el 5 le dan hasta el 10 y paga el 12: 3 días antes de prorrogar y 2 después.
        val resultado = calcular(deuda, pago(12, 10), prorrogas = listOf(prorroga(deuda, dadaEl = 5, vence = 10)))

        assertEquals(5L, resultado.estadoDe(deuda.id)?.atraso)
        assertEquals(dia(10), resultado.estadoDe(deuda.id)?.plazo)
    }

    @Test
    fun `prorrogar antes de vencer no genera atraso`() {
        val deuda = fiado(1, 10, vence = 3)

        val resultado = calcular(deuda, pago(6, 10), prorrogas = listOf(prorroga(deuda, dadaEl = 2, vence = 7)))

        assertEquals(0L, resultado.estadoDe(deuda.id)?.atraso)
    }

    @Test
    fun `sin al menos dos fiados con plazo pagados ni nada vencido no se juzga`() {
        val resultado = calcular(fiado(1, 10, vence = 2), pago(9, 10), fiado(20, 5))

        assertEquals(NivelPuntualidad.SIN_HISTORIAL, resultado.nivel)
        assertEquals(8.0, resultado.diasPromedioParaPagar)
    }

    @Test
    fun `lo vencido cuenta aunque no tenga historial`() {
        assertEquals(NivelPuntualidad.PUNTUAL, calcular(fiado(1, 10, vence = 2), hoy = 3).nivel)
        assertEquals(NivelPuntualidad.SE_DEMORA, calcular(fiado(1, 10, vence = 2), hoy = 4).nivel)
        val cuidado = calcular(fiado(1, 10, vence = 2), fiado(3, 5), hoy = 9)
        assertEquals(NivelPuntualidad.CUIDADO, cuidado.nivel)
        assertEquals(Dinero(1000), cuidado.vencido)
        assertEquals(7L, cuidado.diasVencido)
    }

    @Test
    fun `un atraso promedio de 2 a 6 dias es se demora y de 7 o mas es cuidado`() {
        val demora = calcular(fiado(1, 10, vence = 2), pago(5, 10), fiado(6, 10, vence = 7), pago(10, 10))
        val cuidado = calcular(fiado(1, 10, vence = 2), pago(10, 10), fiado(11, 10, vence = 12), pago(20, 10))

        assertEquals(NivelPuntualidad.SE_DEMORA, demora.nivel)
        assertEquals(NivelPuntualidad.CUIDADO, cuidado.nivel)
    }

    @Test
    fun `solo cuentan los ultimos diez fiados pagados`() {
        val viejos = (1..5).flatMap { listOf(fiado(it, 10, vence = it), pago(it + 20, 10)) }
        val recientes = (1..10).flatMap { listOf(fiado(it + 25, 10, vence = it + 25), pago(it + 25, 10)) }

        val resultado = calcular(*(viejos + recientes).toTypedArray(), hoy = 40)

        assertEquals(0.0, resultado.atrasoPromedio)
        assertEquals(NivelPuntualidad.PUNTUAL, resultado.nivel)
    }

    @Test
    fun `el proximo vencimiento es el plazo mas cercano de lo que aun no vence`() {
        val resultado = calcular(fiado(1, 10, vence = 9), fiado(2, 10, vence = 6), fiado(3, 10, vence = 4), hoy = 5)

        assertEquals(dia(6), resultado.proximoVencimiento)
        assertEquals(1L, resultado.diasVencido)
    }

    @Test
    fun `sugiere el plazo que suele tardar solo con dos fiados pagados o mas`() {
        val unPagado = calcular(fiado(1, 10), pago(4, 10))
        val dosPagados = calcular(fiado(1, 10), pago(4, 10), fiado(5, 10), pago(10, 10))
        val enElDia = calcular(fiado(1, 10), pago(1, 10), fiado(2, 10), pago(2, 10))

        assertNull(unPagado.diasSugeridos)
        assertEquals(4, dosPagados.diasSugeridos)
        assertEquals(1, enElDia.diasSugeridos)
    }

    @Test
    fun `el negocio cuenta clientes por nivel, promedia sus pagos y ordena a los de cuidado por lo vencido`() {
        val puntual = calcular(fiado(1, 10, vence = 2), pago(2, 10), fiado(3, 10, vence = 4), pago(5, 10))
        val debePoco = calcular(fiado(1, 10, vence = 2), hoy = 20)
        val debeMucho = calcular(fiado(1, 50, vence = 2), hoy = 20)
        val sinMovimientos = calcular(fiado(25, 10))
        val ids = List(4) { Uuid.random() }

        val negocio = PuntualidadDelNegocio.de(ids.zip(listOf(puntual, debePoco, debeMucho, sinMovimientos)).toMap())

        assertEquals(1, negocio.clientesEn(NivelPuntualidad.PUNTUAL))
        assertEquals(2, negocio.clientesEn(NivelPuntualidad.CUIDADO))
        assertEquals(0, negocio.clientesEn(NivelPuntualidad.SE_DEMORA))
        assertEquals(listOf(ids[2], ids[1]), negocio.enCuidado)
        assertEquals(1.5, negocio.diasPromedioParaPagar)
    }
}
