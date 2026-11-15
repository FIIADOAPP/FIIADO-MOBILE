package com.fiiadoapp.fiado.core.data

import com.fiiadoapp.fiado.domain.Cliente
import com.fiiadoapp.fiado.domain.ClienteConSaldo
import com.fiiadoapp.fiado.domain.Dinero
import com.fiiadoapp.fiado.domain.MetodoCobro
import com.fiiadoapp.fiado.domain.Movimiento
import com.fiiadoapp.fiado.domain.RepositorioClientes
import com.fiiadoapp.fiado.domain.RepositorioMetodosCobro
import com.fiiadoapp.fiado.domain.RepositorioMovimientos
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.uuid.Uuid

/** Los instantes se guardan con precisión de milisegundos. */
class RepositorioClientesRoom(
    private val dao: ClienteDao,
) : RepositorioClientes {
    override fun observarActivos(busqueda: String): Flow<List<ClienteConSaldo>> =
        dao.observarActivos(patronDeBusqueda(busqueda)).map { filas ->
            filas.map { ClienteConSaldo(it.cliente.aDominio(), Dinero(it.saldoCentimos)) }
        }

    override fun observar(id: Uuid): Flow<Cliente?> = dao.observar(id.toString()).map { it?.aDominio() }

    override suspend fun guardar(cliente: Cliente) = dao.guardar(cliente.aEntidad())
}

/** Los instantes se guardan con precisión de milisegundos. */
class RepositorioMovimientosRoom(
    private val dao: MovimientoDao,
) : RepositorioMovimientos {
    override fun observarDeCliente(clienteId: Uuid): Flow<List<Movimiento>> =
        dao.observarDeCliente(clienteId.toString()).map { filas -> filas.map { it.aDominio() } }

    override fun observarTodos(): Flow<List<Movimiento>> = dao.observarTodos().map { filas -> filas.map { it.aDominio() } }

    override suspend fun obtener(id: Uuid): Movimiento? = dao.obtener(id.toString())?.aDominio()

    override suspend fun ultimoMetodoCobroUsado(): Uuid? = dao.ultimoMetodoCobroUsado()?.let(Uuid::parse)

    override suspend fun guardar(movimiento: Movimiento) = dao.guardar(movimiento.aEntidad())
}

class RepositorioMetodosCobroRoom(
    private val dao: MetodoCobroDao,
) : RepositorioMetodosCobro {
    override fun observarActivos(): Flow<List<MetodoCobro>> = dao.observarActivos().map { filas -> filas.map { it.aDominio() } }

    override suspend fun guardar(metodo: MetodoCobro) = dao.guardar(metodo.aEntidad())
}
