package com.fiiadoapp.fiado.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.uuid.Uuid

class MovimientoTest {
    private val ahora = Instant.parse("2026-11-15T15:00:00Z")
    private val despues = Instant.parse("2026-11-15T16:00:00Z")
    private val hoy = LocalDate.of(2026, 11, 15)
    private val cliente = assertIs<Resultado.Exito<Cliente>>(Cliente.crear(Uuid.random(), "Doña Rosa", null, ahora)).valor

    private fun registrar(
        tipo: TipoMovimiento = TipoMovimiento.FIADO,
        monto: Dinero = Dinero(1250),
        nota: String? = null,
        fecha: LocalDate = hoy,
        para: Cliente = cliente,
        metodo: MetodoCobro? = MetodoCobro.EFECTIVO,
    ) = Movimiento.registrar(Uuid.random(), para, tipo, monto, metodo, nota, fecha, hoy, ahora)

    private fun registrado(
        tipo: TipoMovimiento = TipoMovimiento.FIADO,
        monto: Dinero = Dinero(1250),
    ) = assertIs<Resultado.Exito<Movimiento>>(registrar(tipo, monto)).valor

    @Test
    fun `registra un fiado vigente del cliente`() {
        val movimiento = registrado()

        assertEquals(cliente.id, movimiento.clienteId)
        assertEquals(TipoMovimiento.FIADO, movimiento.tipo)
        assertEquals(Dinero(1250), movimiento.monto)
        assertEquals(hoy, movimiento.fecha)
        assertEquals(ahora, movimiento.registradoEn)
        assertEquals(true, movimiento.estaVigente)
    }

    @Test
    fun `un fiado suma deuda y un pago la resta`() {
        assertEquals(Dinero(1250), registrado(TipoMovimiento.FIADO).efectoEnSaldo)
        assertEquals(Dinero(-1250), registrado(TipoMovimiento.PAGO).efectoEnSaldo)
    }

    @Test
    fun `un pago guarda su metodo y un fiado no`() {
        assertEquals(
            MetodoCobro.YAPE.id,
            assertIs<Resultado.Exito<Movimiento>>(registrar(TipoMovimiento.PAGO, metodo = MetodoCobro.YAPE)).valor.metodoCobroId,
        )
        assertNull(assertIs<Resultado.Exito<Movimiento>>(registrar(TipoMovimiento.FIADO, metodo = MetodoCobro.YAPE)).valor.metodoCobroId)
    }

    @Test
    fun `un pago sin metodo no se registra`() {
        assertEquals(Resultado.Fallo(ErrorMovimiento.SinMetodoCobro), registrar(TipoMovimiento.PAGO, metodo = null))
    }

    @Test
    fun `un pago con un metodo archivado no se registra`() {
        val archivado = MetodoCobro.PLIN.copy(archivadoEn = ahora)

        assertEquals(Resultado.Fallo(ErrorMovimiento.MetodoCobroArchivado), registrar(TipoMovimiento.PAGO, metodo = archivado))
    }

    @Test
    fun `no se puede construir un pago sin metodo ni un fiado con metodo`() {
        val fiado = registrado(TipoMovimiento.FIADO)
        val pago = registrado(TipoMovimiento.PAGO)

        assertThrows<IllegalArgumentException> { fiado.copy(metodoCobroId = MetodoCobro.EFECTIVO.id) }
        assertThrows<IllegalArgumentException> { pago.copy(metodoCobroId = null) }
    }

    @Test
    fun `acepta una fecha pasada`() {
        assertIs<Resultado.Exito<Movimiento>>(registrar(fecha = hoy.minusDays(30)))
    }

    @Test
    fun `rechaza una fecha futura`() {
        assertEquals(Resultado.Fallo(ErrorMovimiento.FechaFutura), registrar(fecha = hoy.plusDays(1)))
    }

    @Test
    fun `rechaza un monto cero`() {
        assertEquals(Resultado.Fallo(ErrorMovimiento.MontoNoPositivo), registrar(monto = Dinero.CERO))
    }

    @Test
    fun `rechaza movimientos de un cliente archivado`() {
        val archivado = assertIs<Resultado.Exito<Cliente>>(cliente.archivar(Dinero.CERO, ahora)).valor

        assertEquals(Resultado.Fallo(ErrorMovimiento.ClienteArchivado), registrar(para = archivado))
    }

    @Test
    fun `la nota se recorta y una nota en blanco no se guarda`() {
        val conNota = assertIs<Resultado.Exito<Movimiento>>(registrar(nota = "  2 panes y leche  ")).valor
        val enBlanco = assertIs<Resultado.Exito<Movimiento>>(registrar(nota = "   ")).valor

        assertEquals("2 panes y leche", conNota.nota)
        assertNull(enBlanco.nota)
    }

    @Test
    fun `la nota acepta 140 caracteres y rechaza 141`() {
        assertIs<Resultado.Exito<Movimiento>>(registrar(nota = "a".repeat(140)))
        assertEquals(Resultado.Fallo(ErrorMovimiento.NotaMuyLarga), registrar(nota = "a".repeat(141)))
    }

    @Test
    fun `anular lo deja fuera del saldo`() {
        val anulado = registrado().anular(despues)

        assertEquals(despues, anulado.anuladoEn)
        assertEquals(false, anulado.estaVigente)
    }

    @Test
    fun `anular dos veces conserva la primera anulacion`() {
        val anulado = registrado().anular(ahora)

        assertSame(anulado, anulado.anular(despues))
    }

    @Test
    fun `no se puede construir con monto cero o negativo`() {
        val base = registrado()

        assertThrows<IllegalArgumentException> { base.copy(monto = Dinero.CERO) }
        assertThrows<IllegalArgumentException> { base.copy(monto = Dinero(-100)) }
    }
}
