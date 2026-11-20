package com.fiiadoapp.fiado

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.fiiadoapp.fiado.core.ui.FiadoTheme
import com.fiiadoapp.fiado.domain.TipoMovimiento
import com.fiiadoapp.fiado.feature.clientes.FormularioCliente
import com.fiiadoapp.fiado.feature.clientes.ListaClientes
import com.fiiadoapp.fiado.feature.clientes.pantallasDeClientes
import com.fiiadoapp.fiado.feature.cuenta.AvisoSesionTerminada
import com.fiiadoapp.fiado.feature.cuenta.Cuenta
import com.fiiadoapp.fiado.feature.cuenta.pantallasDeCuenta
import com.fiiadoapp.fiado.feature.movimientos.HistorialCliente
import com.fiiadoapp.fiado.feature.movimientos.MensajeDeCobro
import com.fiiadoapp.fiado.feature.movimientos.MetodosCobro
import com.fiiadoapp.fiado.feature.movimientos.RegistrarMovimiento
import com.fiiadoapp.fiado.feature.movimientos.ResumenGeneral
import com.fiiadoapp.fiado.feature.movimientos.pantallasDeMovimientos
import dagger.hilt.android.AndroidEntryPoint
import kotlin.uuid.Uuid

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
                val registrar = { clienteId: Uuid, tipo: TipoMovimiento ->
                    navController.navigate(RegistrarMovimiento(clienteId.toString(), tipo.name))
                }
                NavHost(navController = navController, startDestination = ListaClientes) {
                    pantallasDeClientes(
                        navController,
                        onCliente = { navController.navigate(HistorialCliente(it.toString())) },
                        onRegistrar = registrar,
                        onMetodosCobro = { navController.navigate(MetodosCobro) },
                        onMensajeDeCobro = { navController.navigate(MensajeDeCobro) },
                        onResumen = { navController.navigate(ResumenGeneral) },
                        onCuenta = { navController.navigate(Cuenta) },
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
                    )
                    pantallasDeCuenta(navController)
                }
                AvisoSesionTerminada()
            }
        }
    }
}
