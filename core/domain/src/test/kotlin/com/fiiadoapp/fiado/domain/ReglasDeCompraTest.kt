package com.fiiadoapp.fiado.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReglasDeCompraTest {
    private fun compra(
        producto: String = PRODUCTO_QUITAR_ANUNCIOS,
        pagada: Boolean = true,
        reconocida: Boolean = true,
        token: String = "t",
    ) = CompraDePlay(listOf(producto), pagada = pagada, pendiente = !pagada, reconocida = reconocida, token = token)

    @Test
    fun `solo una compra pagada del producto quita los anuncios`() {
        assertTrue(ReglasDeCompra.quitaAnuncios(listOf(compra())))
        assertFalse(ReglasDeCompra.quitaAnuncios(listOf(compra(pagada = false))))
        assertFalse(ReglasDeCompra.quitaAnuncios(listOf(compra(producto = "otro"))))
        assertFalse(ReglasDeCompra.quitaAnuncios(emptyList()))
    }

    @Test
    fun `un pago pendiente se informa pero no quita los anuncios`() {
        val pendiente = listOf(compra(pagada = false))

        assertTrue(ReglasDeCompra.hayPendiente(pendiente))
        assertFalse(ReglasDeCompra.quitaAnuncios(pendiente))
    }

    @Test
    fun `se reconocen solo las compras pagadas que aun no se reconocieron`() {
        val compras =
            listOf(
                compra(reconocida = false, token = "nueva"),
                compra(reconocida = true, token = "vieja"),
                compra(pagada = false, reconocida = false, token = "pendiente"),
            )

        assertEquals(listOf("nueva"), ReglasDeCompra.porReconocer(compras).map { it.token })
    }
}
