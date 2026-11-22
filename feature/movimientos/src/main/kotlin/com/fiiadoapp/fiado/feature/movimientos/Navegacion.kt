package com.fiiadoapp.fiado.feature.movimientos

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
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

@Serializable
data object MetodosCobro

@Serializable
data object ResumenGeneral

@Serializable
data object MensajeDeCobro

/** Pantalla principal que reúne los métodos de cobro y el mensaje del recordatorio. */
@Serializable
data object Cobros

@Serializable
data class HistorialCliente(
    val clienteId: String,
)

/** [onRegistrado] recibe el id del movimiento guardado; [onCerrar] cierra el registro o la pantalla secundaria abierta. */
fun NavGraphBuilder.pantallasDeMovimientos(
    onCerrar: () -> Unit,
    onRegistrado: (Uuid) -> Unit,
    onEditarCliente: (Uuid) -> Unit,
    onRegistrar: (Uuid, TipoMovimiento) -> Unit,
    onMetodosCobro: () -> Unit,
    onMensajeDeCobro: () -> Unit,
    onHistorial: (Uuid) -> Unit,
) {
    dialog<RegistrarMovimiento> {
        RegistrarMovimientoDialogo(onCerrar = onCerrar, onRegistrado = onRegistrado)
    }
    composable<MetodosCobro> {
        MetodosCobroPantalla(onVolver = onCerrar)
    }
    composable<ResumenGeneral> {
        ResumenPantalla(onCliente = onHistorial)
    }
    composable<Cobros> {
        CobrosPantalla(onMetodosCobro = onMetodosCobro, onMensajeDeCobro = onMensajeDeCobro)
    }
    composable<MensajeDeCobro> {
        MensajeDeCobroPantalla(onVolver = onCerrar)
    }
    composable<HistorialCliente> {
        HistorialPantalla(onVolver = onCerrar, onEditar = onEditarCliente, onRegistrar = onRegistrar)
    }
}
