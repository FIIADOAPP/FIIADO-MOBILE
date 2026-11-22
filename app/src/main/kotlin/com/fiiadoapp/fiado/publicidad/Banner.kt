package com.fiiadoapp.fiado.publicidad

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.fiiadoapp.fiado.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/** Tamaño del banner adaptativo para el ancho de la pantalla (ADR-0011). */
@Composable
fun tamanoDeBanner(): AdSize {
    val contexto = LocalContext.current
    val ancho =
        with(LocalDensity.current) {
            LocalWindowInfo.current.containerSize.width
                .toDp()
                .value
                .toInt()
        }
    return remember(ancho) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(contexto, ancho) }
}

/** Alto que ocupa el banner, para que las pantallas dejen ese espacio libre al final. */
fun AdSize.alto(): Dp = height.dp

/** Banner de AdMob a todo el ancho. En `debug`, o sin los ids reales, usa el bloque de prueba de Google. */
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
    AndroidView(factory = { vista }, modifier = modifier.fillMaxWidth().height(tamano.alto()))
}
