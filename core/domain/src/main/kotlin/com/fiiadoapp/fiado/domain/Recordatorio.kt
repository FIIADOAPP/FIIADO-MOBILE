package com.fiiadoapp.fiado.domain

import kotlinx.coroutines.flow.Flow

/**
 * Texto del recordatorio de cobro por WhatsApp. El bodeguero lo puede cambiar; `{cliente}`,
 * `{saldo}` y `{negocio}` se reemplazan al armar cada mensaje.
 */
@JvmInline
value class PlantillaRecordatorio private constructor(
    val texto: String,
) {
    /**
     * Sin nombre del negocio (por ejemplo, sin cuenta), la frase que lo menciona se quita:
     * "Hola Juana, te saluda {negocio}." queda "Hola Juana.".
     */
    fun mensaje(
        cliente: String,
        saldo: Dinero,
        negocio: String?,
    ): String {
        val conNegocio =
            if (negocio.isNullOrBlank()) {
                texto.replace(FRASE_CON_NEGOCIO, "").replace(NEGOCIO, "")
            } else {
                texto.replace(NEGOCIO, negocio)
            }
        return conNegocio
            .replace(CLIENTE, cliente)
            .replace(SALDO, saldo.formatear())
            .lines()
            .joinToString("\n") { it.replace(ESPACIOS_REPETIDOS, " ").trim() }
            .trim()
    }

    companion object {
        const val CLIENTE = "{cliente}"
        const val SALDO = "{saldo}"
        const val NEGOCIO = "{negocio}"
        const val LARGO_MAXIMO = 500

        val POR_DEFECTO = PlantillaRecordatorio("Hola $CLIENTE, te saluda $NEGOCIO.\nTu saldo pendiente es de $SALDO.\n¡Gracias!")

        /** Desde una coma hasta el fin de la frase que menciona al negocio, sin el punto. */
        private val FRASE_CON_NEGOCIO = Regex(""",[^,.\n]*\{negocio\}[^.\n]*""")
        private val ESPACIOS_REPETIDOS = Regex(" {2,}")

        /** El saldo es lo único obligatorio: un recordatorio de cobro sin el monto no sirve. */
        fun desdeTexto(texto: String): Resultado<PlantillaRecordatorio, ErrorPlantilla> {
            val limpio = texto.trim()
            return when {
                limpio.isEmpty() -> Resultado.Fallo(ErrorPlantilla.VACIA)
                limpio.length > LARGO_MAXIMO -> Resultado.Fallo(ErrorPlantilla.LARGA)
                SALDO !in limpio -> Resultado.Fallo(ErrorPlantilla.SIN_SALDO)
                else -> Resultado.Exito(PlantillaRecordatorio(limpio))
            }
        }
    }
}

enum class ErrorPlantilla { VACIA, LARGA, SIN_SALDO }

/** El celular como lo espera `wa.me`: solo dígitos, con el código del país. */
val Telefono.paraWhatsApp: String get() = e164.removePrefix("+")

/** La plantilla vive solo en este teléfono: no se respalda en el servidor. */
interface RepositorioPlantillaRecordatorio {
    fun observar(): Flow<PlantillaRecordatorio>

    suspend fun guardar(plantilla: PlantillaRecordatorio)

    suspend fun restablecer()
}
