package com.fiiadoapp.fiado.feature.movimientos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fiiadoapp.fiado.core.ui.SaldoTexto
import com.fiiadoapp.fiado.domain.Movimiento
import com.fiiadoapp.fiado.domain.TipoMovimiento
import java.time.LocalDate
import kotlin.uuid.Uuid

@Composable
internal fun HistorialPantalla(
    onVolver: () -> Unit,
    onEditar: (Uuid) -> Unit,
    onRegistrar: (Uuid, TipoMovimiento) -> Unit,
    viewModel: HistorialViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val porAnular by viewModel.porAnular.collectAsStateWithLifecycle()
    HistorialContenido(
        estado = estado,
        hoy = viewModel.hoy,
        porAnular = porAnular,
        onVolver = onVolver,
        onEditar = onEditar,
        onRegistrar = onRegistrar,
        onPedirAnulacion = viewModel::pedirAnulacion,
        onConfirmarAnulacion = viewModel::confirmarAnulacion,
        onCancelarAnulacion = viewModel::cancelarAnulacion,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistorialContenido(
    estado: EstadoHistorial?,
    hoy: LocalDate,
    porAnular: Movimiento?,
    onVolver: () -> Unit,
    onEditar: (Uuid) -> Unit,
    onRegistrar: (Uuid, TipoMovimiento) -> Unit,
    onPedirAnulacion: (Movimiento) -> Unit,
    onConfirmarAnulacion: () -> Unit,
    onCancelarAnulacion: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(estado?.cliente?.nombre.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.metodos_volver))
                    }
                },
                actions = {
                    if (estado != null) {
                        IconButton(onClick = { onEditar(estado.cliente.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.historial_editar_cliente))
                        }
                    }
                },
            )
        },
    ) { relleno ->
        if (estado == null) {
            Box(Modifier.padding(relleno).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(Modifier.padding(relleno).fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item { Cabecera(estado, onRegistrar) }
            if (estado.filas.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.historial_vacio),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(estado.filas, key = { it.movimiento.id.toString() }) { fila ->
                FilaMovimiento(fila, hoy, onPedirAnulacion)
                HorizontalDivider()
            }
        }
    }
    if (porAnular != null) {
        ConfirmarAnulacion(porAnular, onConfirmarAnulacion, onCancelarAnulacion)
    }
}

@Composable
private fun Cabecera(
    estado: EstadoHistorial,
    onRegistrar: (Uuid, TipoMovimiento) -> Unit,
) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SaldoTexto(estado.saldo)
        estado.cliente.telefono?.let {
            Text(it.formatear(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { onRegistrar(estado.cliente.id, TipoMovimiento.FIADO) }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.historial_fiar))
            }
            OutlinedButton(onClick = { onRegistrar(estado.cliente.id, TipoMovimiento.PAGO) }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.historial_cobrar))
            }
        }
        if (estado.filas.any { it.movimiento.estaVigente }) {
            Text(
                stringResource(R.string.historial_ayuda),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Un fiado suma a la deuda y un pago la resta; un anulado queda tachado y no se puede tocar. */
@Composable
private fun FilaMovimiento(
    fila: FilaHistorial,
    hoy: LocalDate,
    onPedirAnulacion: (Movimiento) -> Unit,
) {
    val movimiento = fila.movimiento
    val tachado = if (movimiento.estaVigente) TextDecoration.None else TextDecoration.LineThrough
    val titulo =
        when (movimiento.tipo) {
            TipoMovimiento.FIADO -> stringResource(R.string.historial_fiado)
            TipoMovimiento.PAGO -> stringResource(R.string.historial_pago, fila.nombreMetodo.orEmpty())
        }
    val detalle = listOfNotNull(textoDeFecha(movimiento.fecha, hoy), movimiento.nota).joinToString(" · ")
    ListItem(
        headlineContent = { Text(titulo, textDecoration = tachado) },
        supportingContent = {
            Column {
                Text(detalle)
                if (!movimiento.estaVigente) {
                    Text(stringResource(R.string.historial_anulado), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        trailingContent = {
            Text(
                text = (if (movimiento.tipo == TipoMovimiento.FIADO) "+ " else "− ") + movimiento.monto.formatear(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textDecoration = tachado,
                color =
                    when {
                        !movimiento.estaVigente -> MaterialTheme.colorScheme.onSurfaceVariant
                        movimiento.tipo == TipoMovimiento.FIADO -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    },
            )
        },
        modifier = Modifier.fillMaxWidth().clickable(enabled = movimiento.estaVigente) { onPedirAnulacion(movimiento) },
    )
}

@Composable
private fun ConfirmarAnulacion(
    movimiento: Movimiento,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) {
    val tipo = stringResource(if (movimiento.tipo == TipoMovimiento.FIADO) R.string.historial_este_fiado else R.string.historial_este_pago)
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.historial_anular_titulo, tipo, movimiento.monto.formatear())) },
        text = { Text(stringResource(R.string.historial_anular_explicacion)) },
        confirmButton = { TextButton(onClick = onConfirmar) { Text(stringResource(R.string.historial_anular)) } },
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.registro_cancelar)) } },
    )
}
