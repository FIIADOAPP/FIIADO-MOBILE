package com.fiiadoapp.fiado.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals

class RecordatorioTest {
    private val saldo = Dinero(1250)

    private fun plantilla(texto: String) = (PlantillaRecordatorio.desdeTexto(texto) as Resultado.Exito).valor

    @Test
    fun `la plantilla por defecto saluda con el nombre del negocio y el saldo`() {
        assertEquals(
            "Hola Juana, te saluda Bodega Doña Rosa.\nTu saldo pendiente es de S/ 12.50.\n¡Gracias!",
            PlantillaRecordatorio.POR_DEFECTO.mensaje("Juana", saldo, "Bodega Doña Rosa"),
        )
    }

    @Test
    fun `sin nombre del negocio se quita la frase que lo menciona`() {
        assertEquals(
            "Hola Juana.\nTu saldo pendiente es de S/ 12.50.\n¡Gracias!",
            PlantillaRecordatorio.POR_DEFECTO.mensaje("Juana", saldo, null),
        )
    }

    @Test
    fun `sin nombre del negocio ni coma antes, se borra el nombre sin dejar espacios dobles`() {
        assertEquals(
            "te recuerda: Juana debe S/ 12.50.",
            plantilla("{negocio} te recuerda: {cliente} debe {saldo}.").mensaje("Juana", saldo, " "),
        )
    }

    @Test
    fun `una plantilla propia reemplaza cada espacio todas las veces`() {
        assertEquals(
            "Juana: debes S/ 12.50 (S/ 12.50 en total). Bodega",
            plantilla("  {cliente}: debes {saldo} ({saldo} en total). {negocio}  ").mensaje("Juana", saldo, "Bodega"),
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   ", "Hola {cliente}, págame"])
    fun `rechaza una plantilla vacia o sin el saldo`(texto: String) {
        val esperado = if (texto.isBlank()) ErrorPlantilla.VACIA else ErrorPlantilla.SIN_SALDO
        assertEquals(Resultado.Fallo(esperado), PlantillaRecordatorio.desdeTexto(texto))
    }

    @Test
    fun `rechaza una plantilla demasiado larga`() {
        val larga = "{saldo}" + "x".repeat(PlantillaRecordatorio.LARGO_MAXIMO)

        assertEquals(Resultado.Fallo(ErrorPlantilla.LARGA), PlantillaRecordatorio.desdeTexto(larga))
    }

    @Test
    fun `el celular va sin el signo mas para wa_me`() {
        assertEquals("51987654321", Telefono.desdeTexto("987 654 321")?.paraWhatsApp)
    }
}
