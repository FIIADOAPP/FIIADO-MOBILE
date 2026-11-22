package com.fiiadoapp.fiado.feature.movimientos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.fiiadoapp.fiado.domain.AtajoDePlazo
import com.fiiadoapp.fiado.domain.ErrorMovimiento
import com.fiiadoapp.fiado.domain.MetodoCobro
import com.fiiadoapp.fiado.domain.Movimiento
import com.fiiadoapp.fiado.domain.TipoMovimiento
import java.time.LocalDate
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
        onMetodo = viewModel::elegirMetodo,
        onAtajo = viewModel::elegirAtajo,
        onOtraFecha = viewModel::elegirOtraFecha,
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
    onMetodo: (Uuid) -> Unit,
    onAtajo: (AtajoDePlazo?) -> Unit,
    onOtraFecha: (LocalDate) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
) {
    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
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
                shape = MaterialTheme.shapes.small,
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
            if (estado.tipo == TipoMovimiento.PAGO) {
                SelectorDeMetodo(estado.metodos, estado.metodoElegido, onMetodo)
            } else {
                SelectorDePlazo(estado, onAtajo, onOtraFecha)
            }
            val notaMuyLarga = estado.error == ErrorMovimiento.NotaMuyLarga
            val errorNota = pluralStringResource(R.plurals.registro_error_nota, Movimiento.LARGO_MAXIMO_NOTA, Movimiento.LARGO_MAXIMO_NOTA)
            OutlinedTextField(
                shape = MaterialTheme.shapes.small,
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
                leadingIcon = { Icon(Icons.Rounded.DateRange, contentDescription = null) },
                shape = CircleShape,
            )
            val errorGeneral =
                when (estado.error) {
                    ErrorMovimiento.FechaFutura -> R.string.registro_error_fecha
                    ErrorMovimiento.ClienteArchivado -> R.string.registro_error_archivado
                    ErrorMovimiento.SinMetodoCobro, ErrorMovimiento.MetodoCobroArchivado -> R.string.registro_error_metodo
                    ErrorMovimiento.PlazoAntesDeLaFecha -> R.string.registro_error_plazo
                    else -> null
                }
            if (errorGeneral != null) {
                Text(
                    text = stringResource(errorGeneral),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancelar) { Text(stringResource(R.string.registro_cancelar)) }
                Button(onClick = onGuardar, enabled = !estado.guardando, modifier = Modifier.padding(start = 8.dp)) {
                    Text(stringResource(R.string.registro_guardar))
                }
            }
        }
        if (eligiendoFecha) {
            SelectorDeFecha(
                fecha = estado.fecha,
                anios = (estado.hoy.year - 1)..estado.hoy.year,
                seleccionable = { !it.isAfter(estado.hoy) },
                onElegir = {
                    onFecha(it)
                    eligiendoFecha = false
                },
                onCancelar = { eligiendoFecha = false },
            )
        }
    }
}

/** Un toque para cambiarlo; el preseleccionado ya viene marcado. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectorDeMetodo(
    metodos: List<MetodoCobro>,
    elegido: Uuid?,
    onMetodo: (Uuid) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.registro_metodo), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            metodos.forEach { metodo ->
                val seleccionado = metodo.id == elegido
                FilterChip(
                    selected = seleccionado,
                    onClick = { onMetodo(metodo.id) },
                    label = { Text(metodo.nombre) },
                    // El check distingue el elegido sin depender solo del color.
                    leadingIcon = if (seleccionado) ({ Icon(Icons.Rounded.Check, contentDescription = null) }) else null,
                    shape = CircleShape,
                )
            }
        }
    }
}

/** Sin plazo por defecto; los atajos se cuentan desde el día del fiado (ADR-0007). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectorDePlazo(
    estado: EstadoRegistro,
    onAtajo: (AtajoDePlazo?) -> Unit,
    onOtraFecha: (LocalDate) -> Unit,
) {
    var eligiendo by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.registro_plazo), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OpcionDePlazo(stringResource(R.string.registro_plazo_ninguno), estado.vence == null) { onAtajo(null) }
            AtajoDePlazo.entries.forEach { atajo ->
                OpcionDePlazo(textoDeAtajo(atajo), estado.otraFecha == null && estado.atajo == atajo) { onAtajo(atajo) }
            }
            OpcionDePlazo(stringResource(R.string.registro_plazo_otra_fecha), estado.otraFecha != null) { eligiendo = true }
        }
        estado.vence?.let {
            Text(
                textoDePlazo(it, estado.hoy),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (eligiendo) {
        SelectorDeFecha(
            fecha = estado.vence ?: AtajoDePlazo.UNA_SEMANA.desde(estado.fecha),
            anios = estado.fecha.year..(estado.fecha.year + 1),
            seleccionable = { !it.isBefore(estado.fecha) },
            onElegir = {
                onOtraFecha(it)
                eligiendo = false
            },
            onCancelar = { eligiendo = false },
        )
    }
}

@Composable
private fun OpcionDePlazo(
    texto: String,
    elegida: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = elegida,
        onClick = onClick,
        label = { Text(texto) },
        leadingIcon = if (elegida) ({ Icon(Icons.Rounded.Check, contentDescription = null) }) else null,
        shape = CircleShape,
    )
}

@Composable
private fun textoDeAtajo(atajo: AtajoDePlazo): String =
    stringResource(
        when (atajo) {
            AtajoDePlazo.MANANA -> R.string.registro_plazo_manana
            AtajoDePlazo.TRES_DIAS -> R.string.registro_plazo_tres_dias
            AtajoDePlazo.UNA_SEMANA -> R.string.registro_plazo_una_semana
            AtajoDePlazo.FIN_DE_MES -> R.string.registro_plazo_fin_de_mes
        },
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectorDeFecha(
    fecha: LocalDate,
    anios: IntRange,
    seleccionable: (LocalDate) -> Boolean,
    onElegir: (LocalDate) -> Unit,
    onCancelar: () -> Unit,
) {
    val estado =
        rememberDatePickerState(
            initialSelectedDateMillis = fecha.toEpochDay() * MILISEGUNDOS_POR_DIA,
            yearRange = anios,
            selectableDates =
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) =
                        seleccionable(
                            LocalDate.ofEpochDay(utcTimeMillis / MILISEGUNDOS_POR_DIA),
                        )
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
