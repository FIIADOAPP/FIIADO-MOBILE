package com.fiiadoapp.fiado.feature.clientes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiiadoapp.fiado.domain.Cliente
import com.fiiadoapp.fiado.domain.Dinero
import com.fiiadoapp.fiado.domain.ErrorCliente
import com.fiiadoapp.fiado.domain.GeneradorDeIds
import com.fiiadoapp.fiado.domain.RepositorioClientes
import com.fiiadoapp.fiado.domain.RepositorioMovimientos
import com.fiiadoapp.fiado.domain.Resultado
import com.fiiadoapp.fiado.domain.Telefono
import com.fiiadoapp.fiado.domain.saldo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlin.uuid.Uuid

data class EstadoFormularioCliente(
    val esEdicion: Boolean,
    val cargando: Boolean,
    val nombre: String = "",
    val telefono: String = "",
    val error: ErrorCliente? = null,
    val telefonoInvalido: Boolean = false,
    /** Saldo actual del cliente editado; decide si se puede archivar. */
    val saldo: Dinero = Dinero.CERO,
    val guardando: Boolean = false,
    /** La pantalla debe cerrarse: el cliente se guardó o se archivó. */
    val terminado: Boolean = false,
)

@HiltViewModel
class FormularioClienteViewModel
    @Inject
    constructor(
        estadoGuardado: SavedStateHandle,
        private val clientes: RepositorioClientes,
        private val movimientos: RepositorioMovimientos,
        private val generadorDeIds: GeneradorDeIds,
        private val reloj: Clock,
    ) : ViewModel() {
        private val clienteId: Uuid? = estadoGuardado.get<String>(FormularioCliente::clienteId.name)?.let(Uuid::parse)
        private var original: Cliente? = null

        private val _estado = MutableStateFlow(EstadoFormularioCliente(esEdicion = clienteId != null, cargando = clienteId != null))
        val estado: StateFlow<EstadoFormularioCliente> = _estado.asStateFlow()

        init {
            if (clienteId != null) {
                viewModelScope.launch {
                    val cliente = checkNotNull(clientes.observar(clienteId).first()) { "Cliente inexistente: $clienteId" }
                    val saldo = movimientos.observarDeCliente(clienteId).first().saldo()
                    original = cliente
                    _estado.update {
                        it.copy(
                            cargando = false,
                            nombre = cliente.nombre,
                            telefono = cliente.telefono?.formatear().orEmpty(),
                            saldo = saldo,
                        )
                    }
                }
            }
        }

        fun cambiarNombre(nombre: String) = _estado.update { it.copy(nombre = nombre, error = null) }

        fun cambiarTelefono(telefono: String) = _estado.update { it.copy(telefono = telefono, telefonoInvalido = false) }

        fun guardar() {
            val actual = _estado.value
            if (actual.cargando || actual.guardando) return
            val telefono = if (actual.telefono.isBlank()) null else Telefono.desdeTexto(actual.telefono)
            if (actual.telefono.isNotBlank() && telefono == null) {
                _estado.update { it.copy(telefonoInvalido = true) }
                return
            }
            val ahora = Instant.now(reloj)
            val resultado =
                original?.editar(actual.nombre, telefono, ahora)
                    ?: Cliente.crear(generadorDeIds.nuevo(), actual.nombre, telefono, ahora)
            persistir(resultado)
        }

        fun archivar() {
            val cliente = original ?: return
            if (_estado.value.guardando) return
            persistir(cliente.archivar(_estado.value.saldo, Instant.now(reloj)))
        }

        private fun persistir(resultado: Resultado<Cliente, ErrorCliente>) {
            when (resultado) {
                is Resultado.Fallo -> {
                    _estado.update { it.copy(error = resultado.error) }
                }

                is Resultado.Exito -> {
                    viewModelScope.launch {
                        _estado.update { it.copy(guardando = true) }
                        clientes.guardar(resultado.valor)
                        _estado.update { it.copy(guardando = false, terminado = true) }
                    }
                }
            }
        }
    }
