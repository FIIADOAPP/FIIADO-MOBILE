package com.fiiadoapp.fiado.feature.clientes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fiiadoapp.fiado.domain.Cliente
import com.fiiadoapp.fiado.domain.Dinero
import com.fiiadoapp.fiado.domain.ErrorCliente

@Composable
internal fun FormularioClientePantalla(
    onCerrar: () -> Unit,
    viewModel: FormularioClienteViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.terminado) { if (estado.terminado) onCerrar() }
    FormularioClienteContenido(
        estado = estado,
        onNombre = viewModel::cambiarNombre,
        onTelefono = viewModel::cambiarTelefono,
        onGuardar = viewModel::guardar,
        onArchivar = viewModel::archivar,
        onVolver = onCerrar,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FormularioClienteContenido(
    estado: EstadoFormularioCliente,
    onNombre: (String) -> Unit,
    onTelefono: (String) -> Unit,
    onGuardar: () -> Unit,
    onArchivar: () -> Unit,
    onVolver: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (estado.esEdicion) R.string.cliente_editar_titulo else R.string.cliente_nuevo_titulo))
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.volver))
                    }
                },
            )
        },
    ) { relleno ->
        if (estado.cargando) {
            Box(Modifier.padding(relleno).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            modifier =
                Modifier
                    .padding(relleno)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val errorNombre =
                when (estado.error) {
                    ErrorCliente.NombreVacio -> {
                        stringResource(R.string.cliente_error_nombre_vacio)
                    }

                    ErrorCliente.NombreMuyLargo -> {
                        pluralStringResource(
                            R.plurals.cliente_error_nombre_largo,
                            Cliente.LARGO_MAXIMO_NOMBRE,
                            Cliente.LARGO_MAXIMO_NOMBRE,
                        )
                    }

                    else -> {
                        null
                    }
                }
            OutlinedTextField(
                value = estado.nombre,
                onValueChange = onNombre,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.cliente_nombre)) },
                isError = errorNombre != null,
                supportingText = errorNombre?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
            OutlinedTextField(
                value = estado.telefono,
                onValueChange = onTelefono,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.cliente_telefono)) },
                isError = estado.telefonoInvalido,
                supportingText = {
                    Text(
                        stringResource(
                            if (estado.telefonoInvalido) R.string.cliente_error_telefono else R.string.cliente_telefono_ayuda,
                        ),
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            )
            Button(onClick = onGuardar, enabled = !estado.guardando, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.guardar))
            }
            if (estado.esEdicion) {
                HorizontalDivider()
                SeccionArchivar(saldo = estado.saldo, habilitado = !estado.guardando, onArchivar = onArchivar)
            }
        }
    }
}

@Composable
private fun SeccionArchivar(
    saldo: Dinero,
    habilitado: Boolean,
    onArchivar: () -> Unit,
) {
    val puedeArchivar = saldo == Dinero.CERO
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text =
                if (puedeArchivar) {
                    stringResource(R.string.cliente_archivar_ayuda)
                } else {
                    stringResource(R.string.cliente_archivar_con_saldo, textoDeSaldo(saldo))
                },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onArchivar, enabled = puedeArchivar && habilitado, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.cliente_archivar))
        }
    }
}
