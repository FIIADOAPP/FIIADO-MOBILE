package com.fiiadoapp.fiado.core.data.cuenta

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/** Agrega el token de acceso solo a las llamadas marcadas con [REQUIERE_SESION]. */
class TokenDeAcceso(
    private val almacen: AlmacenSesion,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val pedida = chain.request()
        if (pedida.header(MARCA_SESION) == null) return chain.proceed(pedida)
        val sinMarca = pedida.newBuilder().removeHeader(MARCA_SESION).build()
        val sesion = runBlocking { almacen.actual() } ?: return chain.proceed(sinMarca)
        return chain.proceed(sinMarca.conToken(sesion.tokenAcceso))
    }
}

/**
 * Renueva la sesión cuando la API responde `401 token_vencido` y repite la petición (ADR-0005).
 * Varias peticiones vencidas a la vez renuevan una sola vez: las demás reutilizan el token nuevo.
 * Si la API cerró la sesión (otro teléfono, contraseña restablecida, token reutilizado), la borra
 * del teléfono y marca el aviso; los datos locales no se tocan.
 */
class RenovacionDeSesion(
    private val almacen: AlmacenSesion,
    private val api: ApiRefresco,
) : Authenticator {
    private val candado = Any()

    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        // Sin token, el 401 no es de sesión: por ejemplo, credenciales incorrectas al iniciarla.
        if (response.request.header(AUTORIZACION) == null) return null
        if (codigoDeError(response) != TOKEN_VENCIDO) {
            runBlocking { almacen.borrar(terminadaDesdeFuera = true) }
            return null
        }
        synchronized(candado) {
            val sesion = runBlocking { almacen.actual() } ?: return null
            val usado = response.request.header(AUTORIZACION)?.removePrefix(PREFIJO)
            if (usado != sesion.tokenAcceso) {
                // Otra petición ya renovó mientras esta esperaba.
                return response.request.conToken(sesion.tokenAcceso)
            }
            val renovada = api.refrescar(RefrescoDto(sesion.tokenRefresco)).execute()
            val tokens = renovada.body()
            if (!renovada.isSuccessful || tokens == null) {
                runBlocking { almacen.borrar(terminadaDesdeFuera = true) }
                return null
            }
            runBlocking { almacen.actualizarTokens(tokens.tokenAcceso, tokens.tokenRefresco) }
            return response.request.conToken(tokens.tokenAcceso)
        }
    }

    private fun codigoDeError(response: Response): String? =
        runCatching { Json.decodeFromString<ErrorDto>(response.peekBody(MAXIMO_CUERPO).string()).codigo }.getOrNull()

    private companion object {
        const val TOKEN_VENCIDO = "token_vencido"
        const val MAXIMO_CUERPO = 4_096L
    }
}

private const val AUTORIZACION = "Authorization"
private val MARCA_SESION = REQUIERE_SESION.substringBefore(":")
private const val PREFIJO = "Bearer "

private fun Request.conToken(token: String): Request = newBuilder().header(AUTORIZACION, PREFIJO + token).build()
