package com.fiiadoapp.fiado.domain

import java.text.Normalizer
import java.time.Instant
import java.util.Locale
import kotlin.uuid.Uuid

/** Vía por la que entra un pago: Efectivo, Yape, Plin, transferencia... (ADR-0004). */
data class MetodoCobro(
    val id: Uuid,
    val nombre: String,
    val creadoEn: Instant,
    val actualizadoEn: Instant,
    val archivadoEn: Instant?,
) {
    init {
        require(validarNombre(nombre) == null) { "Nombre de método inválido: '$nombre'" }
        require(nombre == normalizarNombre(nombre)) { "Nombre de método sin normalizar: '$nombre'" }
    }

    val estaArchivado: Boolean get() = archivadoEn != null

    /** [otros] son los demás métodos activos, para no repetir nombres. */
    fun renombrar(
        nombre: String,
        otros: List<MetodoCobro>,
        ahora: Instant,
    ): Resultado<MetodoCobro, ErrorMetodoCobro> {
        val normalizado = normalizarNombre(nombre)
        validar(normalizado, otros.filter { it.id != id })?.let { return Resultado.Fallo(it) }
        return Resultado.Exito(copy(nombre = normalizado, actualizadoEn = ahora))
    }

    /** No se archiva el último método activo: un pago siempre necesita uno. */
    fun archivar(
        activos: List<MetodoCobro>,
        ahora: Instant,
    ): Resultado<MetodoCobro, ErrorMetodoCobro> =
        when {
            estaArchivado -> Resultado.Exito(this)
            activos.none { it.id != id && !it.estaArchivado } -> Resultado.Fallo(ErrorMetodoCobro.UltimoActivo)
            else -> Resultado.Exito(copy(archivadoEn = ahora, actualizadoEn = ahora))
        }

    companion object {
        const val LARGO_MAXIMO_NOMBRE = 30

        // Antes que los métodos iniciales, que los usan al construirse.
        private val ESPACIOS = Regex("""\s+""")
        private val MARCAS_DIACRITICAS = Regex("""\p{Mn}+""")

        /** Identificadores fijos: iguales en todos los teléfonos, así restaurar no los duplica. */
        val EFECTIVO = inicial("00000000-0000-7000-8000-000000000001", "Efectivo")
        val YAPE = inicial("00000000-0000-7000-8000-000000000002", "Yape")
        val PLIN = inicial("00000000-0000-7000-8000-000000000003", "Plin")
        val INICIALES = listOf(EFECTIVO, YAPE, PLIN)

        /** [activos] son los métodos activos existentes, para no repetir nombres. */
        fun crear(
            id: Uuid,
            nombre: String,
            activos: List<MetodoCobro>,
            ahora: Instant,
        ): Resultado<MetodoCobro, ErrorMetodoCobro> {
            val normalizado = normalizarNombre(nombre)
            validar(normalizado, activos)?.let { return Resultado.Fallo(it) }
            return Resultado.Exito(MetodoCobro(id, normalizado, ahora, ahora, archivadoEn = null))
        }

        private fun inicial(
            id: String,
            nombre: String,
        ) = MetodoCobro(Uuid.parse(id), nombre, Instant.EPOCH, Instant.EPOCH, archivadoEn = null)

        private fun validar(
            nombre: String,
            otros: List<MetodoCobro>,
        ): ErrorMetodoCobro? =
            validarNombre(nombre)
                ?: ErrorMetodoCobro.NombreRepetido.takeIf {
                    otros.any { !it.estaArchivado && clave(it.nombre) == clave(nombre) }
                }

        private fun normalizarNombre(nombre: String) = nombre.trim().replace(ESPACIOS, " ")

        /** "Efectivo", "EFECTIVO" y "efectívo" son el mismo nombre. */
        private fun clave(nombre: String) =
            Normalizer.normalize(nombre, Normalizer.Form.NFD).replace(MARCAS_DIACRITICAS, "").lowercase(Locale.ROOT)

        private fun validarNombre(nombre: String): ErrorMetodoCobro? =
            when {
                nombre.isBlank() -> ErrorMetodoCobro.NombreVacio
                nombre.length > LARGO_MAXIMO_NOMBRE -> ErrorMetodoCobro.NombreMuyLargo
                else -> null
            }
    }
}

sealed interface ErrorMetodoCobro {
    data object NombreVacio : ErrorMetodoCobro

    data object NombreMuyLargo : ErrorMetodoCobro

    data object NombreRepetido : ErrorMetodoCobro

    data object UltimoActivo : ErrorMetodoCobro
}
