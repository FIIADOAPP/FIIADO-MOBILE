package com.fiiadoapp.fiado.feature.movimientos

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import com.fiiadoapp.fiado.domain.TipoMovimiento
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class RegistrarMovimiento(
    val clienteId: String,
    /** Nombre de [TipoMovimiento]: la ruta no usa el enum del dominio para no exigirle anotaciones de Android. */
    val tipo: String,
)

/** [onRegistrado] recibe el id del movimiento guardado; [onCerrar] se llama al cancelar. */
fun NavGraphBuilder.pantallasDeMovimientos(
    onCerrar: () -> Unit,
    onRegistrado: (Uuid) -> Unit,
) {
    dialog<RegistrarMovimiento> {
        RegistrarMovimientoDialogo(onCerrar = onCerrar, onRegistrado = onRegistrado)
    }
}
