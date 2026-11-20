package com.fiiadoapp.fiado.domain

import kotlinx.coroutines.flow.Flow

/**
 * Cómo va el respaldo de la libreta en el servidor (ADR-0003 de FIIADO-API).
 *
 * [pendientes] son los cambios guardados en el teléfono que el servidor aún no confirmó.
 * [detenido] indica que el servidor rechazó un cambio: el envío no sigue hasta que se corrija la app.
 */
data class EstadoRespaldo(
    val pendientes: Int,
    val detenido: Boolean,
)

interface RepositorioRespaldo {
    fun observar(): Flow<EstadoRespaldo>
}
