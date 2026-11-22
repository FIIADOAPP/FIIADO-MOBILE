package com.fiiadoapp.fiado.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Un destino de la barra inferior. */
data class DestinoBarra(
    val icono: ImageVector,
    val etiqueta: String,
)

/** Alto que ocupa la barra con su margen; las pantallas dejan este espacio libre al final. */
val AltoBarraInferior = 104.dp

/**
 * Barra inferior flotante: una pastilla oscura con los destinos y, al centro, el botón principal
 * redondo en lila para agregar un cliente. Los destinos se reparten a ambos lados del botón.
 */
@Composable
fun BarraInferiorFlotante(
    destinos: List<DestinoBarra>,
    seleccionado: Int?,
    onDestino: (Int) -> Unit,
    descripcionCentral: String,
    onCentral: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mitad = destinos.size / 2
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = MargenPantalla, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().height(72.dp),
            shape = RoundedCornerShape(36.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 6.dp,
        ) {
            BoxWithConstraints(Modifier.padding(horizontal = RellenoBarra)) {
                // Los destinos y el botón central ocupan el mismo ancho: quedan igual de espaciados y el botón,
                // en el centro exacto de la barra.
                val anchoEspacio = maxWidth / (destinos.size + 1)
                val conEtiquetas = etiquetasEntran(destinos, anchoEspacio)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    destinos.forEachIndexed { indice, destino ->
                        if (indice == mitad) {
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { BotonCentral(descripcionCentral, onCentral) }
                        }
                        ElementoBarra(destino, indice == seleccionado, conEtiquetas, Modifier.weight(1f)) { onDestino(indice) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BotonCentral(
    descripcion: String,
    onClick: () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(52.dp),
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
    ) {
        Icon(IconosFiado.NuevoCliente, contentDescription = descripcion, modifier = Modifier.size(26.dp))
    }
}

private val RellenoBarra = 8.dp
private val AltoElemento = 64.dp
private val EstiloEtiqueta = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)

/**
 * `true` si todas las etiquetas caben en su espacio con la letra actual del teléfono. Si alguna no cabe
 * (pantalla angosta o letra muy grande), la barra muestra solo íconos: todas a la vez, para que no quede dispareja.
 */
@Composable
private fun etiquetasEntran(
    destinos: List<DestinoBarra>,
    anchoEspacio: Dp,
): Boolean {
    val medidor = rememberTextMeasurer()
    val densidad = LocalDensity.current
    val estilo = MaterialTheme.typography.labelSmall.merge(EstiloEtiqueta)
    return with(densidad) {
        val anchoDisponible = (anchoEspacio - 4.dp).toPx()
        val altoDisponible = (AltoElemento - 24.dp - 8.dp).toPx()
        destinos.all { destino ->
            val medida = medidor.measure(destino.etiqueta, estilo, maxLines = 1, softWrap = false).size
            medida.width <= anchoDisponible && medida.height <= altoDisponible
        }
    }
}

@Composable
private fun ElementoBarra(
    destino: DestinoBarra,
    activo: Boolean,
    conEtiqueta: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val color = if (activo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier
            .height(AltoElemento)
            .selectable(selected = activo, onClick = onClick, role = Role.Tab),
        contentAlignment = Alignment.Center,
    ) {
        // El ícono y el texto quedan centrados en la barra, a la altura del botón central.
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Sin etiqueta, el ícono crece un poco y lleva el nombre para el lector de pantalla.
            Icon(
                destino.icono,
                contentDescription = if (conEtiqueta) null else destino.etiqueta,
                tint = color,
                modifier = Modifier.size(if (conEtiqueta) 24.dp else 28.dp),
            )
            if (conEtiqueta) {
                Text(
                    destino.etiqueta,
                    color = color,
                    maxLines = 1,
                    softWrap = false,
                    style = MaterialTheme.typography.labelSmall.merge(EstiloEtiqueta),
                )
            }
        }
        // El punto del destino activo va sobre el borde inferior, sin mover el ícono ni el texto.
        if (activo) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .size(4.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
        }
    }
}
