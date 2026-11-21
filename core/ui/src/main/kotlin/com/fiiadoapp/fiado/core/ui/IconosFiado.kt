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

    /** Una persona con un "+" al lado: agregar un cliente, distinto de registrar un cobro. */
    val NuevoCliente: ImageVector by lazy {
        ImageVector
            .Builder("NuevoCliente", 24.dp, 24.dp, 24f, 24f)
            .path(fill = SolidColor(Color.Black)) {
                // Cabeza.
                moveTo(9f, 4f)
                arcTo(4f, 4f, 0f, true, true, 9f, 12f)
                arcTo(4f, 4f, 0f, true, true, 9f, 4f)
                close()
                // Hombros.
                moveTo(1f, 19f)
                curveTo(1f, 15.7f, 5f, 14f, 9f, 14f)
                curveTo(13f, 14f, 17f, 15.7f, 17f, 19f)
                lineTo(17f, 20f)
                lineTo(1f, 20f)
                close()
                // El "+".
                moveTo(19f, 6f)
                lineTo(21f, 6f)
                lineTo(21f, 9f)
                lineTo(24f, 9f)
                lineTo(24f, 11f)
                lineTo(21f, 11f)
                lineTo(21f, 14f)
                lineTo(19f, 14f)
                lineTo(19f, 11f)
                lineTo(16f, 11f)
                lineTo(16f, 9f)
                lineTo(19f, 9f)
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
