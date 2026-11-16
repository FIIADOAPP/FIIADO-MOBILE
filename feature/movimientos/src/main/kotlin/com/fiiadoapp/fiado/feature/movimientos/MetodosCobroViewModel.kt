package com.fiiadoapp.fiado.feature.movimientos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiiadoapp.fiado.domain.ErrorMetodoCobro
import com.fiiadoapp.fiado.domain.GeneradorDeIds
import com.fiiadoapp.fiado.domain.MetodoCobro
import com.fiiadoapp.fiado.domain.RepositorioMetodosCobro
import com.fiiadoapp.fiado.domain.Resultado
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/** Diálogo abierto para crear ([metodo] nulo) o editar un método. */
data class EdicionMetodo(
    val metodo: MetodoCobro?,
    val nombre: String,
    val error: ErrorMetodoCobro? = null,
)

@HiltViewModel
class MetodosCobroViewModel
    @Inject
    constructor(
        private val repositorio: RepositorioMetodosCobro,
        private val generadorDeIds: GeneradorDeIds,
        private val reloj: Clock,
    ) : ViewModel() {
        /** `null` mientras se carga la primera vez. */
        val metodos: StateFlow<List<MetodoCobro>?> =
            repositorio.observarActivos().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        private val _edicion = MutableStateFlow<EdicionMetodo?>(null)
        val edicion: StateFlow<EdicionMetodo?> = _edicion.asStateFlow()

        fun nuevo() {
            _edicion.value = EdicionMetodo(metodo = null, nombre = "")
        }

        fun editar(metodo: MetodoCobro) {
            _edicion.value = EdicionMetodo(metodo, metodo.nombre)
        }

        fun cambiarNombre(nombre: String) = _edicion.update { it?.copy(nombre = nombre, error = null) }

        fun cancelar() {
            _edicion.value = null
        }

        fun guardar() {
            val edicion = _edicion.value ?: return
            persistir { activos, ahora ->
                edicion.metodo?.renombrar(edicion.nombre, activos, ahora)
                    ?: MetodoCobro.crear(generadorDeIds.nuevo(), edicion.nombre, activos, ahora)
            }
        }

        fun archivar() {
            val metodo = _edicion.value?.metodo ?: return
            persistir { activos, ahora -> metodo.archivar(activos, ahora) }
        }

        /** Valida contra los métodos activos guardados, no contra los que muestra la pantalla. */
        private fun persistir(operacion: (List<MetodoCobro>, Instant) -> Resultado<MetodoCobro, ErrorMetodoCobro>) {
            viewModelScope.launch {
                when (val resultado = operacion(repositorio.observarActivos().first(), Instant.now(reloj))) {
                    is Resultado.Fallo -> {
                        _edicion.update { it?.copy(error = resultado.error) }
                    }

                    is Resultado.Exito -> {
                        repositorio.guardar(resultado.valor)
                        _edicion.value = null
                    }
                }
            }
        }
    }
