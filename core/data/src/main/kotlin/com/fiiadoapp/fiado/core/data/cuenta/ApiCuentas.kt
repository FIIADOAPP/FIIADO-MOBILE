package com.fiiadoapp.fiado.core.data.cuenta

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

/** Marca las llamadas que llevan el token de acceso; [TokenDeAcceso] la reemplaza por el token. */
const val REQUIERE_SESION = "Fiado-Requiere-Sesion: si"

/** Endpoints de cuentas y sesiones de FIIADO-API (ADR-0002 de la API). */
interface ApiCuentas {
    @POST("cuentas")
    suspend fun registrar(
        @Body datos: RegistroDto,
    ): CuentaDto

    @POST("cuentas/verificacion")
    suspend fun verificar(
        @Body datos: VerificacionDto,
    )

    @POST("cuentas/verificacion/reenvio")
    suspend fun reenviarCodigo(
        @Body datos: CorreoDto,
    )

    @POST("cuentas/recuperacion")
    suspend fun solicitarRecuperacion(
        @Body datos: CorreoDto,
    )

    @POST("cuentas/recuperacion/restablecimiento")
    suspend fun restablecer(
        @Body datos: RestablecimientoDto,
    )

    @POST("sesiones")
    suspend fun iniciarSesion(
        @Body datos: InicioDeSesionDto,
    ): SesionAbiertaDto

    @GET("cuentas/yo")
    @Headers(REQUIERE_SESION)
    suspend fun yo(): CuentaDto

    @DELETE("sesiones/actual")
    @Headers(REQUIERE_SESION)
    suspend fun cerrarSesion()
}

/** Va en un cliente sin renovación automática: es la llamada que renueva. Síncrona, para el `Authenticator`. */
interface ApiRefresco {
    @POST("sesiones/refresco")
    fun refrescar(
        @Body datos: RefrescoDto,
    ): Call<TokensDto>
}

@Serializable
data class RegistroDto(
    val correo: String,
    val contrasena: String,
    @SerialName("nombre_negocio") val nombreNegocio: String,
)

@Serializable
data class CuentaDto(
    val correo: String,
    @SerialName("nombre_negocio") val nombreNegocio: String,
)

@Serializable
data class VerificacionDto(
    val correo: String,
    val codigo: String,
)

@Serializable
data class CorreoDto(
    val correo: String,
)

@Serializable
data class RestablecimientoDto(
    val correo: String,
    val codigo: String,
    @SerialName("contrasena_nueva") val contrasenaNueva: String,
)

@Serializable
data class InicioDeSesionDto(
    val correo: String,
    val contrasena: String,
    val ceder: Boolean,
)

@Serializable
data class SesionAbiertaDto(
    @SerialName("token_acceso") val tokenAcceso: String,
    @SerialName("token_refresco") val tokenRefresco: String,
    val cuenta: CuentaDto,
)

@Serializable
data class RefrescoDto(
    @SerialName("token_refresco") val tokenRefresco: String,
)

@Serializable
data class TokensDto(
    @SerialName("token_acceso") val tokenAcceso: String,
    @SerialName("token_refresco") val tokenRefresco: String,
)

@Serializable
data class ErrorDto(
    val codigo: String,
    val mensaje: String,
)
