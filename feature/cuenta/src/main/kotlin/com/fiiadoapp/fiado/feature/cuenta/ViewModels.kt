package com.fiiadoapp.fiado.feature.cuenta

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiiadoapp.fiado.domain.CuentaAbierta
import com.fiiadoapp.fiado.domain.ErrorCuenta
import com.fiiadoapp.fiado.domain.EstadoRespaldo
import com.fiiadoapp.fiado.domain.RepositorioCuenta
import com.fiiadoapp.fiado.domain.RepositorioRespaldo
import com.fiiadoapp.fiado.domain.Resultado
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Segundos que la API exige entre dos envíos de código (ADR-0002 de FIIADO-API). */
const val ESPERA_ENTRE_CODIGOS_S = 60

sealed interface EstadoCuenta {
    data object Cargando : EstadoCuenta

    data object SinCuenta : EstadoCuenta

    data class Abierta(
        val cuenta: CuentaAbierta,
        val respaldo: EstadoRespaldo,
    ) : EstadoCuenta
}

@HiltViewModel
class CuentaViewModel
    @Inject
    constructor(
        private val repositorio: RepositorioCuenta,
        respaldo: RepositorioRespaldo,
    ) : ViewModel() {
        val estado: StateFlow<EstadoCuenta> =
            combine(repositorio.observar(), respaldo.observar()) { cuenta, estadoRespaldo ->
                if (cuenta == null) EstadoCuenta.SinCuenta else EstadoCuenta.Abierta(cuenta, estadoRespaldo)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoCuenta.Cargando)

        init {
            // Si la cuenta se abrió en otro teléfono, se descubre al entrar aquí y no recién al sincronizar.
            viewModelScope.launch { if (repositorio.observar().first() != null) repositorio.comprobarSesion() }
        }

        fun cerrarSesion() {
            viewModelScope.launch { repositorio.cerrarSesion() }
        }
    }

data class EstadoCrearCuenta(
    val correo: String = "",
    val contrasena: String = "",
    val nombreNegocio: String = "",
    val enviando: Boolean = false,
    val error: ErrorCuenta? = null,
    /** Correo de la cuenta creada: la pantalla pasa a verificarlo. */
    val creada: String? = null,
)

@HiltViewModel
class CrearCuentaViewModel
    @Inject
    constructor(
        private val repositorio: RepositorioCuenta,
    ) : ViewModel() {
        private val _estado = MutableStateFlow(EstadoCrearCuenta())
        val estado: StateFlow<EstadoCrearCuenta> = _estado.asStateFlow()

        fun cambiarCorreo(valor: String) = _estado.update { it.copy(correo = valor, error = null) }

        fun cambiarContrasena(valor: String) = _estado.update { it.copy(contrasena = valor, error = null) }

        fun cambiarNombreNegocio(valor: String) = _estado.update { it.copy(nombreNegocio = valor, error = null) }

        fun crear() {
            val actual = _estado.value
            if (actual.enviando) return
            _estado.update { it.copy(enviando = true, error = null) }
            viewModelScope.launch {
                val resultado = repositorio.registrar(actual.correo.trim(), actual.contrasena, actual.nombreNegocio)
                _estado.update {
                    when (resultado) {
                        is Resultado.Exito -> it.copy(enviando = false, creada = actual.correo.trim())
                        is Resultado.Fallo -> it.copy(enviando = false, error = resultado.error)
                    }
                }
            }
        }
    }

data class EstadoVerificacion(
    val correo: String,
    val codigo: String = "",
    val enviando: Boolean = false,
    val error: ErrorCuenta? = null,
    /** Segundos que faltan para poder pedir otro código. */
    val esperaReenvio: Int = ESPERA_ENTRE_CODIGOS_S,
    val reenviado: Boolean = false,
    val verificado: Boolean = false,
)

@HiltViewModel
class VerificarCorreoViewModel
    @Inject
    constructor(
        estadoGuardado: SavedStateHandle,
        private val repositorio: RepositorioCuenta,
    ) : ViewModel() {
        private val _estado = MutableStateFlow(EstadoVerificacion(correo = checkNotNull(estadoGuardado["correo"])))
        val estado: StateFlow<EstadoVerificacion> = _estado.asStateFlow()
        private var cuentaRegresiva: Job? = null

        init {
            iniciarCuentaRegresiva()
        }

        fun cambiarCodigo(valor: String) = _estado.update { it.copy(codigo = valor.filter(Char::isDigit).take(6), error = null) }

        fun verificar() {
            val actual = _estado.value
            if (actual.enviando) return
            _estado.update { it.copy(enviando = true, error = null) }
            viewModelScope.launch {
                val resultado = repositorio.verificarCorreo(actual.correo, actual.codigo)
                _estado.update {
                    when (resultado) {
                        is Resultado.Exito -> it.copy(enviando = false, verificado = true)
                        is Resultado.Fallo -> it.copy(enviando = false, error = resultado.error)
                    }
                }
            }
        }

        fun reenviar() {
            if (_estado.value.esperaReenvio > 0) return
            viewModelScope.launch {
                when (val resultado = repositorio.reenviarCodigo(_estado.value.correo)) {
                    is Resultado.Exito -> {
                        _estado.update { it.copy(reenviado = true, codigo = "", error = null) }
                        iniciarCuentaRegresiva()
                    }

                    is Resultado.Fallo -> {
                        _estado.update { it.copy(error = resultado.error) }
                    }
                }
            }
        }

        private fun iniciarCuentaRegresiva() {
            cuentaRegresiva?.cancel()
            cuentaRegresiva =
                viewModelScope.launch {
                    for (restante in ESPERA_ENTRE_CODIGOS_S downTo 0) {
                        _estado.update { it.copy(esperaReenvio = restante) }
                        if (restante > 0) delay(1_000)
                    }
                }
        }
    }

data class EstadoInicioSesion(
    val correo: String,
    val contrasena: String = "",
    val enviando: Boolean = false,
    val error: ErrorCuenta? = null,
    /** La cuenta está activa en otro teléfono: se pide confirmar antes de usar este. */
    val pidiendoCeder: Boolean = false,
    /** El correo no está verificado: la pantalla pasa a verificarlo. */
    val faltaVerificar: Boolean = false,
    val iniciada: Boolean = false,
)

@HiltViewModel
class IniciarSesionViewModel
    @Inject
    constructor(
        estadoGuardado: SavedStateHandle,
        private val repositorio: RepositorioCuenta,
    ) : ViewModel() {
        private val _estado = MutableStateFlow(EstadoInicioSesion(correo = estadoGuardado["correo"] ?: ""))
        val estado: StateFlow<EstadoInicioSesion> = _estado.asStateFlow()

        fun cambiarCorreo(valor: String) = _estado.update { it.copy(correo = valor, error = null) }

        fun cambiarContrasena(valor: String) = _estado.update { it.copy(contrasena = valor, error = null) }

        fun iniciar() = enviar(ceder = false)

        /** El usuario aceptó que este teléfono reemplace al otro. */
        fun cederElOtroTelefono() = enviar(ceder = true)

        fun cancelarCeder() = _estado.update { it.copy(pidiendoCeder = false) }

        fun verificacionAtendida() = _estado.update { it.copy(faltaVerificar = false) }

        private fun enviar(ceder: Boolean) {
            val actual = _estado.value
            if (actual.enviando) return
            _estado.update { it.copy(enviando = true, error = null, pidiendoCeder = false) }
            viewModelScope.launch {
                val resultado = repositorio.iniciarSesion(actual.correo.trim(), actual.contrasena, ceder)
                _estado.update {
                    when (resultado) {
                        is Resultado.Exito -> {
                            it.copy(enviando = false, iniciada = true)
                        }

                        is Resultado.Fallo -> {
                            val error = resultado.error
                            when {
                                error is ErrorCuenta.DeLaApi && error.pideCederElDispositivo -> {
                                    it.copy(
                                        enviando = false,
                                        pidiendoCeder = true,
                                    )
                                }

                                error is ErrorCuenta.DeLaApi && error.faltaVerificarElCorreo -> {
                                    it.copy(
                                        enviando = false,
                                        faltaVerificar = true,
                                    )
                                }

                                else -> {
                                    it.copy(enviando = false, error = error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

data class EstadoRecuperacion(
    val correo: String,
    val codigo: String = "",
    val contrasenaNueva: String = "",
    /** `false`: se pide el correo; `true`: se ingresa el código y la contraseña nueva. */
    val codigoEnviado: Boolean = false,
    val enviando: Boolean = false,
    val error: ErrorCuenta? = null,
    val restablecida: Boolean = false,
)

@HiltViewModel
class RecuperarContrasenaViewModel
    @Inject
    constructor(
        estadoGuardado: SavedStateHandle,
        private val repositorio: RepositorioCuenta,
    ) : ViewModel() {
        private val _estado = MutableStateFlow(EstadoRecuperacion(correo = estadoGuardado["correo"] ?: ""))
        val estado: StateFlow<EstadoRecuperacion> = _estado.asStateFlow()

        fun cambiarCorreo(valor: String) = _estado.update { it.copy(correo = valor, error = null) }

        fun cambiarCodigo(valor: String) = _estado.update { it.copy(codigo = valor.filter(Char::isDigit).take(6), error = null) }

        fun cambiarContrasenaNueva(valor: String) = _estado.update { it.copy(contrasenaNueva = valor, error = null) }

        fun pedirCodigo() = llamar({ repositorio.solicitarRecuperacion(it.correo.trim()) }) { it.copy(codigoEnviado = true) }

        fun restablecer() =
            llamar({ repositorio.restablecerContrasena(it.correo.trim(), it.codigo, it.contrasenaNueva) }) {
                it.copy(restablecida = true)
            }

        private fun llamar(
            peticion: suspend (EstadoRecuperacion) -> Resultado<Unit, ErrorCuenta>,
            alTerminar: (EstadoRecuperacion) -> EstadoRecuperacion,
        ) {
            val actual = _estado.value
            if (actual.enviando) return
            _estado.update { it.copy(enviando = true, error = null) }
            viewModelScope.launch {
                val resultado = peticion(actual)
                _estado.update {
                    when (resultado) {
                        is Resultado.Exito -> alTerminar(it.copy(enviando = false))
                        is Resultado.Fallo -> it.copy(enviando = false, error = resultado.error)
                    }
                }
            }
        }
    }

/** Diálogo global: avisa una vez cuando la API cerró la sesión desde fuera. */
@HiltViewModel
class AvisoSesionViewModel
    @Inject
    constructor(
        private val repositorio: RepositorioCuenta,
    ) : ViewModel() {
        val mostrar: StateFlow<Boolean> =
            repositorio.observarSesionTerminada().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

        fun entendido() {
            viewModelScope.launch { repositorio.descartarAvisoSesionTerminada() }
        }
    }
