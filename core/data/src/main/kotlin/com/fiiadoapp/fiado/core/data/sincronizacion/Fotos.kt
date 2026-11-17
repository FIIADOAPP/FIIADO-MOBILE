package com.fiiadoapp.fiado.core.data.sincronizacion

import com.fiiadoapp.fiado.core.data.ClienteEntity
import com.fiiadoapp.fiado.core.data.MetodoCobroEntity
import com.fiiadoapp.fiado.core.data.MovimientoEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate

/**
 * Foto completa de un registro en el formato de FIIADO-API (ADR-0003): campos en `snake_case`,
 * dinero en céntimos, `fecha` sin hora e instantes ISO 8601 en UTC.
 */
@Serializable
internal data class MetodoCobroFoto(
    val id: String,
    val nombre: String,
    @SerialName("creado_en") val creadoEn: String,
    @SerialName("actualizado_en") val actualizadoEn: String,
    @SerialName("archivado_en") val archivadoEn: String?,
)

@Serializable
internal data class ClienteFoto(
    val id: String,
    val nombre: String,
    val telefono: String?,
    @SerialName("creado_en") val creadoEn: String,
    @SerialName("actualizado_en") val actualizadoEn: String,
    @SerialName("archivado_en") val archivadoEn: String?,
)

@Serializable
internal data class MovimientoFoto(
    val id: String,
    @SerialName("cliente_id") val clienteId: String,
    val tipo: String,
    val monto: Long,
    val nota: String?,
    val fecha: String,
    @SerialName("registrado_en") val registradoEn: String,
    @SerialName("anulado_en") val anuladoEn: String?,
    @SerialName("metodo_cobro_id") val metodoCobroId: String?,
)

/** Valor del campo `entidad` de una operación. */
internal enum class TipoDeRegistro(
    val valor: String,
) {
    METODO_COBRO("metodo_cobro"),
    CLIENTE("cliente"),
    MOVIMIENTO("movimiento"),
}

/** Los `null` también se escriben: la API exige la foto completa. */
internal val jsonDeSincronizacion = Json { explicitNulls = true }

internal fun MetodoCobroEntity.foto(): String =
    jsonDeSincronizacion
        .encodeToString(
            MetodoCobroFoto.serializer(),
            MetodoCobroFoto(id, nombre, instante(creadoEn), instante(actualizadoEn), archivadoEn?.let(::instante)),
        )

internal fun ClienteEntity.foto(): String =
    jsonDeSincronizacion
        .encodeToString(
            ClienteFoto.serializer(),
            ClienteFoto(id, nombre, telefono, instante(creadoEn), instante(actualizadoEn), archivadoEn?.let(::instante)),
        )

internal fun MovimientoEntity.foto(): String =
    jsonDeSincronizacion
        .encodeToString(
            MovimientoFoto.serializer(),
            MovimientoFoto(
                id = id,
                clienteId = clienteId,
                tipo = tipo.lowercase(),
                monto = montoCentimos,
                nota = nota,
                fecha = LocalDate.ofEpochDay(fecha).toString(),
                registradoEn = instante(registradoEn),
                anuladoEn = anuladoEn?.let(::instante),
                metodoCobroId = metodoCobroId,
            ),
        )

/** Desde la fila guardada, no desde el dominio: así la foto tiene la misma precisión (milisegundos) cada vez. */
private fun instante(milisegundos: Long): String = Instant.ofEpochMilli(milisegundos).toString()
