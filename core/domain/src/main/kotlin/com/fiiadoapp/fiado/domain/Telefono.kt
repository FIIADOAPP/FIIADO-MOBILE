package com.fiiadoapp.fiado.domain

/** Celular peruano normalizado como `+519XXXXXXXX`. */
@ConsistentCopyVisibility
data class Telefono private constructor(
    val e164: String,
) {
    /** Formato para mostrar: `987 654 321`. */
    fun formatear(): String = e164.removePrefix(PREFIJO_PERU).chunked(3).joinToString(" ")

    companion object {
        private const val PREFIJO_PERU = "+51"
        private val SEPARADORES = Regex("""[\s\-().]""")
        private val CELULAR = Regex("""(?:\+?51)?(9\d{8})""")

        /** Acepta espacios, guiones, paréntesis y el prefijo 51 o +51. Devuelve `null` si no es un celular peruano. */
        fun desdeTexto(texto: String): Telefono? {
            val digitos = texto.replace(SEPARADORES, "")
            val celular = CELULAR.matchEntire(digitos)?.groupValues?.get(1) ?: return null
            return Telefono(PREFIJO_PERU + celular)
        }
    }
}
