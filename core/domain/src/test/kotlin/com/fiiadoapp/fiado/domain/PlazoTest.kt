package com.fiiadoapp.fiado.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class PlazoTest {
    private val ahora = Instant.parse("2026-11-21T15:00:00Z")
    private val hoy = LocalDate.of(2026, 11, 21)
    private val juana = Cliente(Uuid.random(), "Juana", null, ahora, ahora, null)

    private fun registrar(
        tipo: TipoMovimiento,
        vence: LocalDate?,
        fecha: LocalDate = hoy,
    ) = Movimiento.registrar(
        Uuid.random(),
        juana,
        tipo,
        Dinero(1000),
        MetodoCobro.EFECTIVO,
        null,
        fecha,
        hoy,
        ahora,
        vence,
    )

    @Test
    fun `un fiado guarda su plazo`() {
        val resultado = registrar(TipoMovimiento.FIADO, hoy.plusDays(1))

        assertEquals(hoy.plusDays(1), (resultado as Resultado.Exito).valor.vence)
    }

    @Test
    fun `el plazo puede ser el mismo dia pero no antes`() {
        assertEquals(hoy, (registrar(TipoMovimiento.FIADO, hoy) as Resultado.Exito).valor.vence)
        assertEquals(
            Resultado.Fallo(ErrorMovimiento.PlazoAntesDeLaFecha),
            registrar(TipoMovimiento.FIADO, hoy.minusDays(2), fecha = hoy.minusDays(1)),
        )
    }

    @Test
    fun `un pago nunca lleva plazo`() {
        assertNull((registrar(TipoMovimiento.PAGO, hoy.plusDays(3)) as Resultado.Exito).valor.vence)
        assertThrows<IllegalArgumentException> {
            Movimiento(Uuid.random(), juana.id, TipoMovimiento.PAGO, Dinero(1), null, hoy, ahora, null, MetodoCobro.EFECTIVO.id, hoy)
        }
    }

    @Test
    fun `los atajos se cuentan desde el dia del fiado`() {
        val fecha = LocalDate.of(2026, 2, 26)

        assertEquals(LocalDate.of(2026, 2, 27), AtajoDePlazo.MANANA.desde(fecha))
        assertEquals(LocalDate.of(2026, 3, 1), AtajoDePlazo.TRES_DIAS.desde(fecha))
        assertEquals(LocalDate.of(2026, 3, 5), AtajoDePlazo.UNA_SEMANA.desde(fecha))
        assertEquals(LocalDate.of(2026, 2, 28), AtajoDePlazo.FIN_DE_MES.desde(fecha))
    }
}
