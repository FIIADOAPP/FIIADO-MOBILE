package com.fiiadoapp.fiado.domain

import kotlin.math.absoluteValue

/** Monto en céntimos de sol. Nunca pasa por punto flotante. */
@JvmInline
value class Dinero(
    val centimos: Long,
) : Comparable<Dinero> {
    val esPositivo: Boolean get() = centimos > 0

    val esNegativo: Boolean get() = centimos < 0

    operator fun plus(otro: Dinero) = Dinero(Math.addExact(centimos, otro.centimos))

    operator fun minus(otro: Dinero) = Dinero(Math.subtractExact(centimos, otro.centimos))

    operator fun unaryMinus() = Dinero(Math.negateExact(centimos))

    override fun compareTo(other: Dinero) = centimos.compareTo(other.centimos)

    /** Formato peruano: `S/ 1,234.50`, con `-` delante si es negativo. */
    fun formatear(): String {
        val absoluto = centimos.absoluteValue
        val soles =
            (absoluto / CENTIMOS_POR_SOL)
                .toString()
                .reversed()
                .chunked(3)
                .joinToString(",")
                .reversed()
        val centimosRestantes = (absoluto % CENTIMOS_POR_SOL).toString().padStart(2, '0')
        val signo = if (esNegativo) "-" else ""
        return "${signo}S/ $soles.$centimosRestantes"
    }

    companion object {
        val CERO = Dinero(0)

        private const val CENTIMOS_POR_SOL = 100L
        private val FORMATO_ENTRADA = Regex("""(\d{1,7})(?:[.,](\d{1,2}))?""")

        /**
         * Convierte lo que escribe el usuario ("12", "12.5", "12,50") a céntimos.
         * Devuelve `null` si el texto no es un monto válido. No acepta signo ni separador de miles.
         */
        fun desdeTexto(texto: String): Dinero? {
            val coincidencia = FORMATO_ENTRADA.matchEntire(texto.trim()) ?: return null
            val (soles, decimales) = coincidencia.destructured
            val centimos = decimales.padEnd(2, '0').toLong()
            return Dinero(soles.toLong() * CENTIMOS_POR_SOL + centimos)
        }
    }
}
