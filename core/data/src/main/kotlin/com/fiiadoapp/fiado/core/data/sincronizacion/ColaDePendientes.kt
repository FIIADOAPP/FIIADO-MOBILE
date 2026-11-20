package com.fiiadoapp.fiado.core.data.sincronizacion

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Cambio local que el servidor aún no confirmó (ADR-0003). Se escribe en la misma transacción que
 * el registro, así que no puede existir uno sin el otro.
 *
 * [orden] fija el orden de envío: un cliente siempre sale antes que sus movimientos. [id] hace
 * idempotente el envío. [datos] es la foto del registro, ya en el JSON que espera la API.
 */
@Entity(tableName = "operaciones_pendientes", indices = [Index("id", unique = true)])
data class OperacionPendienteEntity(
    @PrimaryKey(autoGenerate = true) val orden: Long = 0,
    val id: String,
    val entidad: String,
    val datos: String,
)

@Dao
interface ColaDao {
    @Insert
    suspend fun agregar(operacion: OperacionPendienteEntity)

    @Query("SELECT * FROM operaciones_pendientes ORDER BY orden LIMIT :limite")
    suspend fun primeras(limite: Int): List<OperacionPendienteEntity>

    @Query("DELETE FROM operaciones_pendientes WHERE id IN (:ids)")
    suspend fun quitar(ids: List<String>)
}
