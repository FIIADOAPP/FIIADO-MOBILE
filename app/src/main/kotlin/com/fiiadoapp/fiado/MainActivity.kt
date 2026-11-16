package com.fiiadoapp.fiado

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.fiiadoapp.fiado.domain.TipoMovimiento
import com.fiiadoapp.fiado.feature.clientes.FormularioCliente
import com.fiiadoapp.fiado.feature.clientes.ListaClientes
import com.fiiadoapp.fiado.feature.clientes.pantallasDeClientes
import com.fiiadoapp.fiado.feature.movimientos.HistorialCliente
import com.fiiadoapp.fiado.feature.movimientos.MetodosCobro
import com.fiiadoapp.fiado.feature.movimientos.RegistrarMovimiento
import com.fiiadoapp.fiado.feature.movimientos.pantallasDeMovimientos
import com.fiiadoapp.fiado.ui.theme.FiadoTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlin.uuid.Uuid

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                }
            }
        }
    }
}
