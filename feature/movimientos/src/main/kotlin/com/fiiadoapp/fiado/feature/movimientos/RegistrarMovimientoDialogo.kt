package com.fiiadoapp.fiado.feature.movimientos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fiiadoapp.fiado.core.ui.SaldoTexto
import com.fiiadoapp.fiado.domain.ErrorMovimiento
import com.fiiadoapp.fiado.domain.Movimiento
import com.fiiadoapp.fiado.domain.TipoMovimiento
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.uuid.Uuid

private const val MILISEGUNDOS_POR_DIA = 86_400_000L

@Composable
internal fun RegistrarMovimientoDialogo(
    onCerrar: () -> Unit,
    onRegistrado: (Uuid) -> Unit,
    viewModel: RegistrarMovimientoViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.registrado) { estado.registrado?.let(onRegistrado) }
    RegistrarMovimientoContenido(
        estado = estado,
        onMonto = viewModel::cambiarMonto,
        onNota = viewModel::cambiarNota,
        onFecha = viewModel::cambiarFecha,
        onGuardar = viewModel::guardar,
        onCancelar = onCerrar,
    )
}

@Composable
internal fun RegistrarMovimientoContenido(
    estado: EstadoRegistro,
    onMonto: (String) -> Unit,
    onNota: (String) -> Unit,
    onFecha: (LocalDate) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
) {
    Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
        if (estado.cargando) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Surface
        }
        var eligiendoFecha by rememberSaveable { mutableStateOf(false) }
        val foco = remember { FocusRequester() }
        LaunchedEffect(Unit) { foco.requestFocus() }
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text =
                    stringResource(
                        if (estado.tipo == TipoMovimiento.FIADO) R.string.registro_titulo_fiar else R.string.registro_titulo_cobrar,
                        estado.nombreCliente,
                    ),
                style = MaterialTheme.typography.headlineSmall,
            )
            SaldoTexto(estado.saldo)
            OutlinedTextField(
                value = estado.monto,
                onValueChange = onMonto,
                modifier = Modifier.fillMaxWidth().focusRequester(foco),
                label = { Text(stringResource(R.string.registro_monto)) },
                prefix = { Text("S/ ") },
                isError = estado.montoInvalido,
                supportingText = if (estado.montoInvalido) ({ Text(stringResource(R.string.registro_error_monto)) }) else null,
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onGuardar() }),
            )
            val notaMuyLarga = estado.error == ErrorMovimiento.NotaMuyLarga
            val errorNota = pluralStringResource(R.plurals.registro_error_nota, Movimiento.LARGO_MAXIMO_NOTA, Movimiento.LARGO_MAXIMO_NOTA)
            OutlinedTextField(
                value = estado.nota,
                onValueChange = onNota,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.registro_nota)) },
                isError = notaMuyLarga,
                supportingText = if (notaMuyLarga) ({ Text(errorNota) }) else null,
                singleLine = true,
            )
            AssistChip(
                onClick = { eligiendoFecha = true },
                label = { Text(textoDeFecha(estado.fecha, estado.hoy)) },
                leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
            )
            if (estado.error == ErrorMovimiento.FechaFutura || estado.error == ErrorMovimiento.ClienteArchivado) {
                Text(
                    text =
                        stringResource(
                            if (estado.error ==
                                ErrorMovimiento.FechaFutura
                            ) {
                                R.string.registro_error_fecha
                            } else {
                                R.string.registro_error_archivado
                            },
                        ),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancelar) { Text(stringResource(R.string.registro_cancelar)) }
                Button(onClick = onGuardar, enabled = !estado.guardando) { Text(stringResource(R.string.registro_guardar)) }
            }
        }
        if (eligiendoFecha) {
            SelectorDeFecha(
                fecha = estado.fecha,
                hoy = estado.hoy,
                onElegir = {
                    onFecha(it)
                    eligiendoFecha = false
                },
                onCancelar = { eligiendoFecha = false },
            )
        }
    }
}

@Composable
private fun textoDeFecha(
    fecha: LocalDate,
    hoy: LocalDate,
): String =
    when (fecha) {
        hoy -> stringResource(R.string.registro_fecha_hoy)
        hoy.minusDays(1) -> stringResource(R.string.registro_fecha_ayer)
        else -> fecha.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-PE")))
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorDeFecha(
    fecha: LocalDate,
    hoy: LocalDate,
    onElegir: (LocalDate) -> Unit,
    onCancelar: () -> Unit,
) {
    val ultimoDia = hoy.toEpochDay() * MILISEGUNDOS_POR_DIA
    val estado =
        rememberDatePickerState(
            initialSelectedDateMillis = fecha.toEpochDay() * MILISEGUNDOS_POR_DIA,
            yearRange = (hoy.year - 1)..hoy.year,
            selectableDates =
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= ultimoDia
                },
        )
    DatePickerDialog(
        onDismissRequest = onCancelar,
        confirmButton = {
            TextButton(onClick = { estado.selectedDateMillis?.let { onElegir(LocalDate.ofEpochDay(it / MILISEGUNDOS_POR_DIA)) } }) {
                Text(stringResource(R.string.registro_aceptar))
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.registro_cancelar)) } },
    ) {
        DatePicker(state = estado)
    }
}
