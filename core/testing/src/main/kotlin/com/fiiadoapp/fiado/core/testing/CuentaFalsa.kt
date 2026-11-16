package com.fiiadoapp.fiado.core.testing

import com.fiiadoapp.fiado.domain.CuentaAbierta
import com.fiiadoapp.fiado.domain.ErrorCuenta
import com.fiiadoapp.fiado.domain.RepositorioCuenta
import com.fiiadoapp.fiado.domain.Resultado
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Cuenta en memoria. [proximoError] hace fallar la siguiente llamada a la API; [llamadas] registra las hechas. */
class CuentaFalsa : RepositorioCuenta {
    val cuenta = MutableStateFlow<CuentaAbierta?>(null)
    val sesionTerminada = MutableStateFlow(false)
    var proximoError: ErrorCuenta? = null
    val llamadas = mutableListOf<String>()

    override fun observar(): Flow<CuentaAbierta?> = cuenta

    override fun observarSesionTerminada(): Flow<Boolean> = sesionTerminada

    override suspend fun descartarAvisoSesionTerminada() {
        sesionTerminada.value = false
    }

    override suspend fun comprobarSesion(): Resultado<CuentaAbierta, ErrorCuenta> =
        responder("comprobarSesion") { checkNotNull(cuenta.value) }

    override suspend fun registrar(
        correo: String,
        contrasena: String,
        nombreNegocio: String,
    ) = responder("registrar $correo $nombreNegocio") {}

    override suspend fun verificarCorreo(
        correo: String,
        codigo: String,
    ) = responder("verificar $correo $codigo") {}

    override suspend fun reenviarCodigo(correo: String) = responder("reenviar $correo") {}

    override suspend fun iniciarSesion(
        correo: String,
        contrasena: String,
        ceder: Boolean,
    ) = responder("iniciar $correo ceder=$ceder") {
        CuentaAbierta(correo, "Bodega Doña Rosa").also { cuenta.value = it }
    }

    override suspend fun cerrarSesion() {
        llamadas += "cerrarSesion"
        cuenta.value = null
    }

    override suspend fun solicitarRecuperacion(correo: String) = responder("solicitarRecuperacion $correo") {}

    override suspend fun restablecerContrasena(
        correo: String,
        codigo: String,
        contrasenaNueva: String,
    ) = responder("restablecer $correo $codigo") {}

    private fun <T> responder(
        llamada: String,
        exito: () -> T,
    ): Resultado<T, ErrorCuenta> {
        llamadas += llamada
        val error = proximoError ?: return Resultado.Exito(exito())
        proximoError = null
        return Resultado.Fallo(error)
    }
}
