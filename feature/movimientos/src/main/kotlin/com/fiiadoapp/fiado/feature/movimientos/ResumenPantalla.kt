package com.fiiadoapp.fiado.feature.movimientos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fiiadoapp.fiado.domain.Dinero
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FORMATO_MES = DateTimeFormatter.ofPattern("MMMM", Locale.forLanguageTag("es-PE"))

@Composable
internal fun ResumenPantalla(
    onVolver: () -> Unit,
    viewModel: ResumenViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    ResumenContenido(estado, onVolver)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ResumenContenido(
    estado: EstadoResumen?,
    onVolver: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.resumen_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.metodos_volver))
                    }
                },
            )
        },
    ) { relleno ->
        if (estado == null) {
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
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.resumen_por_cobrar), style = MaterialTheme.typography.titleMedium)
                    Text(
                        estado.porCobrar.formatear(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (estado.porCobrar.esPositivo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        stringResource(R.string.resumen_por_cobrar_ayuda),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TarjetaPeriodo(stringResource(R.string.resumen_hoy), estado.delDia)
            TarjetaPeriodo(stringResource(R.string.resumen_mes, estado.hoy.format(FORMATO_MES)), estado.delMes)
        }
    }
}

@Composable
private fun TarjetaPeriodo(
    titulo: String,
    periodo: Periodo,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Linea(stringResource(R.string.resumen_fiado), periodo.fiado, MaterialTheme.typography.bodyLarge)
            Linea(stringResource(R.string.resumen_cobrado), periodo.cobrado, MaterialTheme.typography.bodyLarge)
            if (periodo.porMetodo.isNotEmpty()) {
                HorizontalDivider()
                periodo.porMetodo.forEach { Linea(it.nombre, it.monto, MaterialTheme.typography.bodyMedium, sangria = true) }
            }
        }
    }
}

@Composable
private fun Linea(
    etiqueta: String,
    monto: Dinero,
    estilo: TextStyle,
    sangria: Boolean = false,
) {
    Row(Modifier.fillMaxWidth().padding(start = if (sangria) 16.dp else 0.dp)) {
        Text(etiqueta, style = estilo, modifier = Modifier.weight(1f))
        Text(monto.formatear(), style = estilo, fontWeight = FontWeight.Medium)
    }
}
