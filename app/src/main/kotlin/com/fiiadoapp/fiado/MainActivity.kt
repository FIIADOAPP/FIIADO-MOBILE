package com.fiiadoapp.fiado

import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fiiadoapp.fiado.bloqueo.AutenticadorBiometrico
import com.fiiadoapp.fiado.bloqueo.BloqueoViewModel
import com.fiiadoapp.fiado.bloqueo.EstadoBloqueo
import com.fiiadoapp.fiado.bloqueo.PantallaBloqueada
import com.fiiadoapp.fiado.core.ui.AltoBarraInferior
import com.fiiadoapp.fiado.core.ui.BarraInferiorFlotante
import com.fiiadoapp.fiado.core.ui.ControlDeAnuncios
import com.fiiadoapp.fiado.core.ui.DestinoBarra
import com.fiiadoapp.fiado.core.ui.FiadoTheme
import com.fiiadoapp.fiado.core.ui.IconosFiado
import com.fiiadoapp.fiado.core.ui.LocalAutenticador
import com.fiiadoapp.fiado.core.ui.LocalControlDeAnuncios
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
import com.fiiadoapp.fiado.publicidad.BannerAnuncio
import com.fiiadoapp.fiado.publicidad.Publicidad
import com.fiiadoapp.fiado.publicidad.alto
import com.fiiadoapp.fiado.publicidad.tamanoDeBanner
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.reflect.KClass
import kotlin.uuid.Uuid

/** Índices de Clientes y Resumen en [PRINCIPALES]: las únicas pantallas con banner. */
private val CON_BANNER = setOf(0, 1)

/** Las cuatro pantallas principales, en el orden de la barra inferior. */
private val PRINCIPALES: List<Pair<Any, KClass<*>>> =
    listOf(
        ListaClientes to ListaClientes::class,
        ResumenGeneral to ResumenGeneral::class,
        Cobros to Cobros::class,
        Cuenta to Cuenta::class,
    )

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val bloqueo: BloqueoViewModel by viewModels()

    @Inject lateinit var publicidad: Publicidad

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La app es siempre oscura: barras del sistema transparentes con íconos claros.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        vigilarSegundoPlano()
        ocultarEnRecientesSiHayBloqueo()
        val autenticador = AutenticadorBiometrico(this)
        publicidad.pedirConsentimiento(this)
        val controlDeAnuncios = controlDeAnuncios()
        setContent {
            FiadoTheme {
                CompositionLocalProvider(LocalAutenticador provides autenticador, LocalControlDeAnuncios provides controlDeAnuncios) {
                    val estado by bloqueo.estado.collectAsStateWithLifecycle()
                    when (estado) {
                        EstadoBloqueo.CARGANDO -> {
                            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}
                        }

                        EstadoBloqueo.BLOQUEADA -> {
                            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                                val titulo = stringResource(R.string.bloqueo_pedido)
                                PantallaBloqueada(
                                    onDesbloquear = {
                                        if (autenticador.disponible()) {
                                            autenticador.autenticar(titulo) { bloqueo.desbloquear() }
                                        } else {
                                            bloqueo.desactivarPorElTelefono()
                                        }
                                    },
                                )
                            }
                        }

                        EstadoBloqueo.ABIERTA -> {
                            Aplicacion(publicidad)
                        }
                    }
                    AvisoBloqueoDesactivado()
                }
            }
        }
    }

    /** La compra y la privacidad de los anuncios, con esta actividad para abrir los diálogos de Google. */
    private fun controlDeAnuncios() =
        object : ControlDeAnuncios {
            override val estado = publicidad.estado
            override val opcionesDePrivacidadRequeridas = publicidad.opcionesDePrivacidadRequeridas

            override fun comprar() = publicidad.comprar(this@MainActivity)

            override fun restaurar() = publicidad.restaurar()

            override fun abrirOpcionesDePrivacidad() = publicidad.abrirOpcionesDePrivacidad(this@MainActivity)
        }

    /** El minuto en segundo plano se mide con el reloj monótono del sistema (ADR-0006). */
    private fun vigilarSegundoPlano() {
        lifecycle.addObserver(
            LifecycleEventObserver { _, evento ->
                when (evento) {
                    Lifecycle.Event.ON_STOP -> {
                        bloqueo.alSalir(SystemClock.elapsedRealtime())
                    }

                    Lifecycle.Event.ON_START -> {
                        bloqueo.alVolver(SystemClock.elapsedRealtime())
                    }

                    else -> {}
                }
            },
        )
    }

    /** Con el bloqueo activo, la vista previa de apps recientes no muestra saldos. */
    private fun ocultarEnRecientesSiHayBloqueo() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                bloqueo.activo.collect { activo ->
                    if (activo) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
            }
        }
    }

    @Composable
    private fun AvisoBloqueoDesactivado() {
        val mostrar by bloqueo.desactivadoPorElTelefono.collectAsStateWithLifecycle()
        if (!mostrar) return
        AlertDialog(
            onDismissRequest = bloqueo::descartarAviso,
            title = { Text(stringResource(R.string.bloqueo_desactivado_titulo)) },
            text = { Text(stringResource(R.string.bloqueo_desactivado_explicacion)) },
            confirmButton = { TextButton(onClick = bloqueo::descartarAviso) { Text(stringResource(R.string.bloqueo_entendido)) } },
        )
    }
}

@Composable
private fun Aplicacion(publicidad: Publicidad) {
    val navController = rememberNavController()
    val entrada by navController.currentBackStackEntryAsState()
    val principal = PRINCIPALES.indexOfFirst { (_, ruta) -> entrada?.destination?.hasRoute(ruta) == true }.takeIf { it >= 0 }
    val mostrarAnuncios by publicidad.mostrarAnuncios.collectAsStateWithLifecycle()
    // El banner solo va en Clientes y Resumen, nunca mientras se fía, se cobra o se edita (ADR-0011).
    val conBanner = mostrarAnuncios && principal != null && principal in CON_BANNER
    val tamano = tamanoDeBanner()
    // Surface da el fondo y el color de texto del tema también a las pantallas sin Scaffold.
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            val espacio = AltoBarraInferior + if (conBanner) tamano.alto() else 0.dp
            CompositionLocalProvider(LocalEspacioBarraInferior provides espacio) {
                Navegacion(navController)
            }
            if (principal != null) {
                Column(Modifier.align(Alignment.BottomCenter)) {
                    if (conBanner) BannerAnuncio(tamano)
                    BarraInferiorFlotante(
                        destinos = destinos(),
                        seleccionado = principal,
                        onDestino = { navController.irAPrincipal(PRINCIPALES[it].first) },
                        descripcionCentral = stringResource(R.string.barra_nuevo_cliente),
                        onCentral = { navController.navigate(FormularioCliente()) },
                    )
                }
            }
        }
    }
    AvisoSesionTerminada()
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
            onHistorial = { navController.navigate(HistorialCliente(it.toString())) },
        )
        pantallasDeCuenta(navController)
    }
}
