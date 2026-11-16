package com.fiiadoapp.fiado.core.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ClienteDao {
    /** El saldo se calcula aquí, sumando los movimientos vigentes; nunca se guarda. */
    @Query(
        """
        SELECT c.*,
            COALESCE(SUM(
                CASE
                    WHEN m.anuladoEn IS NOT NULL THEN 0
                    WHEN m.tipo = 'FIADO' THEN m.montoCentimos
                    ELSE -m.montoCentimos
                END
            ), 0) AS saldoCentimos
        FROM clientes c
        LEFT JOIN movimientos m ON m.clienteId = c.id
        WHERE c.archivadoEn IS NULL
            AND c.nombreBusqueda LIKE '%' || :patron || '%' ESCAPE '\'
        GROUP BY c.id
        ORDER BY c.nombreBusqueda, c.id
        """,
    )
    fun observarActivos(patron: String): Flow<List<ClienteConSaldoFila>>

    @Query("SELECT * FROM clientes WHERE id = :id")
    fun observar(id: String): Flow<ClienteEntity?>

    @Upsert
    suspend fun guardar(cliente: ClienteEntity)
}

@Dao
interface MovimientoDao {
    @Query("SELECT * FROM movimientos WHERE clienteId = :clienteId ORDER BY fecha DESC, registradoEn DESC, id DESC")
    fun observarDeCliente(clienteId: String): Flow<List<MovimientoEntity>>

    @Query("SELECT * FROM movimientos WHERE id = :id")
    suspend fun obtener(id: String): MovimientoEntity?

    @Query("SELECT * FROM movimientos")
    fun observarTodos(): Flow<List<MovimientoEntity>>

    @Upsert
    suspend fun guardar(movimiento: MovimientoEntity)

    @Query(
        "SELECT metodoCobroId FROM movimientos WHERE tipo = 'PAGO' AND anuladoEn IS NULL " +
            "ORDER BY registradoEn DESC, id DESC LIMIT 1",
    )
    suspend fun ultimoMetodoCobroUsado(): String?
}

@Dao
interface MetodoCobroDao {
    @Query("SELECT * FROM metodos_cobro WHERE archivadoEn IS NULL ORDER BY creadoEn, id")
    fun observarActivos(): Flow<List<MetodoCobroEntity>>

    @Query("SELECT * FROM metodos_cobro ORDER BY creadoEn, id")
    fun observarTodos(): Flow<List<MetodoCobroEntity>>

    @Upsert
    suspend fun guardar(metodo: MetodoCobroEntity)
}
