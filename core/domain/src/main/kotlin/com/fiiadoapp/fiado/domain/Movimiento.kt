package com.fiiadoapp.fiado.domain

import java.time.Instant
import java.time.LocalDate
import kotlin.uuid.Uuid

enum class TipoMovimiento { FIADO, PAGO }

/**
 * Un fiado o un pago. Es inmutable: solo puede pasar de vigente a anulado.
 * Un pago siempre indica por qué [MetodoCobro] entró; un fiado nunca (ADR-0004).
 * Un fiado puede tener plazo, [vence], que no es anterior a su [fecha] (ADR-0007).
 */
data class Movimiento(
    val id: Uuid,
    val clienteId: Uuid,
    val tipo: TipoMovimiento,
    val monto: Dinero,
    val nota: String?,
    val fecha: LocalDate,
    val registradoEn: Instant,
    val anuladoEn: Instant?,
    val metodoCobroId: Uuid? = null,
    val vence: LocalDate? = null,
) {
    init {
        require(monto.esPositivo) { "El monto debe ser mayor que cero: $monto" }
        require(nota == null || nota == normalizarNota(nota)) { "Nota sin normalizar: '$nota'" }
        require(nota == null || nota.length <= LARGO_MAXIMO_NOTA) { "Nota de más de $LARGO_MAXIMO_NOTA caracteres" }
        require(
            (tipo == TipoMovimiento.PAGO) == (metodoCobroId != null),
        ) { "Un pago lleva método de cobro y un fiado no: $tipo, $metodoCobroId" }
        require(vence == null || (tipo == TipoMovimiento.FIADO && !vence.isBefore(fecha))) { "Plazo inválido: $tipo, $fecha, $vence" }
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

        /** [metodoCobro] es obligatorio para un pago y se ignora en un fiado; [vence], al revés. */
        fun registrar(
            id: Uuid,
            cliente: Cliente,
            tipo: TipoMovimiento,
            monto: Dinero,
            metodoCobro: MetodoCobro?,
            nota: String?,
            fecha: LocalDate,
            hoy: LocalDate,
            ahora: Instant,
            vence: LocalDate? = null,
        ): Resultado<Movimiento, ErrorMovimiento> {
            val notaNormalizada = nota?.let(::normalizarNota)
            val error =
                when {
                    cliente.estaArchivado -> ErrorMovimiento.ClienteArchivado
                    !monto.esPositivo -> ErrorMovimiento.MontoNoPositivo
                    fecha.isAfter(hoy) -> ErrorMovimiento.FechaFutura
                    notaNormalizada != null && notaNormalizada.length > LARGO_MAXIMO_NOTA -> ErrorMovimiento.NotaMuyLarga
                    tipo == TipoMovimiento.PAGO && metodoCobro == null -> ErrorMovimiento.SinMetodoCobro
                    tipo == TipoMovimiento.PAGO && metodoCobro?.estaArchivado == true -> ErrorMovimiento.MetodoCobroArchivado
                    tipo == TipoMovimiento.FIADO && vence != null && vence.isBefore(fecha) -> ErrorMovimiento.PlazoAntesDeLaFecha
                    else -> null
                }
            if (error != null) return Resultado.Fallo(error)
            val metodoCobroId = metodoCobro?.id?.takeIf { tipo == TipoMovimiento.PAGO }
            val plazo = vence?.takeIf { tipo == TipoMovimiento.FIADO }
            return Resultado.Exito(
                Movimiento(id, cliente.id, tipo, monto, notaNormalizada, fecha, ahora, anuladoEn = null, metodoCobroId, plazo),
            )
        }

        private fun normalizarNota(nota: String): String? = nota.trim().ifEmpty { null }
    }
}

sealed interface ErrorMovimiento {
    data object ClienteArchivado : ErrorMovimiento

    data object MontoNoPositivo : ErrorMovimiento

    data object FechaFutura : ErrorMovimiento

    data object NotaMuyLarga : ErrorMovimiento

    data object SinMetodoCobro : ErrorMovimiento

    data object MetodoCobroArchivado : ErrorMovimiento

    data object PlazoAntesDeLaFecha : ErrorMovimiento
}

/** Atajos para el plazo de un fiado, contados desde el día del fiado (ADR-0007). */
enum class AtajoDePlazo {
    MANANA,
    TRES_DIAS,
    UNA_SEMANA,
    FIN_DE_MES,
    ;

    fun desde(fecha: LocalDate): LocalDate =
        when (this) {
            MANANA -> fecha.plusDays(1)
            TRES_DIAS -> fecha.plusDays(3)
            UNA_SEMANA -> fecha.plusWeeks(1)
            FIN_DE_MES -> fecha.withDayOfMonth(fecha.lengthOfMonth())
        }
}
