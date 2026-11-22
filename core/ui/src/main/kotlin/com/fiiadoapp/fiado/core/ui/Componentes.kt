package com.fiiadoapp.fiado.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Espacio que la barra inferior flotante ocupa sobre el contenido; las pantallas lo dejan libre al final. */
val LocalEspacioBarraInferior = compositionLocalOf { 0.dp }

/** Margen lateral común de todas las pantallas. */
val MargenPantalla: Dp = 20.dp

/** Título grande de las pantallas principales, con un subtítulo opcional y una acción a la derecha. */
@Composable
fun EncabezadoGrande(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    accion: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().padding(start = MargenPantalla, end = MargenPantalla, top = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(titulo, style = MaterialTheme.typography.headlineLarge)
            subtitulo?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        accion?.invoke()
    }
}

/** Barra de las pantallas secundarias: volver en un botón redondo y el título, sin fondo propio. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraSecundaria(
    titulo: String,
    onVolver: () -> Unit,
    acciones: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(titulo, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            IconButton(
                onClick = onVolver,
                modifier = Modifier.padding(start = 8.dp),
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.volver))
            }
        },
        actions = acciones,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

/** Tarjeta de la app: color de tarjeta, esquinas de 20 dp y relleno interior. */
@Composable
fun Tarjeta(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    relleno: Dp = 18.dp,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val interior: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.fillMaxWidth().padding(relleno), verticalArrangement = Arrangement.spacedBy(8.dp), content = contenido)
    }
    // Texto blanco explícito: el fondo de tarjeta coincide con surfaceVariant y Material le daría el gris secundario.
    val colores =
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
    if (onClick == null) {
        Card(modifier, colors = colores, content = interior)
    } else {
        Card(onClick = onClick, modifier = modifier, colors = colores, content = interior)
    }
}

/** Anillo de progreso con un texto al centro, como un indicador circular de avance. */
@Composable
fun AnilloDeProgreso(
    fraccion: Float,
    centro: String,
    modifier: Modifier = Modifier,
    diametro: Dp = 112.dp,
    grosor: Dp = 10.dp,
    color: Color = MaterialTheme.colorScheme.tertiary,
) {
    val pista = MaterialTheme.colorScheme.outlineVariant
    val avance = fraccion.coerceIn(0f, 1f)
    Box(modifier.size(diametro), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diametro)) {
            val trazo = grosor.toPx()
            val lado = size.minDimension - trazo
            val origen = Offset(trazo / 2, trazo / 2)
            drawArc(pista, -90f, 360f, false, origen, Size(lado, lado), style = Stroke(trazo))
            if (avance > 0f) {
                drawArc(color, -90f, 360f * avance, false, origen, Size(lado, lado), style = Stroke(trazo, cap = StrokeCap.Round))
            }
        }
        Text(centro, style = MaterialTheme.typography.headlineSmall)
    }
}

/** Barra horizontal redondeada que se llena según [fraccion]. */
@Composable
fun BarraDeProgreso(
    fraccion: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraccion.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}

/** Círculo con las iniciales de un nombre: "Doña Rosa" → "DR". */
@Composable
fun Avatar(
    nombre: String,
    modifier: Modifier = Modifier,
    tamano: Dp = 44.dp,
) {
    val iniciales =
        nombre
            .split(' ')
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
    Box(
        modifier
            .size(tamano)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        // El círculo no crece con la letra del teléfono: las iniciales tampoco, para no salirse.
        ConEscalaDeLetraMaxima(1f) {
            Text(iniciales, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}
