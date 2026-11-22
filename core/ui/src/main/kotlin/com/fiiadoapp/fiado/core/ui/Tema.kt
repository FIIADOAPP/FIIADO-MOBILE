package com.fiiadoapp.fiado.core.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Colores de la app: un solo tema oscuro. El lila es el acento de lo que se toca; el verde, lo que
 * está bien (al día, cobrado); el coral, lo que se debe.
 */
object ColoresFiado {
    val Fondo = Color(0xFF202126)
    val Tarjeta = Color(0xFF292A30)
    val TarjetaElevada = Color(0xFF31323A)
    val Borde = Color(0xFF3A3B43)
    val Texto = Color(0xFFFFFFFF)
    val TextoSecundario = Color(0xFFA3A4AD)
    val Acento = Color(0xFF8B7CF6)
    val AcentoSuave = Color(0xFF38345A)
    val Positivo = Color(0xFF5CCB80)
    val PositivoSuave = Color(0xFF243A2D)
    val Deuda = Color(0xFFFF7B6B)
    val DeudaSuave = Color(0xFF45282A)

    /** Para lo que pide atención sin ser grave: un cliente que se demora o algo por vencer. */
    val Atencion = Color(0xFFF2B84B)
}

private val Esquema =
    darkColorScheme(
        primary = ColoresFiado.Acento,
        onPrimary = ColoresFiado.Texto,
        primaryContainer = ColoresFiado.AcentoSuave,
        onPrimaryContainer = Color(0xFFE4DFFF),
        inversePrimary = Color(0xFF5B4BC4),
        secondary = Color(0xFFBDB3FF),
        onSecondary = Color(0xFF231C4D),
        secondaryContainer = ColoresFiado.AcentoSuave,
        onSecondaryContainer = Color(0xFFE4DFFF),
        tertiary = ColoresFiado.Positivo,
        onTertiary = Color(0xFF0D2A17),
        tertiaryContainer = ColoresFiado.PositivoSuave,
        onTertiaryContainer = Color(0xFFC6F1D4),
        background = ColoresFiado.Fondo,
        onBackground = ColoresFiado.Texto,
        surface = ColoresFiado.Fondo,
        onSurface = ColoresFiado.Texto,
        surfaceVariant = ColoresFiado.Tarjeta,
        onSurfaceVariant = ColoresFiado.TextoSecundario,
        surfaceTint = ColoresFiado.Acento,
        inverseSurface = Color(0xFFE6E6EC),
        inverseOnSurface = ColoresFiado.Fondo,
        error = ColoresFiado.Deuda,
        onError = Color(0xFF3B0B06),
        errorContainer = ColoresFiado.DeudaSuave,
        onErrorContainer = Color(0xFFFFDAD5),
        outline = Color(0xFF5B5C65),
        outlineVariant = ColoresFiado.Borde,
        scrim = Color(0xFF000000),
        surfaceBright = ColoresFiado.TarjetaElevada,
        surfaceDim = ColoresFiado.Fondo,
        surfaceContainerLowest = Color(0xFF1B1C20),
        surfaceContainerLow = Color(0xFF25262B),
        surfaceContainer = ColoresFiado.Tarjeta,
        surfaceContainerHigh = ColoresFiado.TarjetaElevada,
        // Las tarjetas de Material usan este rol: así cada Card queda del color de tarjeta sin repetirlo.
        surfaceContainerHighest = ColoresFiado.Tarjeta,
    )

private val Formas =
    Shapes(
        extraSmall = RoundedCornerShape(10.dp),
        small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

private val base = Typography()

/** Encabezados grandes y apretados; el resto, la tipografía del sistema con pesos más claros. */
private val Tipografia =
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
        headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )

@Composable
fun FiadoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Esquema, shapes = Formas, typography = Tipografia, content = content)
}
