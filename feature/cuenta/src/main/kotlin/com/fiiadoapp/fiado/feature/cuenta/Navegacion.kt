package com.fiiadoapp.fiado.feature.cuenta

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object Cuenta

@Serializable
internal data object CrearCuenta

@Serializable
internal data class VerificarCorreo(
    val correo: String,
)

@Serializable
internal data class IniciarSesion(
    val correo: String = "",
)

@Serializable
internal data class RecuperarContrasena(
    val correo: String = "",
)

/** Cada flujo termina volviendo a [Cuenta], que muestra la sesión abierta. */
fun NavGraphBuilder.pantallasDeCuenta(navController: NavController) {
    fun volverACuenta() = navController.popBackStack(Cuenta, inclusive = false)

    fun iniciarSesionCon(correo: String) = navController.navigate(IniciarSesion(correo)) { popUpTo(Cuenta) { inclusive = false } }

    composable<Cuenta> {
        CuentaPantalla(
            onCrearCuenta = { navController.navigate(CrearCuenta) },
            onIniciarSesion = { navController.navigate(IniciarSesion()) },
        )
    }
    composable<CrearCuenta> {
        CrearCuentaPantalla(
            onVolver = { navController.popBackStack() },
            onCreada = { navController.navigate(VerificarCorreo(it)) { popUpTo(Cuenta) { inclusive = false } } },
        )
    }
    composable<VerificarCorreo> {
        VerificarCorreoPantalla(onVolver = { volverACuenta() }, onVerificado = ::iniciarSesionCon)
    }
    composable<IniciarSesion> {
        IniciarSesionPantalla(
            onVolver = { navController.popBackStack() },
            onIniciada = { volverACuenta() },
            onVerificar = { navController.navigate(VerificarCorreo(it)) },
            onRecuperar = { navController.navigate(RecuperarContrasena(it)) },
        )
    }
    composable<RecuperarContrasena> {
        RecuperarContrasenaPantalla(onVolver = { navController.popBackStack() }, onRestablecida = ::iniciarSesionCon)
    }
}
