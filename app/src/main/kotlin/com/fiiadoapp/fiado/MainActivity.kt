package com.fiiadoapp.fiado

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.fiiadoapp.fiado.feature.clientes.ListaClientes
import com.fiiadoapp.fiado.feature.clientes.pantallasDeClientes
import com.fiiadoapp.fiado.feature.movimientos.RegistrarMovimiento
import com.fiiadoapp.fiado.feature.movimientos.pantallasDeMovimientos
import com.fiiadoapp.fiado.ui.theme.FiadoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FiadoTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = ListaClientes) {
                    pantallasDeClientes(
                        navController,
                        onRegistrar = { clienteId, tipo -> navController.navigate(RegistrarMovimiento(clienteId.toString(), tipo.name)) },
                    )
                    pantallasDeMovimientos(
                        onCerrar = { navController.popBackStack() },
                        onRegistrado = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
