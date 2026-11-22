package com.fiiadoapp.fiado.core.data.sincronizacion

import com.fiiadoapp.fiado.core.data.ClienteEntity
import com.fiiadoapp.fiado.core.data.MetodoCobroEntity
import com.fiiadoapp.fiado.core.data.MovimientoEntity
import com.fiiadoapp.fiado.core.data.ProrrogaEntity
import com.fiiadoapp.fiado.core.data.normalizarParaBusqueda
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

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
    /** Una API anterior a la v0.4.0 no lo envía al restaurar. */
    val vence: String? = null,
)

@Serializable
internal data class ProrrogaFoto(
    val id: String,
    @SerialName("movimiento_id") val movimientoId: String,
    val vence: String,
    @SerialName("registrada_en") val registradaEn: String,
)

/** Valor del campo `entidad` de una operación. */
internal enum class TipoDeRegistro(
    val valor: String,
) {
    METODO_COBRO("metodo_cobro"),
    CLIENTE("cliente"),
    MOVIMIENTO("movimiento"),
    PRORROGA("prorroga"),
}

/** Los `null` también se escriben: la API exige la foto completa. Lo desconocido se ignora al leer. */
internal val jsonDeSincronizacion =
    Json {
        explicitNulls = true
        // La foto es completa: también se escriben los campos con valor por defecto, como `vence`.
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

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
                vence = vence?.let { LocalDate.ofEpochDay(it).toString() },
            ),
        )

internal fun ProrrogaEntity.foto(): String =
    jsonDeSincronizacion.encodeToString(
        ProrrogaFoto.serializer(),
        ProrrogaFoto(id, movimientoId, LocalDate.ofEpochDay(vence).toString(), instante(registradaEn)),
    )

/** Desde la fila guardada, no desde el dominio: así la foto tiene la misma precisión (milisegundos) cada vez. */
private fun instante(milisegundos: Long): String = Instant.ofEpochMilli(milisegundos).toString()

/** Un registro que llegó al restaurar, ya como fila de Room. */
internal sealed interface Restaurado {
    val tipo: TipoDeRegistro
    val id: String

    data class Metodo(
        val fila: MetodoCobroEntity,
    ) : Restaurado {
        override val tipo = TipoDeRegistro.METODO_COBRO
        override val id = fila.id
    }

    data class Cliente(
        val fila: ClienteEntity,
    ) : Restaurado {
        override val tipo = TipoDeRegistro.CLIENTE
        override val id = fila.id
    }

    data class Movimiento(
        val fila: MovimientoEntity,
    ) : Restaurado {
        override val tipo = TipoDeRegistro.MOVIMIENTO
        override val id = fila.id
    }

    data class Prorroga(
        val fila: ProrrogaEntity,
    ) : Restaurado {
        override val tipo = TipoDeRegistro.PRORROGA
        override val id = fila.id
    }
}

/** Falla con [IllegalArgumentException] si el registro no tiene la forma esperada. */
internal fun decodificar(registro: RegistroDto): Restaurado {
    val datos = registro.datos
    return try {
        when (registro.entidad) {
            TipoDeRegistro.METODO_COBRO.valor -> {
                val foto = jsonDeSincronizacion.decodeFromJsonElement(MetodoCobroFoto.serializer(), datos)
                Restaurado.Metodo(
                    MetodoCobroEntity(
                        foto.id,
                        foto.nombre,
                        milis(foto.creadoEn),
                        milis(foto.actualizadoEn),
                        foto.archivadoEn?.let(::milis),
                    ),
                )
            }

            TipoDeRegistro.CLIENTE.valor -> {
                val foto = jsonDeSincronizacion.decodeFromJsonElement(ClienteFoto.serializer(), datos)
                Restaurado.Cliente(
                    ClienteEntity(
                        id = foto.id,
                        nombre = foto.nombre,
                        nombreBusqueda = normalizarParaBusqueda(foto.nombre),
                        telefono = foto.telefono,
                        creadoEn = milis(foto.creadoEn),
                        actualizadoEn = milis(foto.actualizadoEn),
                        archivadoEn = foto.archivadoEn?.let(::milis),
                    ),
                )
            }

            TipoDeRegistro.MOVIMIENTO.valor -> {
                val foto = jsonDeSincronizacion.decodeFromJsonElement(MovimientoFoto.serializer(), datos)
                Restaurado.Movimiento(
                    MovimientoEntity(
                        id = foto.id,
                        clienteId = foto.clienteId,
                        tipo = foto.tipo.uppercase(),
                        montoCentimos = foto.monto,
                        nota = foto.nota,
                        fecha = LocalDate.parse(foto.fecha).toEpochDay(),
                        registradoEn = milis(foto.registradoEn),
                        anuladoEn = foto.anuladoEn?.let(::milis),
                        metodoCobroId = foto.metodoCobroId,
                        vence = foto.vence?.let { LocalDate.parse(it).toEpochDay() },
                    ),
                )
            }

            TipoDeRegistro.PRORROGA.valor -> {
                val foto = jsonDeSincronizacion.decodeFromJsonElement(ProrrogaFoto.serializer(), datos)
                Restaurado.Prorroga(
                    ProrrogaEntity(foto.id, foto.movimientoId, LocalDate.parse(foto.vence).toEpochDay(), milis(foto.registradaEn)),
                )
            }

            else -> {
                throw IllegalArgumentException("Entidad desconocida: ${registro.entidad}")
            }
        }
    } catch (error: DateTimeException) {
        throw IllegalArgumentException("Fecha inválida", error)
    }
}

/** La API puede escribir el desfase como `Z` o `+00:00`; `OffsetDateTime` acepta ambos en todas las versiones de Android. */
private fun milis(texto: String): Long = OffsetDateTime.parse(texto).toInstant().toEpochMilli()
