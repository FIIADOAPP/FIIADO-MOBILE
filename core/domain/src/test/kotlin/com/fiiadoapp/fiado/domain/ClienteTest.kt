package com.fiiadoapp.fiado.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.uuid.Uuid

class ClienteTest {
    private val creado = Instant.parse("2026-11-15T14:00:00Z")
    private val despues = Instant.parse("2026-11-16T09:30:00Z")
    private val telefono = Telefono.desdeTexto("987654321")

    private fun nuevo(nombre: String = "Doña Rosa"): Cliente {
        val resultado = Cliente.crear(Uuid.random(), nombre, telefono, creado)
        return assertIs<Resultado.Exito<Cliente>>(resultado).valor
    }

    @Test
    fun `crear normaliza los espacios del nombre`() {
        val cliente = nuevo("  Doña   Rosa  ")

        assertEquals("Doña Rosa", cliente.nombre)
        assertEquals(creado, cliente.creadoEn)
        assertEquals(creado, cliente.actualizadoEn)
        assertNull(cliente.archivadoEn)
    }

    @Test
    fun `crear rechaza un nombre vacio`() {
        val resultado = Cliente.crear(Uuid.random(), "   ", null, creado)

        assertEquals(Resultado.Fallo(ErrorCliente.NombreVacio), resultado)
    }

    @Test
    fun `crear acepta 60 caracteres y rechaza 61`() {
        assertIs<Resultado.Exito<Cliente>>(Cliente.crear(Uuid.random(), "a".repeat(60), null, creado))
        assertEquals(Resultado.Fallo(ErrorCliente.NombreMuyLargo), Cliente.crear(Uuid.random(), "a".repeat(61), null, creado))
    }

    @Test
    fun `editar cambia nombre y telefono y actualiza la marca de tiempo`() {
        val editado = assertIs<Resultado.Exito<Cliente>>(nuevo().editar(" Rosa  Quispe ", null, despues)).valor

        assertEquals("Rosa Quispe", editado.nombre)
        assertNull(editado.telefono)
        assertEquals(creado, editado.creadoEn)
        assertEquals(despues, editado.actualizadoEn)
    }

    @Test
    fun `editar rechaza un nombre vacio`() {
        assertEquals(Resultado.Fallo(ErrorCliente.NombreVacio), nuevo().editar("", telefono, despues))
    }

    @Test
    fun `archivar con saldo cero lo archiva`() {
        val archivado = assertIs<Resultado.Exito<Cliente>>(nuevo().archivar(Dinero.CERO, despues)).valor

        assertEquals(despues, archivado.archivadoEn)
        assertEquals(true, archivado.estaArchivado)
    }

    @Test
    fun `archivar con deuda o saldo a favor falla`() {
        val cliente = nuevo()

        assertEquals(Resultado.Fallo(ErrorCliente.TieneSaldo(Dinero(1250))), cliente.archivar(Dinero(1250), despues))
        assertEquals(Resultado.Fallo(ErrorCliente.TieneSaldo(Dinero(-500))), cliente.archivar(Dinero(-500), despues))
    }

    @Test
    fun `archivar dos veces conserva la primera fecha`() {
        val archivado = assertIs<Resultado.Exito<Cliente>>(nuevo().archivar(Dinero.CERO, creado)).valor

        val otraVez = assertIs<Resultado.Exito<Cliente>>(archivado.archivar(Dinero.CERO, despues)).valor

        assertSame(archivado, otraVez)
    }

    @Test
    fun `desarchivar lo devuelve a la lista`() {
        val archivado = assertIs<Resultado.Exito<Cliente>>(nuevo().archivar(Dinero.CERO, creado)).valor

        val activo = archivado.desarchivar(despues)

        assertNull(activo.archivadoEn)
        assertEquals(despues, activo.actualizadoEn)
    }

    @Test
    fun `no se puede construir con un nombre invalido`() {
        assertThrows<IllegalArgumentException> { Cliente(Uuid.random(), " Rosa", null, creado, creado, null) }
        assertThrows<IllegalArgumentException> { Cliente(Uuid.random(), "", null, creado, creado, null) }
    }
}
