package com.fiiadoapp.fiado.feature.clientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiiadoapp.fiado.domain.ClienteConSaldo
import com.fiiadoapp.fiado.domain.Dinero
import com.fiiadoapp.fiado.domain.RepositorioClientes
import com.fiiadoapp.fiado.domain.RepositorioMovimientos
import com.fiiadoapp.fiado.domain.TipoMovimiento
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlin.uuid.Uuid

/** Aviso tras registrar un movimiento, con la opción de deshacerlo. */
data class AvisoRegistro(
    val movimientoId: Uuid,
    val tipo: TipoMovimiento,
    val monto: Dinero,
    val nombreCliente: String,
)

@HiltViewModel
class ListaClientesViewModel
    @Inject
    constructor(
        private val clientes: RepositorioClientes,
        private val movimientos: RepositorioMovimientos,
        private val reloj: Clock,
    ) : ViewModel() {
        private val _busqueda = MutableStateFlow("")
        val busqueda: StateFlow<String> = _busqueda.asStateFlow()

        /** `null` mientras se carga la primera vez. */
        @OptIn(ExperimentalCoroutinesApi::class)
        val lista: StateFlow<List<ClienteConSaldo>?> =
            _busqueda
                .flatMapLatest { clientes.observarActivos(it) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        private val _aviso = MutableStateFlow<AvisoRegistro?>(null)
        val aviso: StateFlow<AvisoRegistro?> = _aviso.asStateFlow()

        fun buscar(texto: String) {
            _busqueda.value = texto
        }

        fun avisarRegistro(movimientoId: Uuid) {
            viewModelScope.launch {
                val movimiento = movimientos.obtener(movimientoId) ?: return@launch
                val cliente = clientes.observar(movimiento.clienteId).first() ?: return@launch
                _aviso.value = AvisoRegistro(movimiento.id, movimiento.tipo, movimiento.monto, cliente.nombre)
            }
        }

        /** Anula el movimiento del aviso: queda en el historial, pero deja de contar en el saldo. */
        fun deshacer() {
            val aviso = _aviso.value ?: return
            _aviso.value = null
            viewModelScope.launch {
                val movimiento = movimientos.obtener(aviso.movimientoId) ?: return@launch
                movimientos.guardar(movimiento.anular(Instant.now(reloj)))
            }
        }

        fun descartarAviso() {
            _aviso.value = null
        }
    }
