package com.fiiadoapp.fiado.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.uuid.Uuid

class MetodoCobroTest {
    private val ahora = Instant.parse("2026-11-15T15:00:00Z")
    private val iniciales = MetodoCobro.INICIALES

    private fun creado(nombre: String) =
        assertIs<Resultado.Exito<MetodoCobro>>(MetodoCobro.crear(Uuid.random(), nombre, iniciales, ahora)).valor

    @Test
    fun `empieza con Efectivo, Yape y Plin con identificadores fijos`() {
        assertEquals(listOf("Efectivo", "Yape", "Plin"), iniciales.map { it.nombre })
        assertEquals(Uuid.parse("00000000-0000-7000-8000-000000000001"), MetodoCobro.EFECTIVO.id)
        assertEquals(3, iniciales.map { it.id }.toSet().size)
    }

    @Test
    fun `crear normaliza los espacios del nombre`() {
        val metodo = creado("  Transferencia   BCP ")

        assertEquals("Transferencia BCP", metodo.nombre)
        assertEquals(ahora, metodo.creadoEn)
    }

    @Test
    fun `crear rechaza un nombre vacio o demasiado largo`() {
        assertEquals(Resultado.Fallo(ErrorMetodoCobro.NombreVacio), MetodoCobro.crear(Uuid.random(), "  ", iniciales, ahora))
        assertIs<Resultado.Exito<MetodoCobro>>(MetodoCobro.crear(Uuid.random(), "a".repeat(30), iniciales, ahora))
        assertEquals(Resultado.Fallo(ErrorMetodoCobro.NombreMuyLargo), MetodoCobro.crear(Uuid.random(), "a".repeat(31), iniciales, ahora))
    }

    @Test
    fun `no repite un nombre activo aunque cambien mayusculas o tildes`() {
        listOf("yape", "YAPE", " Yápe ").forEach {
            assertEquals(Resultado.Fallo(ErrorMetodoCobro.NombreRepetido), MetodoCobro.crear(Uuid.random(), it, iniciales, ahora))
        }
    }

    @Test
    fun `un nombre archivado se puede volver a usar`() {
        val yapeArchivado = MetodoCobro.YAPE.copy(archivadoEn = ahora)

        assertIs<Resultado.Exito<MetodoCobro>>(MetodoCobro.crear(Uuid.random(), "Yape", listOf(MetodoCobro.EFECTIVO, yapeArchivado), ahora))
    }

    @Test
    fun `renombrar conserva el id y puede mantener su propio nombre`() {
        val renombrado = assertIs<Resultado.Exito<MetodoCobro>>(MetodoCobro.YAPE.renombrar("Yape negocio", iniciales, ahora)).valor
        val igual = assertIs<Resultado.Exito<MetodoCobro>>(MetodoCobro.YAPE.renombrar("yape", iniciales, ahora)).valor

        assertEquals(MetodoCobro.YAPE.id, renombrado.id)
        assertEquals("Yape negocio", renombrado.nombre)
        assertEquals(ahora, renombrado.actualizadoEn)
        assertEquals("yape", igual.nombre)
    }

    @Test
    fun `renombrar no puede repetir el nombre de otro activo`() {
        assertEquals(Resultado.Fallo(ErrorMetodoCobro.NombreRepetido), MetodoCobro.YAPE.renombrar("Plin", iniciales, ahora))
    }

    @Test
    fun `archivar lo saca de los activos`() {
        val archivado = assertIs<Resultado.Exito<MetodoCobro>>(MetodoCobro.PLIN.archivar(iniciales, ahora)).valor

        assertEquals(ahora, archivado.archivadoEn)
        assertSame(archivado, assertIs<Resultado.Exito<MetodoCobro>>(archivado.archivar(iniciales, ahora.plusSeconds(1))).valor)
    }

    @Test
    fun `no se archiva el ultimo metodo activo`() {
        val activos = listOf(MetodoCobro.EFECTIVO, MetodoCobro.YAPE.copy(archivadoEn = ahora))

        assertEquals(Resultado.Fallo(ErrorMetodoCobro.UltimoActivo), MetodoCobro.EFECTIVO.archivar(activos, ahora))
    }

    @Test
    fun `no se puede construir con un nombre invalido`() {
        assertThrows<IllegalArgumentException> { MetodoCobro(Uuid.random(), " Yape", ahora, ahora, null) }
    }
}
