package com.fiiadoapp.fiado.publicidad

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.fiiadoapp.fiado.BuildConfig
import com.fiiadoapp.fiado.R
import com.fiiadoapp.fiado.core.ui.MargenPantalla
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/** Margen entre el borde de la tarjeta y el anuncio: el anuncio nunca se recorta (política de AdMob). */
private val RellenoTarjeta = 6.dp
private val AltoEtiqueta = 16.dp
private val EspacioEtiqueta = 4.dp

/** Separa la tarjeta del anuncio de la barra inferior, para que no parezca parte del menú. */
private val SeparacionBarra = 6.dp

/** Tamaño del banner adaptativo para el ancho que deja la tarjeta (ADR-0011). */
@Composable
fun tamanoDeBanner(): AdSize {
    val contexto = LocalContext.current
    val ancho =
        with(LocalDensity.current) {
            (
                LocalWindowInfo.current.containerSize.width
                    .toDp() - MargenPantalla * 2 - RellenoTarjeta * 2
            ).value
                .toInt()
        }
    return remember(ancho) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(contexto, ancho) }
}

/** Alto de la tarjeta con su etiqueta, para que las pantallas dejen ese espacio libre al final. */
fun AdSize.alto(): Dp = AltoEtiqueta + EspacioEtiqueta + RellenoTarjeta * 2 + height.dp + SeparacionBarra

/**
 * Banner de AdMob dentro de una tarjeta con los márgenes de la app y la etiqueta «Anuncio», para que se
 * distinga del contenido. En `debug`, o sin los ids reales, usa el bloque de prueba de Google.
 */
@Composable
fun BannerAnuncio(
    tamano: AdSize,
    modifier: Modifier = Modifier,
) {
    val contexto = LocalContext.current
    val vista =
        remember(tamano) {
            AdView(contexto).apply {
                adUnitId = BuildConfig.ADMOB_BANNER
                setAdSize(tamano)
                loadAd(AdRequest.Builder().build())
            }
        }
    DisposableEffect(vista) { onDispose { vista.destroy() } }
    // La etiqueta va dentro de la tarjeta, con fondo, para que no flote sobre la lista.
    Surface(
        modifier = modifier.fillMaxWidth().padding(start = MargenPantalla, end = MargenPantalla, bottom = SeparacionBarra),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(RellenoTarjeta), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.anuncio_etiqueta),
                modifier = Modifier.align(Alignment.Start).height(AltoEtiqueta).padding(start = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AndroidView(
                factory = { vista },
                modifier = Modifier.padding(top = EspacioEtiqueta).size(tamano.width.dp, tamano.height.dp),
            )
        }
    }
}
