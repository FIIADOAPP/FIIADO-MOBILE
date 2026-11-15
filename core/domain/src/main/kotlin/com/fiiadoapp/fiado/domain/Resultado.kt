package com.fiiadoapp.fiado.domain

/** Resultado de una operación del dominio que puede fallar por una regla de negocio. */
sealed interface Resultado<out T, out E> {
    data class Exito<out T>(
        val valor: T,
    ) : Resultado<T, Nothing>

    data class Fallo<out E>(
        val error: E,
    ) : Resultado<Nothing, E>
}
