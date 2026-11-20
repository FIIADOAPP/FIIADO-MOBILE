package com.fiiadoapp.fiado

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fiiadoapp.fiado.core.ui.AltoBarraInferior
import com.fiiadoapp.fiado.core.ui.BarraInferiorFlotante
import com.fiiadoapp.fiado.core.ui.DestinoBarra
import com.fiiadoapp.fiado.core.ui.FiadoTheme
import com.fiiadoapp.fiado.core.ui.IconosFiado
import com.fiiadoapp.fiado.core.ui.LocalEspacioBarraInferior
import com.fiiadoapp.fiado.domain.TipoMovimiento
import com.fiiadoapp.fiado.feature.clientes.FormularioCliente
import com.fiiadoapp.fiado.feature.clientes.ListaClientes
import com.fiiadoapp.fiado.feature.clientes.pantallasDeClientes
import com.fiiadoapp.fiado.feature.cuenta.AvisoSesionTerminada
import com.fiiadoapp.fiado.feature.cuenta.Cuenta
import com.fiiadoapp.fiado.feature.cuenta.pantallasDeCuenta
import com.fiiadoapp.fiado.feature.movimientos.Cobros
import com.fiiadoapp.fiado.feature.movimientos.HistorialCliente
import com.fiiadoapp.fiado.feature.movimientos.MensajeDeCobro
import com.fiiadoapp.fiado.feature.movimientos.MetodosCobro
import com.fiiadoapp.fiado.feature.movimientos.RegistrarMovimiento
import com.fiiadoapp.fiado.feature.movimientos.ResumenGeneral
import com.fiiadoapp.fiado.feature.movimientos.pantallasDeMovimientos
import dagger.hilt.android.AndroidEntryPoint
import kotlin.reflect.KClass
import kotlin.uuid.Uuid

/** Las cuatro pantallas principales, en el orden de la barra inferior. */
private val PRINCIPALES: List<Pair<Any, KClass<*>>> =
    listOf(
        ListaClientes to ListaClientes::class,
        ResumenGeneral to ResumenGeneral::class,
        Cobros to Cobros::class,
        Cuenta to Cuenta::class,
    )

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La app es siempre oscura: barras del sistema transparentes con íconos claros.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            FiadoTheme {
                val navController = rememberNavController()
                val entrada by navController.currentBackStackEntryAsState()
                val principal = PRINCIPALES.indexOfFirst { (_, ruta) -> entrada?.destination?.hasRoute(ruta) == true }.takeIf { it >= 0 }
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    CompositionLocalProvider(LocalEspacioBarraInferior provides AltoBarraInferior) {
                        Navegacion(navController)
                    }
                    if (principal != null) {
                        BarraInferiorFlotante(
                            destinos = destinos(),
                            seleccionado = principal,
                            onDestino = { navController.irAPrincipal(PRINCIPALES[it].first) },
                            descripcionCentral = stringResource(R.string.barra_nuevo_cliente),
                            onCentral = { navController.navigate(FormularioCliente()) },
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
                AvisoSesionTerminada()
            }
        }
    }
}

@Composable
private fun destinos(): List<DestinoBarra> =
    listOf(
        DestinoBarra(Icons.Rounded.Home, stringResource(R.string.barra_clientes)),
        DestinoBarra(IconosFiado.Resumen, stringResource(R.string.barra_resumen)),
        DestinoBarra(IconosFiado.Cobros, stringResource(R.string.barra_cobros)),
        DestinoBarra(Icons.Rounded.AccountCircle, stringResource(R.string.barra_cuenta)),
    )

/** Cambia de pestaña sin apilar pantallas principales y recordando dónde quedó cada una. */
private fun NavHostController.irAPrincipal(ruta: Any) =
    navigate(ruta) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

@Composable
private fun Navegacion(navController: NavHostController) {
    val registrar = { clienteId: Uuid, tipo: TipoMovimiento ->
        navController.navigate(RegistrarMovimiento(clienteId.toString(), tipo.name))
    }
    NavHost(navController = navController, startDestination = ListaClientes) {
        pantallasDeClientes(
            navController,
            onCliente = { navController.navigate(HistorialCliente(it.toString())) },
            onRegistrar = registrar,
            onResumen = { navController.irAPrincipal(ResumenGeneral) },
        )
        pantallasDeMovimientos(
            onCerrar = { navController.popBackStack() },
            onEditarCliente = { navController.navigate(FormularioCliente(it.toString())) },
            onRegistrar = registrar,
            onRegistrado = { movimientoId ->
                navController.popBackStack()
                navController.currentBackStackEntry?.savedStateHandle?.set(
                    ListaClientes.MOVIMIENTO_REGISTRADO,
                    movimientoId.toString(),
                )
            },
            onMetodosCobro = { navController.navigate(MetodosCobro) },
            onMensajeDeCobro = { navController.navigate(MensajeDeCobro) },
        )
        pantallasDeCuenta(navController)
    }
}
