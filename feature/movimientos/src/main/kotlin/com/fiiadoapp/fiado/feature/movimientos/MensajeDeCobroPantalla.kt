package com.fiiadoapp.fiado.feature.movimientos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fiiadoapp.fiado.core.ui.BarraSecundaria
import com.fiiadoapp.fiado.core.ui.MargenPantalla
import com.fiiadoapp.fiado.core.ui.Tarjeta
import com.fiiadoapp.fiado.domain.ErrorPlantilla
import com.fiiadoapp.fiado.domain.PlantillaRecordatorio

@Composable
internal fun MensajeDeCobroPantalla(
    onVolver: () -> Unit,
    viewModel: MensajeDeCobroViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.guardada) { if (estado.guardada) onVolver() }
    MensajeDeCobroContenido(
        estado = estado,
        onVolver = onVolver,
        onCambiar = viewModel::cambiar,
        onRestablecer = viewModel::restablecer,
        onGuardar = viewModel::guardar,
    )
}

@Composable
internal fun MensajeDeCobroContenido(
    estado: EstadoMensajeDeCobro,
    onVolver: () -> Unit,
    onCambiar: (String) -> Unit,
    onRestablecer: () -> Unit,
    onGuardar: () -> Unit,
) {
    Scaffold(
        topBar = { BarraSecundaria(stringResource(R.string.mensaje_titulo), onVolver) },
    ) { relleno ->
        Column(
            Modifier
                .padding(relleno)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MargenPantalla, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(
                    R.string.mensaje_ayuda,
                    PlantillaRecordatorio.CLIENTE,
                    PlantillaRecordatorio.SALDO,
                    PlantillaRecordatorio.NEGOCIO,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                shape = MaterialTheme.shapes.small,
                value = estado.texto,
                onValueChange = onCambiar,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.mensaje_campo)) },
                minLines = 4,
                isError = estado.error != null,
                supportingText = estado.error?.let { { Text(textoDeError(it)) } },
            )
            estado.vistaPrevia?.let {
                Text(stringResource(R.string.mensaje_vista_previa), style = MaterialTheme.typography.labelLarge)
                Tarjeta(Modifier.fillMaxWidth()) { Text(it, style = MaterialTheme.typography.bodyLarge) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                TextButton(onClick = onRestablecer) { Text(stringResource(R.string.mensaje_restablecer)) }
                Button(onClick = onGuardar) { Text(stringResource(R.string.mensaje_guardar)) }
            }
        }
    }
}

@Composable
private fun textoDeError(error: ErrorPlantilla): String =
    when (error) {
        ErrorPlantilla.VACIA -> {
            stringResource(R.string.mensaje_error_vacio)
        }

        ErrorPlantilla.LARGA -> {
            pluralStringResource(R.plurals.mensaje_error_largo, PlantillaRecordatorio.LARGO_MAXIMO, PlantillaRecordatorio.LARGO_MAXIMO)
        }

        ErrorPlantilla.SIN_SALDO -> {
            stringResource(R.string.mensaje_error_sin_saldo, PlantillaRecordatorio.SALDO)
        }
    }
