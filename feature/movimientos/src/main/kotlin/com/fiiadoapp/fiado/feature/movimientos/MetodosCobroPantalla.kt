package com.fiiadoapp.fiado.feature.movimientos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fiiadoapp.fiado.domain.ErrorMetodoCobro
import com.fiiadoapp.fiado.domain.MetodoCobro

@Composable
internal fun MetodosCobroPantalla(
    onVolver: () -> Unit,
    viewModel: MetodosCobroViewModel = hiltViewModel(),
) {
    val metodos by viewModel.metodos.collectAsStateWithLifecycle()
    val edicion by viewModel.edicion.collectAsStateWithLifecycle()
    MetodosCobroContenido(
        metodos = metodos,
        edicion = edicion,
        onVolver = onVolver,
        onNuevo = viewModel::nuevo,
        onEditar = viewModel::editar,
        onNombre = viewModel::cambiarNombre,
        onGuardar = viewModel::guardar,
        onArchivar = viewModel::archivar,
        onCancelar = viewModel::cancelar,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MetodosCobroContenido(
    metodos: List<MetodoCobro>?,
    edicion: EdicionMetodo?,
    onVolver: () -> Unit,
    onNuevo: () -> Unit,
    onEditar: (MetodoCobro) -> Unit,
    onNombre: (String) -> Unit,
    onGuardar: () -> Unit,
    onArchivar: () -> Unit,
    onCancelar: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.metodos_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.metodos_volver))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNuevo,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.metodos_nuevo)) },
            )
        },
    ) { relleno ->
        if (metodos == null) {
            Box(Modifier.padding(relleno).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(Modifier.padding(relleno), contentPadding = PaddingValues(bottom = 88.dp)) {
                item {
                    Text(
                        text = stringResource(R.string.metodos_ayuda),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(metodos, key = { it.id.toString() }) { metodo ->
                    ListItem(
                        headlineContent = { Text(metodo.nombre, style = MaterialTheme.typography.titleMedium) },
                        modifier = Modifier.clickable { onEditar(metodo) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
    if (edicion != null) {
        DialogoMetodo(edicion, onNombre, onGuardar, onArchivar, onCancelar)
    }
}

@Composable
private fun DialogoMetodo(
    edicion: EdicionMetodo,
    onNombre: (String) -> Unit,
    onGuardar: () -> Unit,
    onArchivar: () -> Unit,
    onCancelar: () -> Unit,
) {
    val error =
        when (edicion.error) {
            ErrorMetodoCobro.NombreVacio -> {
                stringResource(R.string.metodos_error_vacio)
            }

            ErrorMetodoCobro.NombreRepetido -> {
                stringResource(R.string.metodos_error_repetido)
            }

            ErrorMetodoCobro.UltimoActivo -> {
                stringResource(R.string.metodos_error_ultimo)
            }

            ErrorMetodoCobro.NombreMuyLargo -> {
                pluralStringResource(R.plurals.metodos_error_largo, MetodoCobro.LARGO_MAXIMO_NOMBRE, MetodoCobro.LARGO_MAXIMO_NOMBRE)
            }

            null -> {
                null
            }
        }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(if (edicion.metodo == null) R.string.metodos_nuevo else R.string.metodos_editar)) },
        text = {
            OutlinedTextField(
                value = edicion.nombre,
                onValueChange = onNombre,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.metodos_nombre)) },
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        },
        confirmButton = { TextButton(onClick = onGuardar) { Text(stringResource(R.string.registro_guardar)) } },
        dismissButton = {
            if (edicion.metodo != null) {
                TextButton(onClick = onArchivar) { Text(stringResource(R.string.metodos_archivar)) }
            }
            TextButton(onClick = onCancelar) { Text(stringResource(R.string.registro_cancelar)) }
        },
    )
}
