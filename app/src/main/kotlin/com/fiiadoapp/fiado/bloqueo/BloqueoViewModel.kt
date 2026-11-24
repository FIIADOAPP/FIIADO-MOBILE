package com.fiiadoapp.fiado.bloqueo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiiadoapp.fiado.domain.ReglaDeBloqueo
import com.fiiadoapp.fiado.domain.RepositorioBloqueo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class EstadoBloqueo {
    /** Aún no se sabe si el bloqueo está activo: no se muestra la libreta. */
    CARGANDO,
    BLOQUEADA,
    ABIERTA,
}

/**
 * Bloquea al abrir la app y al volver tras un minuto o más en segundo plano (ADR-0006). Vive con la
 * actividad: girar el teléfono no vuelve a pedir la huella; un proceso nuevo sí.
 */
@HiltViewModel
class BloqueoViewModel
    @Inject
    constructor(
        private val bloqueo: RepositorioBloqueo,
    ) : ViewModel() {
        private val _estado = MutableStateFlow(EstadoBloqueo.CARGANDO)
        val estado: StateFlow<EstadoBloqueo> = _estado.asStateFlow()

        val activo: StateFlow<Boolean> = bloqueo.observarActivo().stateIn(viewModelScope, SharingStarted.Eagerly, false)

        private val _desactivadoPorElTelefono = MutableStateFlow(false)

        /** `true` hasta que el usuario lea que el bloqueo se apagó porque el teléfono ya no tiene bloqueo de pantalla. */
        val desactivadoPorElTelefono: StateFlow<Boolean> = _desactivadoPorElTelefono.asStateFlow()

        private var salioEn: Long? = null

        init {
            viewModelScope.launch {
                _estado.value = if (bloqueo.observarActivo().first()) EstadoBloqueo.BLOQUEADA else EstadoBloqueo.ABIERTA
            }
        }

        /** [ahora] de un reloj monótono. Si ya está bloqueada (por ejemplo, mientras se pide el PIN), no cuenta. */
        fun alSalir(ahora: Long) {
            if (_estado.value == EstadoBloqueo.ABIERTA) salioEn = ahora
        }

        /**
         * Se decide en el acto, con lo que ya está en memoria: si esperara una corrutina, la libreta alcanzaría
         * a dibujarse un instante antes de bloquearse.
         */
        fun alVolver(ahora: Long) {
            val salida = salioEn
            salioEn = null
            if (activo.value && ReglaDeBloqueo.pedirAlVolver(salida, ahora)) {
                _estado.value = EstadoBloqueo.BLOQUEADA
            }
        }

        fun desbloquear() {
            _estado.value = EstadoBloqueo.ABIERTA
        }

        /** El teléfono quitó su bloqueo de pantalla: se apaga el de la app en lugar de dejarla inaccesible. */
        fun desactivarPorElTelefono() {
            viewModelScope.launch {
                bloqueo.cambiar(false)
                _desactivadoPorElTelefono.value = true
                _estado.value = EstadoBloqueo.ABIERTA
            }
        }

        fun descartarAviso() {
            _desactivadoPorElTelefono.value = false
        }
    }
