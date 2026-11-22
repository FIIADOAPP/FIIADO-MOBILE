package com.fiiadoapp.fiado.feature.movimientos

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fiiadoapp.fiado.core.ui.Avatar
import com.fiiadoapp.fiado.core.ui.BarraSecundaria
import com.fiiadoapp.fiado.core.ui.ColoresFiado
import com.fiiadoapp.fiado.core.ui.IndicadorDePuntualidad
import com.fiiadoapp.fiado.core.ui.MargenPantalla
import com.fiiadoapp.fiado.core.ui.SaldoTexto
import com.fiiadoapp.fiado.core.ui.Tarjeta
import com.fiiadoapp.fiado.core.ui.etiquetaDeVencimiento
import com.fiiadoapp.fiado.domain.EstadoFiado
import com.fiiadoapp.fiado.domain.Movimiento
import com.fiiadoapp.fiado.domain.NivelPuntualidad
import com.fiiadoapp.fiado.domain.Puntualidad
import com.fiiadoapp.fiado.domain.TipoMovimiento
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.uuid.Uuid

@Composable
internal fun HistorialPantalla(
    onVolver: () -> Unit,
    onEditar: (Uuid) -> Unit,
    onRegistrar: (Uuid, TipoMovimiento) -> Unit,
    viewModel: HistorialViewModel = hiltViewModel(),
) {
    val contexto = LocalContext.current
    val sinWhatsApp = stringResource(R.string.historial_sin_whatsapp)
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val porAnular by viewModel.porAnular.collectAsStateWithLifecycle()
    val opciones by viewModel.opciones.collectAsStateWithLifecycle()
    val porProrrogar by viewModel.porProrrogar.collectAsStateWithLifecycle()
    HistorialContenido(
        estado = estado,
        hoy = viewModel.hoy,
        porAnular = porAnular,
        onVolver = onVolver,
        onEditar = onEditar,
        onRegistrar = onRegistrar,
        onRecordar = { abrirWhatsApp(contexto, it, sinWhatsApp) },
        onTocar = viewModel::tocar,
        onConfirmarAnulacion = viewModel::confirmarAnulacion,
        onCancelarAnulacion = viewModel::cancelarAnulacion,
    )
    opciones?.let { OpcionesDeFiado(it, viewModel.hoy, viewModel::elegirProrrogar, viewModel::elegirAnular, viewModel::cancelarOpciones) }
    porProrrogar?.let { fila ->
        val vigente = checkNotNull(fila.plazo)
        SelectorDeFecha(
            fecha = vigente.plusDays(1),
            anios = vigente.year..(vigente.year + 1),
            seleccionable = { it.isAfter(vigente) },
            onElegir = viewModel::darMasPlazo,
            onCancelar = viewModel::cancelarOpciones,
        )
    }
}

@Composable
internal fun HistorialContenido(
    estado: EstadoHistorial?,
    hoy: LocalDate,
    porAnular: Movimiento?,
    onVolver: () -> Unit,
    onEditar: (Uuid) -> Unit,
    onRegistrar: (Uuid, TipoMovimiento) -> Unit,
    onRecordar: (Recordatorio.Listo) -> Unit,
    onTocar: (FilaHistorial) -> Unit,
    onConfirmarAnulacion: () -> Unit,
    onCancelarAnulacion: () -> Unit,
) {
    Scaffold(
        topBar = {
            BarraSecundaria(estado?.cliente?.nombre.orEmpty(), onVolver) {
                if (estado != null) {
                    IconButton(onClick = { onEditar(estado.cliente.id) }) {
                        Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.historial_editar_cliente))
                    }
                }
            }
        },
    ) { relleno ->
        if (estado == null) {
            Box(Modifier.padding(relleno).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(relleno).fillMaxSize(),
            contentPadding = PaddingValues(start = MargenPantalla, end = MargenPantalla, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "cabecera") { Cabecera(estado, onRegistrar, onRecordar, onEditar) }
            estado.puntualidad?.let { item(key = "como-paga") { ComoPaga(it, hoy) } }
            item(key = "titulo") {
                Column(Modifier.padding(top = 14.dp, bottom = 2.dp)) {
                    Text(stringResource(R.string.historial_movimientos), style = MaterialTheme.typography.titleLarge)
                    if (estado.filas.any { it.movimiento.estaVigente }) {
                        Text(
                            stringResource(R.string.historial_ayuda),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (estado.filas.isEmpty()) {
                item(key = "vacio") {
                    Text(
                        text = stringResource(R.string.historial_vacio),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(estado.filas, key = { it.movimiento.id.toString() }) { fila -> FilaMovimiento(fila, hoy, onTocar) }
        }
    }
    if (porAnular != null) {
        ConfirmarAnulacion(porAnular, onConfirmarAnulacion, onCancelarAnulacion)
    }
}

/** Para un fiado con plazo: dar más plazo o anularlo (ADR-0007). */
@Composable
private fun OpcionesDeFiado(
    fila: FilaHistorial,
    hoy: LocalDate,
    onProrrogar: () -> Unit,
    onAnular: () -> Unit,
    onCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.historial_opciones_titulo, fila.movimiento.monto.formatear())) },
        text = { Text(fila.plazo?.let { textoDePlazo(it, hoy) }.orEmpty()) },
        confirmButton = { TextButton(onClick = onProrrogar) { Text(stringResource(R.string.historial_dar_mas_plazo)) } },
        dismissButton = {
            Row {
                TextButton(onClick = onCancelar) { Text(stringResource(R.string.registro_cancelar)) }
                TextButton(onClick = onAnular) { Text(stringResource(R.string.historial_anular)) }
            }
        },
    )
}

@Composable
private fun Cabecera(
    estado: EstadoHistorial,
    onRegistrar: (Uuid, TipoMovimiento) -> Unit,
    onRecordar: (Recordatorio.Listo) -> Unit,
    onEditar: (Uuid) -> Unit,
) {
    Tarjeta(Modifier.fillMaxWidth(), relleno = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Avatar(estado.cliente.nombre, tamano = 52.dp)
            Column(Modifier.weight(1f)) {
                SaldoTexto(estado.saldo, estilo = MaterialTheme.typography.headlineSmall)
                estado.cliente.telefono?.let {
                    Text(it.formatear(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { onRegistrar(estado.cliente.id, TipoMovimiento.FIADO) }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.historial_fiar))
            }
            FilledTonalButton(
                onClick = { onRegistrar(estado.cliente.id, TipoMovimiento.PAGO) },
                modifier = Modifier.weight(1f),
                colors =
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
            ) {
                Text(stringResource(R.string.historial_cobrar))
            }
        }
        when (val recordatorio = estado.recordatorio) {
            is Recordatorio.Listo -> {
                Button(onClick = { onRecordar(recordatorio) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.historial_recordar), Modifier.padding(start = 8.dp))
                }
            }

            Recordatorio.SinCelular -> {
                TextButton(onClick = { onEditar(estado.cliente.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.historial_recordar_sin_celular))
                }
            }

            null -> {}
        }
    }
}

/** Un fiado suma a la deuda y un pago la resta; un anulado queda tachado y no se puede tocar. */
@Composable
private fun FilaMovimiento(
    fila: FilaHistorial,
    hoy: LocalDate,
    onTocar: (FilaHistorial) -> Unit,
) {
    val movimiento = fila.movimiento
    val vigente = movimiento.estaVigente
    val esFiado = movimiento.tipo == TipoMovimiento.FIADO
    val tachado = if (vigente) TextDecoration.None else TextDecoration.LineThrough
    val titulo =
        when (movimiento.tipo) {
            TipoMovimiento.FIADO -> stringResource(R.string.historial_fiado)
            TipoMovimiento.PAGO -> stringResource(R.string.historial_pago, fila.nombreMetodo.orEmpty())
        }
    val plazo =
        fila.plazo?.let {
            textoDePlazo(it, hoy) + if (fila.prorrogado) " " + stringResource(R.string.historial_con_mas_plazo) else ""
        }
    val situacion = fila.estado?.let { situacionDeFiado(it, hoy) }
    val detalle = listOfNotNull(textoDeFecha(movimiento.fecha, hoy), plazo, movimiento.nota).joinToString(" · ")
    val color =
        when {
            !vigente -> MaterialTheme.colorScheme.onSurfaceVariant
            esFiado -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.tertiary
        }
    Tarjeta(
        Modifier.fillMaxWidth(),
        onClick = if (vigente) ({ onTocar(fila) }) else null,
        relleno = 14.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            !vigente -> MaterialTheme.colorScheme.surfaceContainerHigh
                            esFiado -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.tertiaryContainer
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (esFiado) Icons.Rounded.Add else Icons.Rounded.Check,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium, textDecoration = tachado)
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                situacion?.let { (texto, color) -> Text(texto, style = MaterialTheme.typography.bodySmall, color = color) }
                if (!vigente) {
                    Text(
                        stringResource(R.string.historial_anulado),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Text(
                text = (if (esFiado) "+ " else "− ") + movimiento.monto.formatear(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textDecoration = tachado,
                color = color,
            )
        }
    }
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

/** Abre WhatsApp (o WhatsApp Business) con el mensaje escrito; el bodeguero lo revisa y lo envía. */
private fun abrirWhatsApp(
    contexto: Context,
    recordatorio: Recordatorio.Listo,
    sinWhatsApp: String,
) {
    val enlace =
        Uri
            .Builder()
            .scheme("https")
            .authority("wa.me")
            .appendPath(recordatorio.celular)
            .appendQueryParameter("text", recordatorio.mensaje)
            .build()
    try {
        contexto.startActivity(Intent(Intent.ACTION_VIEW, enlace))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(contexto, sinWhatsApp, Toast.LENGTH_LONG).show()
    }
}

/** "Vencido hace 3 días" si sigue pendiente; "Pagado a tiempo" o "Pagado 2 días tarde" si tenía plazo. */
@Composable
private fun situacionDeFiado(
    estado: EstadoFiado,
    hoy: LocalDate,
): Pair<String, Color>? {
    if (estado.plazo == null) return null
    val vencido = estado.vencidoHace(hoy).toInt()
    return when {
        !estado.pagado && vencido > 0 -> {
            pluralStringResource(R.plurals.historial_vencido_hace, vencido, vencido) to MaterialTheme.colorScheme.error
        }

        !estado.pagado -> {
            null
        }

        estado.atraso <= Puntualidad.DIAS_TOLERADOS -> {
            stringResource(R.string.historial_pagado_a_tiempo) to MaterialTheme.colorScheme.tertiary
        }

        else -> {
            val dias = estado.atraso.toInt()
            pluralStringResource(R.plurals.historial_pagado_tarde, dias, dias) to ColoresFiado.Atencion
        }
    }
}

/** Cómo paga el cliente: su nivel y los números que lo explican (ADR-0007). */
@Composable
private fun ComoPaga(
    puntualidad: Puntualidad,
    hoy: LocalDate,
) {
    Tarjeta(Modifier.fillMaxWidth(), relleno = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.historial_como_paga), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IndicadorDePuntualidad(puntualidad.nivel)
        }
        val lineas =
            buildList {
                puntualidad.diasPromedioParaPagar?.let {
                    val dias = it.roundToInt()
                    add(pluralStringResource(R.plurals.historial_tarda_en_pagar, dias, dias))
                }
                puntualidad.porcentajeATiempo?.let { add(stringResource(R.string.historial_a_tiempo, it)) }
                if (puntualidad.vencido.esPositivo) {
                    val dias = puntualidad.diasVencido.toInt()
                    add(pluralStringResource(R.plurals.historial_vencido_total, dias, puntualidad.vencido.formatear(), dias))
                }
            }
        if (puntualidad.nivel == NivelPuntualidad.SIN_HISTORIAL) {
            Text(
                stringResource(R.string.historial_sin_historial),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        lineas.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        etiquetaDeVencimiento(puntualidad, hoy)
            ?.takeIf { puntualidad.diasVencido == 0L }
            ?.let { (texto, color) -> Text(texto, style = MaterialTheme.typography.bodyMedium, color = color) }
    }
}
