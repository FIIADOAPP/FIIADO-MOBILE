package com.fiiadoapp.fiado.domain

import java.time.Instant
import kotlin.uuid.Uuid

data class Cliente(
    val id: Uuid,
    val nombre: String,
    val telefono: Telefono?,
    val creadoEn: Instant,
    val actualizadoEn: Instant,
    val archivadoEn: Instant?,
) {
    init {
        require(validarNombre(nombre) == null) { "Nombre de cliente inválido: '$nombre'" }
        require(nombre == normalizarNombre(nombre)) { "Nombre de cliente sin normalizar: '$nombre'" }
    }

    val estaArchivado: Boolean get() = archivadoEn != null

    fun editar(
        nombre: String,
        telefono: Telefono?,
        ahora: Instant,
    ): Resultado<Cliente, ErrorCliente> {
        val normalizado = normalizarNombre(nombre)
        validarNombre(normalizado)?.let { return Resultado.Fallo(it) }
        return Resultado.Exito(copy(nombre = normalizado, telefono = telefono, actualizadoEn = ahora))
    }

    /** Solo se archiva con saldo cero, para que ninguna deuda desaparezca de la vista. */
    fun archivar(
        saldo: Dinero,
        ahora: Instant,
    ): Resultado<Cliente, ErrorCliente> =
        when {
            estaArchivado -> Resultado.Exito(this)
            saldo != Dinero.CERO -> Resultado.Fallo(ErrorCliente.TieneSaldo(saldo))
            else -> Resultado.Exito(copy(archivadoEn = ahora, actualizadoEn = ahora))
        }

    fun desarchivar(ahora: Instant): Cliente = if (estaArchivado) copy(archivadoEn = null, actualizadoEn = ahora) else this

    companion object {
        const val LARGO_MAXIMO_NOMBRE = 60

        private val ESPACIOS = Regex("""\s+""")

        fun crear(
            id: Uuid,
            nombre: String,
            telefono: Telefono?,
            ahora: Instant,
        ): Resultado<Cliente, ErrorCliente> {
            val normalizado = normalizarNombre(nombre)
            validarNombre(normalizado)?.let { return Resultado.Fallo(it) }
            return Resultado.Exito(Cliente(id, normalizado, telefono, ahora, ahora, archivadoEn = null))
        }

        private fun normalizarNombre(nombre: String) = nombre.trim().replace(ESPACIOS, " ")

        private fun validarNombre(nombre: String): ErrorCliente? =
            when {
                nombre.isBlank() -> ErrorCliente.NombreVacio
                nombre.length > LARGO_MAXIMO_NOMBRE -> ErrorCliente.NombreMuyLargo
                else -> null
            }
    }
}

sealed interface ErrorCliente {
    data object NombreVacio : ErrorCliente

    data object NombreMuyLargo : ErrorCliente

    data class TieneSaldo(
        val saldo: Dinero,
    ) : ErrorCliente
}
