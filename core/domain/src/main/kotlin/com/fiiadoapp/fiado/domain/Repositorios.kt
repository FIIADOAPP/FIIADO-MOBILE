package com.fiiadoapp.fiado.domain

import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

data class ClienteConSaldo(
    val cliente: Cliente,
    val saldo: Dinero,
)

interface RepositorioClientes {
    /** Clientes no archivados cuyo nombre contiene [busqueda], sin distinguir mayúsculas ni tildes, ordenados por nombre. */
    fun observarActivos(busqueda: String): Flow<List<ClienteConSaldo>>

    fun observar(id: Uuid): Flow<Cliente?>

    /** Inserta el cliente o reemplaza el que tenga el mismo `id`. */
    suspend fun guardar(cliente: Cliente)
}

interface RepositorioMovimientos {
    /** Movimientos del cliente, anulados incluidos, del más reciente al más antiguo. */
    fun observarDeCliente(clienteId: Uuid): Flow<List<Movimiento>>

    fun observarTodos(): Flow<List<Movimiento>>

    suspend fun obtener(id: Uuid): Movimiento?

    /** Método del pago vigente registrado más recientemente; preselecciona el siguiente cobro. */
    suspend fun ultimoMetodoCobroUsado(): Uuid?

    /** Inserta el movimiento o reemplaza el que tenga el mismo `id`. */
    suspend fun guardar(movimiento: Movimiento)
}

interface RepositorioMetodosCobro {
    /** Métodos no archivados, en orden de creación. */
    fun observarActivos(): Flow<List<MetodoCobro>>

    /** Inserta el método o reemplaza el que tenga el mismo `id`. */
    suspend fun guardar(metodo: MetodoCobro)
}
