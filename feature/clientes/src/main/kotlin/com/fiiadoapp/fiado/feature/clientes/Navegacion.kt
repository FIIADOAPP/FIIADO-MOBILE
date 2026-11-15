package com.fiiadoapp.fiado.feature.clientes

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object ListaClientes

/** Sin `clienteId` crea un cliente; con él, lo edita. */
@Serializable
data class FormularioCliente(
    val clienteId: String? = null,
)

fun NavGraphBuilder.pantallasDeClientes(navController: NavController) {
    composable<ListaClientes> {
        ListaClientesPantalla(
            onNuevo = { navController.navigate(FormularioCliente()) },
            onCliente = { navController.navigate(FormularioCliente(it.toString())) },
        )
    }
    composable<FormularioCliente> {
        FormularioClientePantalla(onCerrar = { navController.popBackStack() })
    }
}
