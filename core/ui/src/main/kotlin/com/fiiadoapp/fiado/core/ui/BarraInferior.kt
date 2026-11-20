package com.fiiadoapp.fiado.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
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
 * redondo en lila. Los destinos se reparten a ambos lados del botón.
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
            Row(
                Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                destinos.forEachIndexed { indice, destino ->
                    if (indice == mitad) BotonCentral(descripcionCentral, onCentral)
                    ElementoBarra(destino, indice == seleccionado, Modifier.weight(1f)) { onDestino(indice) }
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
        Icon(Icons.Rounded.Add, contentDescription = descripcion, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun ElementoBarra(
    destino: DestinoBarra,
    activo: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val color = if (activo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier
            .height(64.dp)
            .selectable(selected = activo, onClick = onClick, role = Role.Tab),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(destino.icono, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Text(
            destino.etiqueta,
            color = color,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
        )
        Box(
            Modifier
                .padding(top = 3.dp)
                .size(4.dp)
                .background(if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
        )
    }
}
