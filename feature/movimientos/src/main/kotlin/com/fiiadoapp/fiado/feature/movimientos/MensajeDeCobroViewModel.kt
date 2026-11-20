package com.fiiadoapp.fiado.feature.movimientos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiiadoapp.fiado.domain.Dinero
import com.fiiadoapp.fiado.domain.ErrorPlantilla
import com.fiiadoapp.fiado.domain.PlantillaRecordatorio
import com.fiiadoapp.fiado.domain.RepositorioCuenta
import com.fiiadoapp.fiado.domain.RepositorioPlantillaRecordatorio
import com.fiiadoapp.fiado.domain.Resultado
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** [vistaPrevia] muestra el mensaje con un cliente de ejemplo; `null` mientras el texto no sea válido. */
data class EstadoMensajeDeCobro(
    val texto: String = "",
    val vistaPrevia: String? = null,
    val error: ErrorPlantilla? = null,
    val guardada: Boolean = false,
)

@HiltViewModel
class MensajeDeCobroViewModel
    @Inject
    constructor(
        private val plantillas: RepositorioPlantillaRecordatorio,
        private val cuenta: RepositorioCuenta,
    ) : ViewModel() {
        private val _estado = MutableStateFlow(EstadoMensajeDeCobro())
        val estado: StateFlow<EstadoMensajeDeCobro> = _estado.asStateFlow()
        private var negocio: String? = null

        init {
            viewModelScope.launch {
                negocio = cuenta.observar().first()?.nombreNegocio
                cambiar(plantillas.observar().first().texto)
            }
        }

        fun cambiar(texto: String) =
            _estado.update {
                val plantilla = (PlantillaRecordatorio.desdeTexto(texto) as? Resultado.Exito)?.valor
                it.copy(texto = texto, vistaPrevia = plantilla?.mensaje(CLIENTE_DE_EJEMPLO, SALDO_DE_EJEMPLO, negocio), error = null)
            }

        fun restablecer() = cambiar(PlantillaRecordatorio.POR_DEFECTO.texto)

        fun guardar() {
            when (val resultado = PlantillaRecordatorio.desdeTexto(_estado.value.texto)) {
                is Resultado.Fallo -> {
                    _estado.update { it.copy(error = resultado.error) }
                }

                is Resultado.Exito -> {
                    viewModelScope.launch {
                        if (resultado.valor == PlantillaRecordatorio.POR_DEFECTO) {
                            plantillas.restablecer()
                        } else {
                            plantillas.guardar(resultado.valor)
                        }
                        _estado.update { it.copy(guardada = true) }
                    }
                }
            }
        }

        private companion object {
            const val CLIENTE_DE_EJEMPLO = "Juana"
            val SALDO_DE_EJEMPLO = Dinero(1250)
        }
    }
