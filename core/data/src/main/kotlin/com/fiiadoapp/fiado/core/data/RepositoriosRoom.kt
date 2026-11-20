package com.fiiadoapp.fiado.core.data

import androidx.room.withTransaction
import com.fiiadoapp.fiado.core.data.sincronizacion.OperacionPendienteEntity
import com.fiiadoapp.fiado.core.data.sincronizacion.TipoDeRegistro
import com.fiiadoapp.fiado.core.data.sincronizacion.foto
import com.fiiadoapp.fiado.domain.Cliente
import com.fiiadoapp.fiado.domain.ClienteConSaldo
import com.fiiadoapp.fiado.domain.Dinero
import com.fiiadoapp.fiado.domain.GeneradorDeIds
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
    private val db: FiadoDatabase,
    private val ids: GeneradorDeIds,
) : RepositorioClientes {
    private val dao = db.clienteDao()

    override fun observarActivos(busqueda: String): Flow<List<ClienteConSaldo>> =
        dao.observarActivos(patronDeBusqueda(busqueda)).map { filas ->
            filas.map { ClienteConSaldo(it.cliente.aDominio(), Dinero(it.saldoCentimos)) }
        }

    override fun observar(id: Uuid): Flow<Cliente?> = dao.observar(id.toString()).map { it?.aDominio() }

    override suspend fun guardar(cliente: Cliente) {
        val fila = cliente.aEntidad()
        db.guardarConOperacion(ids, TipoDeRegistro.CLIENTE, fila.foto()) { dao.guardar(fila) }
    }
}

/** Los instantes se guardan con precisión de milisegundos. */
class RepositorioMovimientosRoom(
    private val db: FiadoDatabase,
    private val ids: GeneradorDeIds,
) : RepositorioMovimientos {
    private val dao = db.movimientoDao()

    override fun observarDeCliente(clienteId: Uuid): Flow<List<Movimiento>> =
        dao.observarDeCliente(clienteId.toString()).map { filas -> filas.map { it.aDominio() } }

    override fun observarTodos(): Flow<List<Movimiento>> = dao.observarTodos().map { filas -> filas.map { it.aDominio() } }

    override suspend fun obtener(id: Uuid): Movimiento? = dao.obtener(id.toString())?.aDominio()

    override suspend fun ultimoMetodoCobroUsado(): Uuid? = dao.ultimoMetodoCobroUsado()?.let(Uuid::parse)

    override suspend fun guardar(movimiento: Movimiento) {
        val fila = movimiento.aEntidad()
        db.guardarConOperacion(ids, TipoDeRegistro.MOVIMIENTO, fila.foto()) { dao.guardar(fila) }
    }
}

class RepositorioMetodosCobroRoom(
    private val db: FiadoDatabase,
    private val ids: GeneradorDeIds,
) : RepositorioMetodosCobro {
    private val dao = db.metodoCobroDao()

    override fun observarActivos(): Flow<List<MetodoCobro>> = dao.observarActivos().map { filas -> filas.map { it.aDominio() } }

    override fun observarTodos(): Flow<List<MetodoCobro>> = dao.observarTodos().map { filas -> filas.map { it.aDominio() } }

    override suspend fun guardar(metodo: MetodoCobro) {
        val fila = metodo.aEntidad()
        db.guardarConOperacion(ids, TipoDeRegistro.METODO_COBRO, fila.foto()) { dao.guardar(fila) }
    }
}

/** El registro y su operación pendiente se escriben juntos o no se escribe ninguno (ADR-0003). */
private suspend fun FiadoDatabase.guardarConOperacion(
    ids: GeneradorDeIds,
    tipo: TipoDeRegistro,
    foto: String,
    guardar: suspend () -> Unit,
) = withTransaction {
    guardar()
    colaDao().agregar(OperacionPendienteEntity(id = ids.nuevo().toString(), entidad = tipo.valor, datos = foto))
}
