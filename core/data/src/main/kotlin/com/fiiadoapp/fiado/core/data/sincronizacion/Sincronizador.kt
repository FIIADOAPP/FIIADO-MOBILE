package com.fiiadoapp.fiado.core.data.sincronizacion

import com.fiiadoapp.fiado.core.data.cuenta.AlmacenSesion
import com.fiiadoapp.fiado.core.data.cuenta.REQUIERE_SESION
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.HttpException
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.IOException
import javax.inject.Inject

/** Endpoint de envío de FIIADO-API (ADR-0003). */
interface ApiSincronizacion {
    @POST("sincronizacion/operaciones")
    @Headers(REQUIERE_SESION)
    suspend fun subir(
        @Body lote: LoteDto,
    ): ConfirmacionDto

    @GET("sincronizacion/restauracion")
    @Headers(REQUIERE_SESION)
    suspend fun restaurar(
        @Query("cursor") cursor: String?,
        @Query("limite") limite: Int,
    ): PaginaDto
}

@Serializable
data class OperacionDto(
    val id: String,
    val entidad: String,
    val datos: JsonElement,
)

@Serializable
data class LoteDto(
    val operaciones: List<OperacionDto>,
)

@Serializable
data class ConfirmacionDto(
    val aplicadas: List<String>,
)

enum class ResultadoEnvio {
    /** La cola quedó vacía. */
    COMPLETO,

    /** No hay sesión, o la API la cerró: la cola se conserva para cuando vuelva a iniciarse. */
    SIN_SESION,

    /** Sin red o el servidor falló: se reintenta más tarde con lo que quedó en la cola. */
    REINTENTAR,

    /** El servidor rechazó un lote. Es un error de programación: reintentar no lo arregla. */
    RECHAZADO,
}

/** Lo que el trabajo en segundo plano ejecuta; una interfaz para probar el trabajo sin red. */
fun interface EnvioDePendientes {
    suspend fun enviar(): ResultadoEnvio
}

/**
 * Sube la cola en el orden en que se registró, en lotes de hasta [TAMANO_LOTE]. Una operación sale
 * de la cola solo cuando el servidor confirma su `id`; si la respuesta se pierde, el lote se reenvía
 * y el servidor lo reconoce sin aplicarlo dos veces.
 */
class Sincronizador
    @Inject
    constructor(
        private val cola: ColaDao,
        private val api: ApiSincronizacion,
        private val almacen: AlmacenSesion,
        private val restaurador: Restaurador,
    ) : EnvioDePendientes {
        /** Primero restaura si hace falta (ADR-0003): así nunca se sube algo antes de traer lo del servidor. */
        override suspend fun enviar(): ResultadoEnvio {
            val sesion = almacen.actual() ?: return ResultadoEnvio.SIN_SESION
            restaurador.restaurarSiFalta(sesion.correo)?.let { return it }
            while (true) {
                if (almacen.actual() == null) return ResultadoEnvio.SIN_SESION
                val pendientes = cola.primeras(TAMANO_LOTE)
                if (pendientes.isEmpty()) return ResultadoEnvio.COMPLETO
                val confirmacion =
                    when (val respuesta = llamarApi { api.subir(LoteDto(pendientes.map { it.aDto() })) }) {
                        is Respuesta.Exito -> respuesta.valor
                        is Respuesta.Fallo -> return respuesta.motivo
                    }
                // Una confirmación que no incluye nada de lo enviado no puede avanzar la cola: se reintenta.
                if (confirmacion.aplicadas.none { aplicada -> pendientes.any { it.id == aplicada } }) {
                    return ResultadoEnvio.REINTENTAR
                }
                cola.quitar(confirmacion.aplicadas)
            }
        }

        private fun OperacionPendienteEntity.aDto() = OperacionDto(id, entidad, jsonDeSincronizacion.parseToJsonElement(datos))

        private companion object {
            const val TAMANO_LOTE = 100
        }
    }

internal sealed interface Respuesta<out T> {
    data class Exito<T>(
        val valor: T,
    ) : Respuesta<T>

    data class Fallo(
        val motivo: ResultadoEnvio,
    ) : Respuesta<Nothing>
}

/** Traduce los fallos de una llamada a la API a lo que el envío debe hacer. */
internal suspend fun <T> llamarApi(llamada: suspend () -> T): Respuesta<T> =
    try {
        Respuesta.Exito(llamada())
    } catch (error: HttpException) {
        Respuesta.Fallo(
            when (error.code()) {
                HTTP_NO_AUTORIZADO -> ResultadoEnvio.SIN_SESION
                in HTTP_ERRORES_DEL_CLIENTE -> ResultadoEnvio.RECHAZADO
                else -> ResultadoEnvio.REINTENTAR
            },
        )
    } catch (_: IOException) {
        Respuesta.Fallo(ResultadoEnvio.REINTENTAR)
    }

private const val HTTP_NO_AUTORIZADO = 401

/** 408 y 429 son pasajeros: se reintentan. */
private val HTTP_ERRORES_DEL_CLIENTE = (400..499) - setOf(408, 429)
