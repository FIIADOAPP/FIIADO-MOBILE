package com.fiiadoapp.fiado.feature.cuenta

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.fiiadoapp.fiado.core.ui.BarraSecundaria
import com.fiiadoapp.fiado.core.ui.EncabezadoGrande
import com.fiiadoapp.fiado.core.ui.LocalEspacioBarraInferior
import com.fiiadoapp.fiado.domain.ErrorCuenta

/**
 * Pantalla de la cuenta con contenido desplazable. Con [onVolver] es un paso con su barra para volver;
 * sin él, es la pantalla principal de la barra inferior, con el título grande.
 */
@Composable
internal fun PantallaDeCuenta(
    titulo: String,
    onVolver: (() -> Unit)?,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            if (onVolver == null) {
                EncabezadoGrande(titulo, Modifier.statusBarsPadding())
            } else {
                BarraSecundaria(titulo, onVolver)
            }
        },
    ) { relleno ->
        Column(
            modifier =
                Modifier
                    .padding(relleno)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            contenido()
            Spacer(Modifier.height(if (onVolver == null) LocalEspacioBarraInferior.current else 0.dp))
        }
    }
}

@Composable
internal fun Campo(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    tipo: KeyboardType = KeyboardType.Text,
    secreto: Boolean = false,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(etiqueta) },
        singleLine = true,
        visualTransformation = if (secreto) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = tipo),
    )
}

/** El mensaje de la API ya viene listo para mostrar; sin conexión se explica en la app. */
@Composable
internal fun MensajeDeError(error: ErrorCuenta?) {
    val texto =
        when (error) {
            null -> return
            ErrorCuenta.SinConexion -> stringResource(R.string.cuenta_sin_conexion)
            is ErrorCuenta.DeLaApi -> error.mensaje
            is ErrorCuenta.OtraCuenta -> stringResource(R.string.cuenta_otra_cuenta, error.correoVinculado)
        }
    Text(texto, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
}

@Composable
internal fun BotonPrincipal(
    texto: String,
    enviando: Boolean,
    habilitado: Boolean = true,
    onClick: () -> Unit,
) {
    Button(onClick = onClick, enabled = habilitado && !enviando, modifier = Modifier.fillMaxWidth()) {
        if (enviando) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) else Text(texto)
    }
}
