package com.fiiadoapp.fiado.feature.movimientos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiiadoapp.fiado.domain.Cliente
import com.fiiadoapp.fiado.domain.Dinero
import com.fiiadoapp.fiado.domain.Movimiento
import com.fiiadoapp.fiado.domain.RepositorioClientes
import com.fiiadoapp.fiado.domain.RepositorioMetodosCobro
import com.fiiadoapp.fiado.domain.RepositorioMovimientos
import com.fiiadoapp.fiado.domain.saldo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlin.uuid.Uuid

/** Un movimiento del historial, con el nombre de su método si es un pago. */
data class FilaHistorial(
    val movimiento: Movimiento,
    val nombreMetodo: String?,
)

data class EstadoHistorial(
    val cliente: Cliente,
    val saldo: Dinero,
    val filas: List<FilaHistorial>,
)

@HiltViewModel
class HistorialViewModel
    @Inject
    constructor(
        estadoGuardado: SavedStateHandle,
        clientes: RepositorioClientes,
        private val movimientos: RepositorioMovimientos,
        metodosCobro: RepositorioMetodosCobro,
        private val reloj: Clock,
    ) : ViewModel() {
        private val clienteId = Uuid.parse(checkNotNull(estadoGuardado.get<String>(HistorialCliente::clienteId.name)))

        val hoy: LocalDate = LocalDate.now(reloj)

        /** `null` mientras se carga la primera vez. */
        val estado: StateFlow<EstadoHistorial?> =
            combine(
                clientes.observar(clienteId).filterNotNull(),
                movimientos.observarDeCliente(clienteId),
                metodosCobro.observarTodos(),
            ) { cliente, deCliente, metodos ->
                val nombres = metodos.associate { it.id to it.nombre }
                EstadoHistorial(cliente, deCliente.saldo(), deCliente.map { FilaHistorial(it, it.metodoCobroId?.let(nombres::get)) })
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        private val _porAnular = MutableStateFlow<Movimiento?>(null)

        /** Movimiento que espera confirmación para anularse. */
        val porAnular: StateFlow<Movimiento?> = _porAnular.asStateFlow()

        fun pedirAnulacion(movimiento: Movimiento) {
            if (movimiento.estaVigente) _porAnular.value = movimiento
        }

        fun cancelarAnulacion() {
            _porAnular.value = null
        }

        fun confirmarAnulacion() {
            val movimiento = _porAnular.value ?: return
            _porAnular.value = null
            viewModelScope.launch { movimientos.guardar(movimiento.anular(Instant.now(reloj))) }
        }
    }
