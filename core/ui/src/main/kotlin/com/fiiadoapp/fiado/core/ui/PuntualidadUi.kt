package com.fiiadoapp.fiado.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fiiadoapp.fiado.domain.NivelPuntualidad
import com.fiiadoapp.fiado.domain.Puntualidad
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Verde, ámbar o coral según el semáforo (ADR-0007); gris sin historial. */
@Composable
fun colorDe(nivel: NivelPuntualidad): Color =
    when (nivel) {
        NivelPuntualidad.PUNTUAL -> MaterialTheme.colorScheme.tertiary
        NivelPuntualidad.SE_DEMORA -> ColoresFiado.Atencion
        NivelPuntualidad.CUIDADO -> MaterialTheme.colorScheme.error
        NivelPuntualidad.SIN_HISTORIAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }

@Composable
fun textoDe(nivel: NivelPuntualidad): String =
    stringResource(
        when (nivel) {
            NivelPuntualidad.PUNTUAL -> R.string.puntualidad_puntual
            NivelPuntualidad.SE_DEMORA -> R.string.puntualidad_se_demora
            NivelPuntualidad.CUIDADO -> R.string.puntualidad_cuidado
            NivelPuntualidad.SIN_HISTORIAL -> R.string.puntualidad_sin_historial
        },
    )

/** Pastilla con un punto de color y el nombre del nivel. */
@Composable
fun IndicadorDePuntualidad(
    nivel: NivelPuntualidad,
    modifier: Modifier = Modifier,
) {
    val color = colorDe(nivel)
    Row(
        modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.size(8.dp).clip(CircleShape).background(color)) {}
        Text(textoDe(nivel), style = MaterialTheme.typography.labelMedium, color = color)
    }
}

/** Lo más urgente de un cliente: "Vencido hace 3 días", "Vence hoy", "Vence mañana" o "Vence en 4 días". */
@Composable
fun etiquetaDeVencimiento(
    puntualidad: Puntualidad,
    hoy: LocalDate,
): Pair<String, Color>? {
    if (puntualidad.diasVencido > 0) {
        val dias = puntualidad.diasVencido.toInt()
        val color = if (puntualidad.diasVencido >= Puntualidad.DIAS_DEMORA) MaterialTheme.colorScheme.error else ColoresFiado.Atencion
        return pluralStringResource(R.plurals.vencido_hace, dias, dias) to color
    }
    val proximo = puntualidad.proximoVencimiento ?: return null
    val dias = ChronoUnit.DAYS.between(hoy, proximo).toInt()
    val texto =
        when {
            dias == 0 -> stringResource(R.string.vence_hoy)
            dias == 1 -> stringResource(R.string.vence_manana)
            dias <= DIAS_AVISO_VENCIMIENTO -> pluralStringResource(R.plurals.vence_en, dias, dias)
            else -> return null
        }
    return texto to ColoresFiado.Atencion
}

/** Más allá de una semana, el vencimiento no se destaca en la lista. */
private const val DIAS_AVISO_VENCIMIENTO = 7
