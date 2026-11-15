package com.fiiadoapp.fiado.feature.clientes

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.fiiadoapp.fiado.domain.TipoMovimiento
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data object ListaClientes {
    /** Clave del estado guardado de la lista donde el registro deja el id del movimiento recién guardado. */
    const val MOVIMIENTO_REGISTRADO = "movimientoRegistrado"
}

/** Sin `clienteId` crea un cliente; con él, lo edita. */
@Serializable
data class FormularioCliente(
    val clienteId: String? = null,
)

/** [onRegistrar] abre el registro de un fiado o un pago para el cliente. */
fun NavGraphBuilder.pantallasDeClientes(
    navController: NavController,
    onRegistrar: (Uuid, TipoMovimiento) -> Unit,
    onMetodosCobro: () -> Unit,
) {
    composable<ListaClientes> { entrada ->
        val registrado by entrada.savedStateHandle
            .getStateFlow<String?>(ListaClientes.MOVIMIENTO_REGISTRADO, null)
            .collectAsStateWithLifecycle()
        ListaClientesPantalla(
            movimientoRegistrado = registrado?.let(Uuid::parse),
            onAvisoConsumido = { entrada.savedStateHandle[ListaClientes.MOVIMIENTO_REGISTRADO] = null },
            onNuevo = { navController.navigate(FormularioCliente()) },
            onCliente = { navController.navigate(FormularioCliente(it.toString())) },
            onRegistrar = onRegistrar,
            onMetodosCobro = onMetodosCobro,
        )
    }
    composable<FormularioCliente> {
        FormularioClientePantalla(onCerrar = { navController.popBackStack() })
    }
}
