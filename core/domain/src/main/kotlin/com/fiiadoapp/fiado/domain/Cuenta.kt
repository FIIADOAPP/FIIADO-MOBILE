package com.fiiadoapp.fiado.domain

import kotlinx.coroutines.flow.Flow

/** Cuenta con la sesión abierta en este teléfono (ADR-0005). */
data class CuentaAbierta(
    val correo: String,
    val nombreNegocio: String,
)

/** Lo que puede fallar al hablar con la API de cuentas. */
sealed interface ErrorCuenta {
    data object SinConexion : ErrorCuenta

    /** Este teléfono guarda la libreta de [correoVinculado]: no se mezcla con la de otra cuenta (ADR-0003). */
    data class OtraCuenta(
        val correoVinculado: String,
    ) : ErrorCuenta

    /** [codigo] es estable (ADR-0002 de FIIADO-API); [mensaje] viene listo para mostrar. */
    data class DeLaApi(
        val codigo: String,
        val mensaje: String,
    ) : ErrorCuenta {
        val pideCederElDispositivo: Boolean get() = codigo == "sesion_activa_en_otro_dispositivo"

        val faltaVerificarElCorreo: Boolean get() = codigo == "correo_sin_verificar"
    }
}

/** La cuenta es opcional: la libreta funciona igual sin ella, y la sesión se mantiene sin conexión. */
interface RepositorioCuenta {
    /** `null` sin cuenta. Se lee del teléfono: no necesita conexión. */
    fun observar(): Flow<CuentaAbierta?>

    /** `true` si la API cerró la sesión desde fuera (otro teléfono, contraseña restablecida). */
    fun observarSesionTerminada(): Flow<Boolean>

    suspend fun descartarAvisoSesionTerminada()

    /** Pregunta a la API si la sesión sigue abierta; si se cerró desde fuera, la borra y avisa. */
    suspend fun comprobarSesion(): Resultado<CuentaAbierta, ErrorCuenta>

    suspend fun registrar(
        correo: String,
        contrasena: String,
        nombreNegocio: String,
    ): Resultado<Unit, ErrorCuenta>

    suspend fun verificarCorreo(
        correo: String,
        codigo: String,
    ): Resultado<Unit, ErrorCuenta>

    suspend fun reenviarCodigo(correo: String): Resultado<Unit, ErrorCuenta>

    /** Con otro teléfono activo falla con `sesion_activa_en_otro_dispositivo`; se repite con [ceder]. */
    suspend fun iniciarSesion(
        correo: String,
        contrasena: String,
        ceder: Boolean,
    ): Resultado<CuentaAbierta, ErrorCuenta>

    /** Siempre cierra en el teléfono; avisar al servidor es un mejor esfuerzo. Los datos locales se quedan. */
    suspend fun cerrarSesion()

    suspend fun solicitarRecuperacion(correo: String): Resultado<Unit, ErrorCuenta>

    suspend fun restablecerContrasena(
        correo: String,
        codigo: String,
        contrasenaNueva: String,
    ): Resultado<Unit, ErrorCuenta>
}
