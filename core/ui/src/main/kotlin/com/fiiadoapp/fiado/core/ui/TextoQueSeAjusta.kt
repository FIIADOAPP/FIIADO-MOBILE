package com.fiiadoapp.fiado.core.ui

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Una sola línea que achica la letra hasta [minimo] cuando no entra, en vez de cortarse o partirse.
 * Sirve para montos y etiquetas en teléfonos angostos o con la letra agrandada.
 */
@Composable
fun TextoQueSeAjusta(
    texto: String,
    modifier: Modifier = Modifier,
    estilo: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    grosor: FontWeight? = null,
    minimo: TextUnit = 10.sp,
) {
    Text(
        text = texto,
        modifier = modifier,
        style = estilo,
        color = color,
        fontWeight = grosor,
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(minFontSize = minimo, maxFontSize = estilo.fontSize, stepSize = 0.5.sp),
    )
}
