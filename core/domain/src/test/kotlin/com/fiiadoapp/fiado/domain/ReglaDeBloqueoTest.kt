package com.fiiadoapp.fiado.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReglaDeBloqueoTest {
    @Test
    fun `una salida corta no vuelve a pedir la huella`() {
        assertFalse(ReglaDeBloqueo.pedirAlVolver(salioEn = 1_000, ahora = 1_000 + 59_999))
    }

    @Test
    fun `un minuto o mas en segundo plano la vuelve a pedir`() {
        assertTrue(ReglaDeBloqueo.pedirAlVolver(salioEn = 1_000, ahora = 1_000 + 60_000))
        assertTrue(ReglaDeBloqueo.pedirAlVolver(salioEn = 1_000, ahora = 1_000 + 3_600_000))
    }

    @Test
    fun `si la app no salio no hay nada que pedir`() {
        assertFalse(ReglaDeBloqueo.pedirAlVolver(salioEn = null, ahora = 999_999))
    }
}
