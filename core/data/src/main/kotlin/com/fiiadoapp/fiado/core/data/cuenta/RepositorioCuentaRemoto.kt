package com.fiiadoapp.fiado.core.data.cuenta

import com.fiiadoapp.fiado.core.data.sincronizacion.Vinculacion
import com.fiiadoapp.fiado.domain.CuentaAbierta
import com.fiiadoapp.fiado.domain.ErrorCuenta
import com.fiiadoapp.fiado.domain.RepositorioCuenta
import com.fiiadoapp.fiado.domain.Resultado
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

class RepositorioCuentaRemoto(
    private val api: ApiCuentas,
    private val almacen: AlmacenSesion,
    private val vinculacion: Vinculacion,
) : RepositorioCuenta {
    override fun observar(): Flow<CuentaAbierta?> = almacen.sesion.map { it?.let { s -> CuentaAbierta(s.correo, s.nombreNegocio) } }

    override fun observarSesionTerminada(): Flow<Boolean> = almacen.terminadaDesdeFuera

    override suspend fun descartarAvisoSesionTerminada() = almacen.descartarAviso()

    override suspend fun comprobarSesion(): Resultado<CuentaAbierta, ErrorCuenta> =
        when (val resultado = llamar { api.yo() }) {
            is Resultado.Fallo -> {
                resultado
            }

            is Resultado.Exito -> {
                almacen.actual()?.let { almacen.guardar(it.copy(nombreNegocio = resultado.valor.nombreNegocio)) }
                Resultado.Exito(CuentaAbierta(resultado.valor.correo, resultado.valor.nombreNegocio))
            }
        }

    override suspend fun registrar(
        correo: String,
        contrasena: String,
        nombreNegocio: String,
    ) = llamar { api.registrar(RegistroDto(correo, contrasena, nombreNegocio)) }.sinValor()

    override suspend fun verificarCorreo(
        correo: String,
        codigo: String,
    ) = llamar { api.verificar(VerificacionDto(correo, codigo)) }

    override suspend fun reenviarCodigo(correo: String) = llamar { api.reenviarCodigo(CorreoDto(correo)) }

    override suspend fun iniciarSesion(
        correo: String,
        contrasena: String,
        ceder: Boolean,
    ): Resultado<CuentaAbierta, ErrorCuenta> {
        // Se comprueba antes de llamar a la API: iniciar sesión podría desplazar al otro teléfono de esa cuenta.
        val vinculado = vinculacion.correoVinculado()
        if (vinculado != null && vinculado != correo.trim().lowercase()) return Resultado.Fallo(ErrorCuenta.OtraCuenta(vinculado))
        return when (val resultado = llamar { api.iniciarSesion(InicioDeSesionDto(correo, contrasena, ceder)) }) {
            is Resultado.Fallo -> {
                resultado
            }

            is Resultado.Exito -> {
                val abierta = resultado.valor
                almacen.guardar(
                    SesionGuardada(abierta.tokenAcceso, abierta.tokenRefresco, abierta.cuenta.correo, abierta.cuenta.nombreNegocio),
                )
                vinculacion.prepararRestauracion(abierta.cuenta.correo)
                Resultado.Exito(CuentaAbierta(abierta.cuenta.correo, abierta.cuenta.nombreNegocio))
            }
        }
    }

    override suspend fun cerrarSesion() {
        llamar { api.cerrarSesion() }
        almacen.borrar(terminadaDesdeFuera = false)
    }

    override suspend fun solicitarRecuperacion(correo: String) = llamar { api.solicitarRecuperacion(CorreoDto(correo)) }

    override suspend fun restablecerContrasena(
        correo: String,
        codigo: String,
        contrasenaNueva: String,
    ) = llamar { api.restablecer(RestablecimientoDto(correo, codigo, contrasenaNueva)) }

    /** Traduce las fallas de red y las respuestas de error de la API a errores del dominio. */
    private suspend fun <T> llamar(peticion: suspend () -> T): Resultado<T, ErrorCuenta> =
        try {
            Resultado.Exito(peticion())
        } catch (error: HttpException) {
            Resultado.Fallo(errorDeLaApi(error))
        } catch (_: IOException) {
            Resultado.Fallo(ErrorCuenta.SinConexion)
        }

    private fun errorDeLaApi(error: HttpException): ErrorCuenta {
        val cuerpo =
            error
                .response()
                ?.errorBody()
                ?.string()
                .orEmpty()
        val dto = runCatching { formato.decodeFromString<ErrorDto>(cuerpo) }.getOrNull()
        return ErrorCuenta.DeLaApi(dto?.codigo ?: "http_${error.code()}", dto?.mensaje ?: "Algo salió mal. Intenta de nuevo.")
    }

    private fun <T> Resultado<T, ErrorCuenta>.sinValor(): Resultado<Unit, ErrorCuenta> =
        when (this) {
            is Resultado.Exito -> Resultado.Exito(Unit)
            is Resultado.Fallo -> this
        }

    private companion object {
        val formato = Json { ignoreUnknownKeys = true }
    }
}
