package com.fiiadoapp.fiado.feature.clientes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fiiadoapp.fiado.domain.ClienteConSaldo
import kotlin.uuid.Uuid

@Composable
internal fun ListaClientesPantalla(
    onNuevo: () -> Unit,
    onCliente: (Uuid) -> Unit,
    viewModel: ListaClientesViewModel = hiltViewModel(),
) {
    val busqueda by viewModel.busqueda.collectAsStateWithLifecycle()
    val clientes by viewModel.clientes.collectAsStateWithLifecycle()
    ListaClientesContenido(
        busqueda = busqueda,
        clientes = clientes,
        onBuscar = viewModel::buscar,
        onNuevo = onNuevo,
        onCliente = onCliente,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ListaClientesContenido(
    busqueda: String,
    clientes: List<ClienteConSaldo>?,
    onBuscar: (String) -> Unit,
    onNuevo: () -> Unit,
    onCliente: (Uuid) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.clientes_titulo)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNuevo,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.clientes_nuevo)) },
            )
        },
    ) { relleno ->
        Column(Modifier.padding(relleno).fillMaxSize()) {
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
                else -> ListaDeClientes(clientes, onCliente)
            }
        }
    }
}

@Composable
private fun ListaDeClientes(
    clientes: List<ClienteConSaldo>,
    onCliente: (Uuid) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
        items(clientes, key = { it.cliente.id.toString() }) { (cliente, saldo) ->
            ListItem(
                headlineContent = { Text(cliente.nombre, style = MaterialTheme.typography.titleMedium) },
                supportingContent = cliente.telefono?.let { telefono -> { Text(telefono.formatear()) } },
                trailingContent = { SaldoTexto(saldo) },
                modifier = Modifier.clickable { onCliente(cliente.id) },
            )
            HorizontalDivider()
        }
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
