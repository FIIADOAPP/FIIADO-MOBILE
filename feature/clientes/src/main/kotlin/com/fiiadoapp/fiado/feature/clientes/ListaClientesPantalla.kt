package com.fiiadoapp.fiado.feature.clientes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fiiadoapp.fiado.core.ui.Avatar
import com.fiiadoapp.fiado.core.ui.EncabezadoGrande
import com.fiiadoapp.fiado.core.ui.LocalEspacioBarraInferior
import com.fiiadoapp.fiado.core.ui.MargenPantalla
import com.fiiadoapp.fiado.core.ui.SaldoTexto
import com.fiiadoapp.fiado.core.ui.Tarjeta
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
    onCliente: (Uuid) -> Unit,
    onRegistrar: (Uuid, TipoMovimiento) -> Unit,
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
        onCliente = onCliente,
        onRegistrar = onRegistrar,
        onResumen = onResumen,
        onDeshacer = viewModel::deshacer,
        onDescartarAviso = viewModel::descartarAviso,
    )
}

@Composable
internal fun ListaClientesContenido(
    busqueda: String,
    clientes: List<ClienteConSaldo>?,
    porCobrar: Dinero?,
    aviso: AvisoRegistro?,
    onBuscar: (String) -> Unit,
    onCliente: (Uuid) -> Unit,
    onRegistrar: (Uuid, TipoMovimiento) -> Unit,
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
    val subtitulo = clientes?.let { pluralStringResource(R.plurals.clientes_cantidad, it.size, it.size) }
    Scaffold(
        topBar = { EncabezadoGrande(stringResource(R.string.clientes_titulo), Modifier.statusBarsPadding(), subtitulo) },
        // Por encima de la barra inferior flotante, para que el aviso y su "Deshacer" se vean.
        snackbarHost = { SnackbarHost(avisos, Modifier.padding(bottom = LocalEspacioBarraInferior.current)) },
    ) { relleno ->
        LazyColumn(
            modifier = Modifier.padding(relleno).fillMaxSize(),
            contentPadding = PaddingValues(start = MargenPantalla, end = MargenPantalla, bottom = LocalEspacioBarraInferior.current),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (porCobrar != null) item(key = "por-cobrar") { TarjetaPorCobrar(porCobrar, onResumen) }
            item(key = "buscador") { Buscador(busqueda, onBuscar) }
            when {
                clientes == null -> {
                    item(key = "cargando") { Centrado { CircularProgressIndicator() } }
                }

                clientes.isEmpty() && busqueda.isBlank() -> {
                    item(key = "vacio") { Mensaje(stringResource(R.string.clientes_vacio)) }
                }

                clientes.isEmpty() -> {
                    item(
                        key = "sin-resultados",
                    ) { Mensaje(stringResource(R.string.clientes_sin_resultados, busqueda.trim())) }
                }

                else -> {
                    items(
                        clientes,
                        key = { it.cliente.id.toString() },
                    ) { (cliente, saldo) -> FilaCliente(cliente, saldo, onCliente, onRegistrar) }
                }
            }
        }
    }
}

@Composable
private fun Buscador(
    busqueda: String,
    onBuscar: (String) -> Unit,
) {
    TextField(
        value = busqueda,
        onValueChange = onBuscar,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        placeholder = { Text(stringResource(R.string.clientes_buscar)) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
            if (busqueda.isNotEmpty()) {
                IconButton(onClick = { onBuscar("") }) {
                    Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.clientes_limpiar_busqueda))
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
    )
}

/** Fiar o cobrar empieza con un toque desde la tarjeta; tocar el resto abre el historial. */
@Composable
private fun FilaCliente(
    cliente: Cliente,
    saldo: Dinero,
    onCliente: (Uuid) -> Unit,
    onRegistrar: (Uuid, TipoMovimiento) -> Unit,
) {
    Tarjeta(Modifier.fillMaxWidth(), onClick = { onCliente(cliente.id) }, relleno = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(cliente.nombre)
            Column(Modifier.weight(1f)) {
                Text(cliente.nombre, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                SaldoTexto(saldo, estilo = MaterialTheme.typography.bodyMedium)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                onClick = { onRegistrar(cliente.id, TipoMovimiento.FIADO) },
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.clientes_fiar)) }
            FilledTonalButton(
                onClick = { onRegistrar(cliente.id, TipoMovimiento.PAGO) },
                modifier = Modifier.weight(1f),
                colors =
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
            ) { Text(stringResource(R.string.clientes_cobrar)) }
        }
    }
}

/** Lo pendiente de todos los clientes; un toque lleva al resumen de hoy y del mes. */
@Composable
private fun TarjetaPorCobrar(
    porCobrar: Dinero,
    onResumen: () -> Unit,
) {
    Tarjeta(Modifier.fillMaxWidth(), onClick = onResumen, relleno = 20.dp) {
        Text(stringResource(R.string.clientes_por_cobrar), style = MaterialTheme.typography.titleMedium)
        Text(
            porCobrar.formatear(),
            style = MaterialTheme.typography.displaySmall,
            color = if (porCobrar.esPositivo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
        )
        Row(
            Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.clientes_ver_resumen), style = MaterialTheme.typography.labelLarge)
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun Centrado(contenido: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { contenido() }
}

@Composable
private fun Mensaje(texto: String) {
    Text(
        text = texto,
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 12.dp),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
