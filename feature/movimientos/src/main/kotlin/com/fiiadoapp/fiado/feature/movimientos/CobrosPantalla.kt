package com.fiiadoapp.fiado.feature.movimientos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fiiadoapp.fiado.core.ui.EncabezadoGrande
import com.fiiadoapp.fiado.core.ui.LocalEspacioBarraInferior
import com.fiiadoapp.fiado.core.ui.MargenPantalla
import com.fiiadoapp.fiado.core.ui.Tarjeta

/** Reúne lo que se configura para cobrar: los métodos de cobro y el mensaje del recordatorio. */
@Composable
internal fun CobrosPantalla(
    onMetodosCobro: () -> Unit,
    onMensajeDeCobro: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        EncabezadoGrande(stringResource(R.string.cobros_titulo), subtitulo = stringResource(R.string.cobros_subtitulo))
        Column(Modifier.padding(horizontal = MargenPantalla), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Opcion(stringResource(R.string.metodos_titulo), stringResource(R.string.cobros_metodos_ayuda), onMetodosCobro)
            Opcion(stringResource(R.string.mensaje_titulo), stringResource(R.string.cobros_mensaje_ayuda), onMensajeDeCobro)
        }
        Spacer(Modifier.height(LocalEspacioBarraInferior.current))
    }
}

@Composable
private fun Opcion(
    titulo: String,
    ayuda: String,
    onClick: () -> Unit,
) {
    Tarjeta(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Text(ayuda, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
