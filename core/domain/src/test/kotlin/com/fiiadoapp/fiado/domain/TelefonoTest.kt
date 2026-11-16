package com.fiiadoapp.fiado.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TelefonoTest {
    @ParameterizedTest
    @ValueSource(
        strings = ["987654321", "987 654 321", "987-654-321", "+51 987 654 321", "51987654321", "+51(987)654-321", " 987654321 "],
    )
    fun `normaliza las formas comunes de escribir un celular`(texto: String) {
        assertEquals("+51987654321", Telefono.desdeTexto(texto)?.e164)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "12345", "887654321", "98765432", "9876543210", "014567890", "+1 987654321", "52987654321", "98765432a"])
    fun `rechaza lo que no es un celular peruano`(texto: String) {
        assertNull(Telefono.desdeTexto(texto))
    }

    @Test
    fun `se muestra en grupos de tres`() {
        assertEquals("987 654 321", Telefono.desdeTexto("+51987654321")?.formatear())
    }
}
