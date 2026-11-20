package com.fiiadoapp.fiado.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Íconos propios, dibujados con el mismo trazo redondeado que los de Material. */
object IconosFiado {
    /** Tres barras de alturas distintas: el resumen. */
    val Resumen: ImageVector by lazy {
        ImageVector
            .Builder("Resumen", 24.dp, 24.dp, 24f, 24f)
            .path(fill = SolidColor(Color.Black)) {
                barra(4f, 13f, 4f, 7f)
                barra(10f, 8f, 4f, 12f)
                barra(16f, 4f, 4f, 16f)
            }.build()
    }

    /** Un billete con una moneda al centro: lo relacionado con cobrar. */
    val Cobros: ImageVector by lazy {
        ImageVector
            .Builder("Cobros", 24.dp, 24.dp, 24f, 24f)
            .path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                // Billete con esquinas redondeadas.
                moveTo(5f, 5f)
                lineTo(19f, 5f)
                arcTo(3f, 3f, 0f, false, true, 22f, 8f)
                lineTo(22f, 16f)
                arcTo(3f, 3f, 0f, false, true, 19f, 19f)
                lineTo(5f, 19f)
                arcTo(3f, 3f, 0f, false, true, 2f, 16f)
                lineTo(2f, 8f)
                arcTo(3f, 3f, 0f, false, true, 5f, 5f)
                close()
                // Moneda hueca al centro.
                moveTo(12f, 9f)
                arcTo(3f, 3f, 0f, true, false, 12f, 15f)
                arcTo(3f, 3f, 0f, true, false, 12f, 9f)
                close()
            }.build()
    }

    private fun PathBuilder.barra(
        x: Float,
        y: Float,
        ancho: Float,
        alto: Float,
    ) {
        val radio = ancho / 2
        moveTo(x, y + radio)
        arcTo(radio, radio, 0f, false, true, x + ancho, y + radio)
        lineTo(x + ancho, y + alto)
        lineTo(x, y + alto)
        close()
    }
}
