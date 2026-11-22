package com.fiiadoapp.fiado.domain

import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import kotlin.uuid.Uuid

/**
 * Más plazo para un fiado (ADR-0007). Es inmutable: dar más plazo otra vez crea otra prórroga.
 * El fiado no cambia; su plazo vigente sale de él y de sus prórrogas.
 */
data class Prorroga(
    val id: Uuid,
    val movimientoId: Uuid,
    val vence: LocalDate,
    val registradaEn: Instant,
) {
    companion object {
        /** Solo alarga el plazo de un fiado vigente que ya tenía plazo. */
        fun dar(
            id: Uuid,
            fiado: Movimiento,
            prorrogas: List<Prorroga>,
            vence: LocalDate,
            ahora: Instant,
        ): Resultado<Prorroga, ErrorProrroga> {
            val vigente = plazoVigente(fiado, prorrogas)
            return when {
                fiado.tipo != TipoMovimiento.FIADO || vigente == null -> Resultado.Fallo(ErrorProrroga.SinPlazo)
                !fiado.estaVigente -> Resultado.Fallo(ErrorProrroga.Anulado)
                !vence.isAfter(vigente) -> Resultado.Fallo(ErrorProrroga.NoAlarga)
                else -> Resultado.Exito(Prorroga(id, fiado.id, vence, ahora))
            }
        }
    }
}

sealed interface ErrorProrroga {
    data object SinPlazo : ErrorProrroga

    data object Anulado : ErrorProrroga

    /** La fecha nueva no es posterior al plazo vigente. */
    data object NoAlarga : ErrorProrroga
}

/** El plazo de la prórroga más lejana del fiado o, si no tiene, el suyo. `null` sin plazo. */
fun plazoVigente(
    fiado: Movimiento,
    prorrogas: List<Prorroga>,
): LocalDate? {
    val original = fiado.vence ?: return null
    return prorrogas.filter { it.movimientoId == fiado.id }.maxOfOrNull { it.vence }?.takeIf { it.isAfter(original) } ?: original
}

interface RepositorioProrrogas {
    fun observarDeCliente(clienteId: Uuid): Flow<List<Prorroga>>

    fun observarTodas(): Flow<List<Prorroga>>

    suspend fun guardar(prorroga: Prorroga)
}
