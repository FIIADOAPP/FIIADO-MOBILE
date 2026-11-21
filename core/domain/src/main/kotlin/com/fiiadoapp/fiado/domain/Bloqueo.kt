package com.fiiadoapp.fiado.domain

import kotlinx.coroutines.flow.Flow

/** Si la app pide la huella o el PIN del teléfono (ADR-0006). Vive solo en este teléfono. */
interface RepositorioBloqueo {
    fun observarActivo(): Flow<Boolean>

    suspend fun cambiar(activo: Boolean)
}

/**
 * Cuándo volver a pedir la huella al regresar a la app (ADR-0006). Los tiempos son de un reloj
 * monótono en milisegundos: cambiar la hora del teléfono no los altera.
 */
object ReglaDeBloqueo {
    const val ESPERA_EN_SEGUNDO_PLANO_MS = 60_000L

    /** [salioEn] es cuándo la app pasó a segundo plano, o `null` si no salió. */
    fun pedirAlVolver(
        salioEn: Long?,
        ahora: Long,
    ): Boolean = salioEn != null && ahora - salioEn >= ESPERA_EN_SEGUNDO_PLANO_MS
}
