package com.fiiadoapp.fiado.core.data

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeneradorUuidV7Test {
    private val generador = GeneradorUuidV7()

    @Test
    fun `genera UUID version 7`() {
        assertEquals('7', generador.nuevo().toString()[14])
    }

    @Test
    fun `los identificadores sucesivos quedan en orden de creacion`() {
        val ids = List(1_000) { generador.nuevo().toString() }

        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.zipWithNext().all { (anterior, siguiente) -> anterior < siguiente })
    }
}
