package com.fiiadoapp.fiado.domain

import kotlinx.coroutines.flow.Flow

/** Producto de Google Play que quita los anuncios para siempre (ADR-0011). */
const val PRODUCTO_QUITAR_ANUNCIOS = "quitar_anuncios"

/** Cómo está la compra que quita los anuncios, para mostrarla en Cuenta. */
sealed interface EstadoQuitarAnuncios {
    /** Todavía no se sabe: Play no respondió. */
    data object Consultando : EstadoQuitarAnuncios

    /** Se puede comprar, con el precio en la moneda del usuario. */
    data class Disponible(
        val precio: String,
    ) : EstadoQuitarAnuncios

    /** Se eligió un medio de pago que se confirma después; mientras tanto siguen los anuncios. */
    data object Pendiente : EstadoQuitarAnuncios

    data object Comprado : EstadoQuitarAnuncios

    /** Play no está disponible (sin cuenta de Google o sin conexión la primera vez). */
    data object NoDisponible : EstadoQuitarAnuncios
}

/** Una compra tal como la informa Google Play, sin depender de su biblioteca. */
data class CompraDePlay(
    val productos: List<String>,
    val pagada: Boolean,
    val pendiente: Boolean,
    val reconocida: Boolean,
    val token: String,
)

/** Qué significan para los anuncios las compras que devuelve Google Play (ADR-0011). */
object ReglasDeCompra {
    fun quitaAnuncios(compras: List<CompraDePlay>): Boolean = compras.any { it.esDelProducto() && it.pagada }

    fun hayPendiente(compras: List<CompraDePlay>): Boolean = compras.any { it.esDelProducto() && it.pendiente }

    /** Pagadas y todavía sin reconocer: si no se reconocen en 3 días, Google las devuelve. */
    fun porReconocer(compras: List<CompraDePlay>): List<CompraDePlay> = compras.filter { it.esDelProducto() && it.pagada && !it.reconocida }

    private fun CompraDePlay.esDelProducto() = PRODUCTO_QUITAR_ANUNCIOS in productos
}

/** Último estado conocido de la compra, para que sin internet quien pagó siga sin anuncios. */
interface RepositorioCompra {
    fun observarAnunciosQuitados(): Flow<Boolean>

    suspend fun guardarAnunciosQuitados(quitados: Boolean)
}
