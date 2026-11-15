package com.fiiadoapp.fiado.core.testing

import com.fiiadoapp.fiado.domain.Cliente
import com.fiiadoapp.fiado.domain.ClienteConSaldo
import com.fiiadoapp.fiado.domain.Movimiento
import com.fiiadoapp.fiado.domain.RepositorioClientes
import com.fiiadoapp.fiado.domain.RepositorioMovimientos
import com.fiiadoapp.fiado.domain.saldo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import kotlin.uuid.Uuid

/** Libreta en memoria que cumple los contratos de los repositorios. */
class LibretaFalsa {
    val clientes = MutableStateFlow<Map<Uuid, Cliente>>(emptyMap())
    val movimientos = MutableStateFlow<Map<Uuid, Movimiento>>(emptyMap())

    val repositorioClientes =
        object : RepositorioClientes {
            override fun observarActivos(busqueda: String): Flow<List<ClienteConSaldo>> =
                combine(clientes, movimientos) { clientes, movimientos ->
                    clientes.values
                        .filter { !it.estaArchivado && it.nombre.contains(busqueda.trim(), ignoreCase = true) }
                        .sortedBy { it.nombre.lowercase() }
                        .map { cliente -> ClienteConSaldo(cliente, movimientos.values.filter { it.clienteId == cliente.id }.saldo()) }
                }

            override fun observar(id: Uuid): Flow<Cliente?> = clientes.map { it[id] }

            override suspend fun guardar(cliente: Cliente) = clientes.update { it + (cliente.id to cliente) }
        }

    val repositorioMovimientos =
        object : RepositorioMovimientos {
            override fun observarDeCliente(clienteId: Uuid): Flow<List<Movimiento>> =
                movimientos.map { todos -> todos.values.filter { it.clienteId == clienteId } }

            override fun observarTodos(): Flow<List<Movimiento>> = movimientos.map { it.values.toList() }

            override suspend fun obtener(id: Uuid): Movimiento? = movimientos.value[id]

            override suspend fun guardar(movimiento: Movimiento) = movimientos.update { it + (movimiento.id to movimiento) }
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReglaDispatcherPrincipal : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(UnconfinedTestDispatcher())

    override fun finished(description: Description) = Dispatchers.resetMain()
}
