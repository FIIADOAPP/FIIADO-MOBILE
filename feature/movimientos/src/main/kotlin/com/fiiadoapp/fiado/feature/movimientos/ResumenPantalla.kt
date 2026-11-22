package com.fiiadoapp.fiado.feature.movimientos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fiiadoapp.fiado.core.ui.AnilloDeProgreso
import com.fiiadoapp.fiado.core.ui.Avatar
import com.fiiadoapp.fiado.core.ui.BarraDeProgreso
import com.fiiadoapp.fiado.core.ui.EncabezadoGrande
import com.fiiadoapp.fiado.core.ui.LocalEspacioBarraInferior
import com.fiiadoapp.fiado.core.ui.MargenPantalla
import com.fiiadoapp.fiado.core.ui.Tarjeta
import com.fiiadoapp.fiado.core.ui.TextoQueSeAjusta
import com.fiiadoapp.fiado.core.ui.colorDe
import com.fiiadoapp.fiado.core.ui.textoDe
import com.fiiadoapp.fiado.domain.Dinero
import com.fiiadoapp.fiado.domain.NivelPuntualidad
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.uuid.Uuid

private val LOCALE_PERU = Locale.forLanguageTag("es-PE")
private val FORMATO_MES = DateTimeFormatter.ofPattern("MMMM", LOCALE_PERU)
private val FORMATO_DIA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", LOCALE_PERU)

@Composable
internal fun ResumenPantalla(
    onCliente: (Uuid) -> Unit,
    viewModel: ResumenViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    ResumenContenido(estado, onCliente)
}

@Composable
internal fun ResumenContenido(
    estado: EstadoResumen?,
    onCliente: (Uuid) -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        EncabezadoGrande(
            stringResource(R.string.resumen_titulo),
            subtitulo = estado?.hoy?.format(FORMATO_DIA)?.replaceFirstChar { it.titlecase(LOCALE_PERU) },
        )
        if (estado == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MargenPantalla),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TarjetaPorCobrar(estado.porCobrar)
            val mes = estado.hoy.format(FORMATO_MES).replaceFirstChar { it.titlecase(LOCALE_PERU) }
            TarjetaDelMes(stringResource(R.string.resumen_mes, mes), estado.delMes)
            Text(stringResource(R.string.resumen_hoy), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Cifra(stringResource(R.string.resumen_fiado), estado.delDia.fiado, MaterialTheme.colorScheme.error, Modifier.weight(1f))
                Cifra(
                    stringResource(R.string.resumen_cobrado),
                    estado.delDia.cobrado,
                    MaterialTheme.colorScheme.tertiary,
                    Modifier.weight(1f),
                )
            }
            if (estado.delDia.porMetodo.isNotEmpty()) {
                TarjetaPorMetodo(stringResource(R.string.resumen_por_metodo_hoy), estado.delDia)
            }
            estado.comoTePagan?.let { TarjetaComoTePagan(it, onCliente) }
            Spacer(Modifier.height(LocalEspacioBarraInferior.current))
        }
    }
}

@Composable
private fun TarjetaPorCobrar(porCobrar: Dinero) {
    Tarjeta(Modifier.fillMaxWidth(), relleno = 20.dp) {
        Text(stringResource(R.string.resumen_por_cobrar), style = MaterialTheme.typography.titleMedium)
        TextoQueSeAjusta(
            porCobrar.formatear(),
            estilo = MaterialTheme.typography.displaySmall,
            color = if (porCobrar.esPositivo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
        )
        Text(
            stringResource(R.string.resumen_por_cobrar_ayuda),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Lo fiado y lo cobrado del mes, con un anillo que muestra cuánto de lo fiado ya se cobró. */
@Composable
private fun TarjetaDelMes(
    titulo: String,
    periodo: Periodo,
) {
    Tarjeta(Modifier.fillMaxWidth(), relleno = 20.dp) {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Monto(stringResource(R.string.resumen_fiado), periodo.fiado, MaterialTheme.colorScheme.error)
                Monto(stringResource(R.string.resumen_cobrado), periodo.cobrado, MaterialTheme.colorScheme.tertiary)
            }
            val fraccion = fraccionCobrada(periodo)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnilloDeProgreso(fraccion ?: 0f, fraccion?.let { "${(it * 100).roundToInt()}%" } ?: "—")
                Text(
                    stringResource(R.string.resumen_cobrado_de_lo_fiado),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        if (periodo.porMetodo.isNotEmpty()) {
            Text(
                stringResource(R.string.resumen_por_metodo),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            BarrasPorMetodo(periodo)
        }
    }
}

@Composable
private fun TarjetaPorMetodo(
    titulo: String,
    periodo: Periodo,
) {
    Tarjeta(Modifier.fillMaxWidth(), relleno = 20.dp) {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        BarrasPorMetodo(periodo)
    }
}

/** Cada método con su monto y una barra proporcional a lo cobrado en el periodo. */
@Composable
private fun BarrasPorMetodo(periodo: Periodo) {
    val total = periodo.porMetodo.sumOf { it.monto.centimos }.coerceAtLeast(1)
    periodo.porMetodo.forEach { metodo ->
        Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row {
                Text(metodo.nombre, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(metodo.monto.formatear(), style = MaterialTheme.typography.bodyMedium)
            }
            BarraDeProgreso(metodo.monto.centimos.toFloat() / total)
        }
    }
}

@Composable
private fun Cifra(
    etiqueta: String,
    monto: Dinero,
    color: Color,
    modifier: Modifier,
) {
    Tarjeta(modifier, relleno = 16.dp) {
        Monto(etiqueta, monto, color)
    }
}

@Composable
private fun Monto(
    etiqueta: String,
    monto: Dinero,
    color: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextoQueSeAjusta(monto.formatear(), estilo = MaterialTheme.typography.titleLarge, color = color)
    }
}

/** `null` si en el periodo no se fió nada: no hay de qué calcular un porcentaje. */
private fun fraccionCobrada(periodo: Periodo): Float? =
    if (periodo.fiado.centimos <= 0) null else (periodo.cobrado.centimos.toFloat() / periodo.fiado.centimos).coerceAtMost(1f)

/** Clientes por nivel, lo que tardan en pagar y quiénes están en Cuidado (ADR-0007). */
@Composable
private fun TarjetaComoTePagan(
    comoTePagan: ComoTePagan,
    onCliente: (Uuid) -> Unit,
) {
    val negocio = comoTePagan.negocio
    Text(
        stringResource(R.string.resumen_como_te_pagan),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 8.dp),
    )
    Tarjeta(Modifier.fillMaxWidth(), relleno = 20.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(NivelPuntualidad.PUNTUAL, NivelPuntualidad.SE_DEMORA, NivelPuntualidad.CUIDADO).forEach { nivel ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(negocio.clientesEn(nivel).toString(), style = MaterialTheme.typography.headlineMedium, color = colorDe(nivel))
                    Text(textoDe(nivel), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        val sinHistorial = negocio.clientesEn(NivelPuntualidad.SIN_HISTORIAL)
        if (sinHistorial > 0) {
            Text(
                pluralStringResource(R.plurals.resumen_sin_historial, sinHistorial, sinHistorial),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        negocio.diasPromedioParaPagar?.let {
            val dias = it.roundToInt()
            Text(pluralStringResource(R.plurals.resumen_tardan_en_pagar, dias, dias), style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (comoTePagan.enCuidado.isNotEmpty()) {
        Tarjeta(Modifier.fillMaxWidth(), relleno = 8.dp) {
            Text(
                stringResource(R.string.resumen_para_tener_cuidado),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 12.dp, top = 8.dp),
            )
            comoTePagan.enCuidado.forEach { cliente -> FilaEnCuidado(cliente, onCliente) }
        }
    }
}

@Composable
private fun FilaEnCuidado(
    cliente: ClienteEnCuidado,
    onCliente: (Uuid) -> Unit,
) {
    val puntualidad = cliente.puntualidad
    val motivo =
        if (puntualidad.vencido.esPositivo) {
            val dias = puntualidad.diasVencido.toInt()
            pluralStringResource(R.plurals.resumen_debe_vencido, dias, puntualidad.vencido.formatear(), dias)
        } else {
            val dias = (puntualidad.atrasoPromedio ?: 0.0).roundToInt()
            pluralStringResource(R.plurals.resumen_suele_pagar_tarde, dias, dias)
        }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onCliente(cliente.id) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Avatar(cliente.nombre)
        Column(Modifier.weight(1f)) {
            Text(cliente.nombre, style = MaterialTheme.typography.bodyLarge)
            Text(motivo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
