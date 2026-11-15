package com.fiiadoapp.fiado.feature.clientes

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.fiiadoapp.fiado.domain.TipoMovimiento
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data object ListaClientes

/** Sin `clienteId` crea un cliente; con él, lo edita. */
@Serializable
data class FormularioCliente(
    val clienteId: String? = null,
)

/** [onRegistrar] abre el registro de un fiado o un pago para el cliente. */
fun NavGraphBuilder.pantallasDeClientes(
    navController: NavController,
    onRegistrar: (Uuid, TipoMovimiento) -> Unit,
) {
    composable<ListaClientes> {
        ListaClientesPantalla(
            onNuevo = { navController.navigate(FormularioCliente()) },
            onCliente = { navController.navigate(FormularioCliente(it.toString())) },
            onRegistrar = onRegistrar,
        )
    }
    composable<FormularioCliente> {
        FormularioClientePantalla(onCerrar = { navController.popBackStack() })
    }
}
