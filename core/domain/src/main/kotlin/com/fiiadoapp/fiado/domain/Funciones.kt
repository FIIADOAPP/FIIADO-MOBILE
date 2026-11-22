package com.fiiadoapp.fiado.domain

/**
 * Lo que ofrece esta compilación de la app. La versión 1.0 de la Play Store guarda la libreta solo en el
 * teléfono, sin cuenta ni respaldo en el servidor (ADR-0010).
 */
data class Funciones(
    val cuentaDisponible: Boolean,
)
