package com.fiiadoapp.fiado.domain

import java.time.Instant
import java.time.LocalDate
import kotlin.uuid.Uuid

enum class TipoMovimiento { FIADO, PAGO }

/** Un fiado o un pago. Es inmutable: solo puede pasar de vigente a anulado. */
data class Movimiento(
    val id: Uuid,
    val clienteId: Uuid,
    val tipo: TipoMovimiento,
    val monto: Dinero,
    val nota: String?,
    val fecha: LocalDate,
    val registradoEn: Instant,
    val anuladoEn: Instant?,
) {
    init {
        require(monto.esPositivo) { "El monto debe ser mayor que cero: $monto" }
        require(nota == null || nota == normalizarNota(nota)) { "Nota sin normalizar: '$nota'" }
        require(nota == null || nota.length <= LARGO_MAXIMO_NOTA) { "Nota de más de $LARGO_MAXIMO_NOTA caracteres" }
    }

    val estaVigente: Boolean get() = anuladoEn == null

    /** Lo que suma al saldo del cliente: un fiado aumenta la deuda y un pago la reduce. */
    val efectoEnSaldo: Dinero
        get() =
            when (tipo) {
                TipoMovimiento.FIADO -> monto
                TipoMovimiento.PAGO -> -monto
            }

    /** Anular dos veces conserva la primera anulación. */
    fun anular(ahora: Instant): Movimiento = if (estaVigente) copy(anuladoEn = ahora) else this

    companion object {
        const val LARGO_MAXIMO_NOTA = 140

        fun registrar(
            id: Uuid,
            cliente: Cliente,
            tipo: TipoMovimiento,
            monto: Dinero,
            nota: String?,
            fecha: LocalDate,
            hoy: LocalDate,
            ahora: Instant,
        ): Resultado<Movimiento, ErrorMovimiento> {
            val notaNormalizada = nota?.let(::normalizarNota)
            val error =
                when {
                    cliente.estaArchivado -> ErrorMovimiento.ClienteArchivado
                    !monto.esPositivo -> ErrorMovimiento.MontoNoPositivo
                    fecha.isAfter(hoy) -> ErrorMovimiento.FechaFutura
                    notaNormalizada != null && notaNormalizada.length > LARGO_MAXIMO_NOTA -> ErrorMovimiento.NotaMuyLarga
                    else -> null
                }
            if (error != null) return Resultado.Fallo(error)
            return Resultado.Exito(Movimiento(id, cliente.id, tipo, monto, notaNormalizada, fecha, ahora, anuladoEn = null))
        }

        private fun normalizarNota(nota: String): String? = nota.trim().ifEmpty { null }
    }
}

sealed interface ErrorMovimiento {
    data object ClienteArchivado : ErrorMovimiento

    data object MontoNoPositivo : ErrorMovimiento

    data object FechaFutura : ErrorMovimiento

    data object NotaMuyLarga : ErrorMovimiento
}
