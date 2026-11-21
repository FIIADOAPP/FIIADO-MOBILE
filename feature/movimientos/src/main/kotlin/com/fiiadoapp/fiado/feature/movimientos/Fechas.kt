package com.fiiadoapp.fiado.feature.movimientos

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FORMATO_FECHA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-PE"))

/** "Hoy", "Ayer" o "sábado 14 de noviembre". */
@Composable
internal fun textoDeFecha(
    fecha: LocalDate,
    hoy: LocalDate,
): String =
    when (fecha) {
        hoy -> stringResource(R.string.registro_fecha_hoy)
        hoy.minusDays(1) -> stringResource(R.string.registro_fecha_ayer)
        else -> fecha.format(FORMATO_FECHA)
    }

/** "Para hoy", "Para mañana" o "Para el sábado 22 de noviembre". */
@Composable
internal fun textoDePlazo(
    vence: LocalDate,
    hoy: LocalDate,
): String =
    when (vence) {
        hoy -> stringResource(R.string.plazo_hoy)
        hoy.plusDays(1) -> stringResource(R.string.plazo_manana)
        else -> stringResource(R.string.plazo_fecha, vence.format(FORMATO_FECHA))
    }
