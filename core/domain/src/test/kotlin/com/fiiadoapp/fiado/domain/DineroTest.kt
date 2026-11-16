package com.fiiadoapp.fiado.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DineroTest {
    @ParameterizedTest
    @CsvSource(
        "12, 1200",
        "12.5, 1250",
        "12.50, 1250",
        "'12,5', 1250",
        "0.05, 5",
        "0.5, 50",
        "' 7.25 ', 725",
        "9999999.99, 999999999",
    )
    fun `convierte texto valido a centimos`(
        texto: String,
        centimos: Long,
    ) {
        assertEquals(Dinero(centimos), Dinero.desdeTexto(texto))
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "abc", "12.", ".5", "12.505", "-5", "+5", "1,234.50", "1 000", "12000000", "S/ 5", "1e3"])
    fun `rechaza texto que no es un monto`(texto: String) {
        assertNull(Dinero.desdeTexto(texto))
    }

    @ParameterizedTest
    @CsvSource(
        "0, S/ 0.00",
        "5, S/ 0.05",
        "1250, S/ 12.50",
        "123450, 'S/ 1,234.50'",
        "100000000, 'S/ 1,000,000.00'",
        "-1250, -S/ 12.50",
    )
    fun `formatea al estilo peruano`(
        centimos: Long,
        esperado: String,
    ) {
        assertEquals(esperado, Dinero(centimos).formatear())
    }

    @Test
    fun `suma y resta sin perder centimos`() {
        val total = Dinero(10) + Dinero(20) - Dinero(5)

        assertEquals(Dinero(25), total)
        assertEquals(Dinero(-25), -total)
    }

    @Test
    fun `falla en lugar de desbordarse`() {
        assertThrows<ArithmeticException> { Dinero(Long.MAX_VALUE) + Dinero(1) }
    }
}
