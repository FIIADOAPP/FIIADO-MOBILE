package com.fiiadoapp.fiado.feature.clientes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fiiadoapp.fiado.core.ui.SaldoTexto
import com.fiiadoapp.fiado.domain.Cliente
import com.fiiadoapp.fiado.domain.ClienteConSaldo
import com.fiiadoapp.fiado.domain.Dinero
import com.fiiadoapp.fiado.domain.TipoMovimiento
import kotlin.uuid.Uuid

/** [movimientoRegistrado] es el id que devuelve el registro al cerrarse; se muestra una vez y se consume. */
@Composable
internal fun ListaClientesPantalla(
    movimientoRegistrado: Uuid?,
    onAvisoConsumido: () -> Unit,
    onNuevo: () -> Unit,
    onCliente: (Uuid) -> Unit,
    onRegistrar: (Uuid, TipoMovimiento) -> Unit,
    onMetodosCobro: () -> Unit,
    onResumen: () -> Unit,
    viewModel: ListaClientesViewModel = hiltViewModel(),
) {
    val busqueda by viewModel.busqueda.collectAsStateWithLifecycle()
    val clientes by viewModel.lista.collectAsStateWithLifecycle()
    val aviso by viewModel.aviso.collectAsStateWithLifecycle()
    val porCobrar by viewModel.porCobrar.collectAsStateWithLifecycle()
    val foco = LocalFocusManager.current
    // Al cerrarse el registro, Compose devuelve el foco al buscador y abre el teclado sobre la lista.
    LifecycleResumeEffect(Unit) {
        foco.clearFocus()
        onPauseOrDispose {}
    }
    LaunchedEffect(movimientoRegistrado) {
        movimientoRegistrado?.let {
            viewModel.avisarRegistro(it)
            onAvisoConsumido()
        }
    }
    ListaClientesContenido(
        busqueda = busqueda,
        clientes = clientes,
        porCobrar = porCobrar,
        aviso = aviso,
        onBuscar = viewModel::buscar,
        onNuevo = onNuevo,
        onCliente = onCliente,
        onRegistrar = onRegistrar,
        onMetodosCobro = onMetodosCobro,
        onResumen = onResumen,
        onDeshacer = viewModel::deshacer,
        onDescartarAviso = viewModel::descartarAviso,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ListaClientesContenido(
    busqueda: String,
    clientes: List<ClienteConSaldo>?,
    porCobrar: Dinero?,
    aviso: AvisoRegistro?,
    onBuscar: (String) -> Unit,
    onNuevo: () -> Unit,
    onCliente: (Uuid) -> Unit,
    onRegistrar: (Uuid, TipoMovimiento) -> Unit,
    onMetodosCobro: () -> Unit,
    onResumen: () -> Unit,
    onDeshacer: () -> Unit,
    onDescartarAviso: () -> Unit,
) {
    val avisos = remember { SnackbarHostState() }
    if (aviso != null) {
        val mensaje =
            stringResource(
                if (aviso.tipo == TipoMovimiento.FIADO) R.string.aviso_fiado else R.string.aviso_pago,
                aviso.monto.formatear(),
                aviso.nombreCliente,
            )
        val accion = stringResource(R.string.aviso_deshacer)
        LaunchedEffect(aviso) {
            val resultado = avisos.showSnackbar(mensaje, actionLabel = accion, duration = SnackbarDuration.Long)
            if (resultado == SnackbarResult.ActionPerformed) onDeshacer() else onDescartarAviso()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.clientes_titulo)) },
                actions = { MenuDeOpciones(onMetodosCobro) },
            )
        },
        snackbarHost = { SnackbarHost(avisos) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNuevo,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.clientes_nuevo)) },
            )
        },
    ) { relleno ->
        Column(Modifier.padding(relleno).fillMaxSize()) {
            if (porCobrar != null) TarjetaPorCobrar(porCobrar, onResumen)
            OutlinedTextField(
                value = busqueda,
                onValueChange = onBuscar,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.clientes_buscar)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (busqueda.isNotEmpty()) {
                        IconButton(onClick = { onBuscar("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.clientes_limpiar_busqueda))
                        }
                    }
                },
                singleLine = true,
            )
            when {
                clientes == null -> Centrado { CircularProgressIndicator() }
                clientes.isEmpty() && busqueda.isBlank() -> Centrado { Mensaje(stringResource(R.string.clientes_vacio)) }
                clientes.isEmpty() -> Centrado { Mensaje(stringResource(R.string.clientes_sin_resultados, busqueda.trim())) }
                else -> ListaDeClientes(clientes, onCliente, onRegistrar)
            }
        }
    }
}

@Composable
private fun ListaDeClientes(
    clientes: List<ClienteConSaldo>,
    onCliente: (Uuid) -> Unit,
    onRegistrar: (Uuid, TipoMovimiento) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
        items(clientes, key = { it.cliente.id.toString() }) { (cliente, saldo) ->
            FilaCliente(cliente, saldo, onCliente, onRegistrar)
            HorizontalDivider()
        }
    }
}

/** Fiar o cobrar empieza con un toque desde la fila. */
@Composable
private fun FilaCliente(
    cliente: Cliente,
    saldo: Dinero,
    onCliente: (Uuid) -> Unit,
    onRegistrar: (Uuid, TipoMovimiento) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onCliente(
                        cliente.id,
                    )
                }.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(cliente.nombre, style = MaterialTheme.typography.titleMedium)
            SaldoTexto(saldo)
        }
        FilledTonalButton(onClick = { onRegistrar(cliente.id, TipoMovimiento.FIADO) }) {
            Text(stringResource(R.string.clientes_fiar))
        }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(onClick = { onRegistrar(cliente.id, TipoMovimiento.PAGO) }) {
            Text(stringResource(R.string.clientes_cobrar))
        }
    }
}

/** Lo pendiente de todos los clientes; un toque lleva al resumen de hoy y del mes. */
@Composable
private fun TarjetaPorCobrar(
    porCobrar: Dinero,
    onResumen: () -> Unit,
) {
    Card(onClick = onResumen, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.clientes_por_cobrar), style = MaterialTheme.typography.labelLarge)
                Text(porCobrar.formatear(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Text(stringResource(R.string.clientes_ver_resumen), color = MaterialTheme.colorScheme.primary)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun MenuDeOpciones(onMetodosCobro: () -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    IconButton(onClick = { abierto = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.clientes_mas_opciones))
    }
    DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.clientes_metodos_cobro)) },
            onClick = {
                abierto = false
                onMetodosCobro()
            },
        )
    }
}

@Composable
private fun Centrado(contenido: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { contenido() }
}

@Composable
private fun Mensaje(texto: String) {
    Column(verticalArrangement = Arrangement.Center) {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
