package com.fiiadoapp.fiado.core.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Instantes en milisegundos desde la época Unix (UTC). */
@Entity(tableName = "clientes", indices = [Index("nombreBusqueda")])
data class ClienteEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    /** Nombre en minúsculas y sin tildes, para buscar "dona" y encontrar "Doña". */
    val nombreBusqueda: String,
    val telefono: String?,
    val creadoEn: Long,
    val actualizadoEn: Long,
    val archivadoEn: Long?,
)

@Entity(tableName = "metodos_cobro")
data class MetodoCobroEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val creadoEn: Long,
    val actualizadoEn: Long,
    val archivadoEn: Long?,
)

/** `fecha` en días desde la época (`LocalDate.toEpochDay`); los instantes en milisegundos UTC. */
@Entity(
    tableName = "movimientos",
    foreignKeys = [
        ForeignKey(entity = ClienteEntity::class, parentColumns = ["id"], childColumns = ["clienteId"]),
        ForeignKey(entity = MetodoCobroEntity::class, parentColumns = ["id"], childColumns = ["metodoCobroId"]),
    ],
    indices = [Index("clienteId"), Index("fecha"), Index("metodoCobroId")],
)
data class MovimientoEntity(
    @PrimaryKey val id: String,
    val clienteId: String,
    val tipo: String,
    val montoCentimos: Long,
    val nota: String?,
    val fecha: Long,
    val registradoEn: Long,
    val anuladoEn: Long?,
    /** Obligatorio en un pago y nulo en un fiado (ADR-0004). */
    val metodoCobroId: String?,
)

data class ClienteConSaldoFila(
    @Embedded val cliente: ClienteEntity,
    val saldoCentimos: Long,
)
