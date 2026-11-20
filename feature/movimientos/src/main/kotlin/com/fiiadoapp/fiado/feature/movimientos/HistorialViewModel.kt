package com.fiiadoapp.fiado.feature.movimientos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiiadoapp.fiado.domain.Cliente
import com.fiiadoapp.fiado.domain.Dinero
import com.fiiadoapp.fiado.domain.Movimiento
import com.fiiadoapp.fiado.domain.RepositorioClientes
import com.fiiadoapp.fiado.domain.RepositorioCuenta
import com.fiiadoapp.fiado.domain.RepositorioMetodosCobro
import com.fiiadoapp.fiado.domain.RepositorioMovimientos
import com.fiiadoapp.fiado.domain.RepositorioPlantillaRecordatorio
import com.fiiadoapp.fiado.domain.paraWhatsApp
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

/** Lo que hace falta para recordarle el cobro; solo existe si el cliente debe. */
sealed interface Recordatorio {
    /** [celular] en el formato de `wa.me`; [mensaje] ya armado con la plantilla. */
    data class Listo(
        val celular: String,
        val mensaje: String,
    ) : Recordatorio

    data object SinCelular : Recordatorio
}

data class EstadoHistorial(
    val cliente: Cliente,
    val saldo: Dinero,
    val filas: List<FilaHistorial>,
    val recordatorio: Recordatorio? = null,
)

@HiltViewModel
class HistorialViewModel
    @Inject
    constructor(
        estadoGuardado: SavedStateHandle,
        clientes: RepositorioClientes,
        private val movimientos: RepositorioMovimientos,
        metodosCobro: RepositorioMetodosCobro,
        cuenta: RepositorioCuenta,
        plantillas: RepositorioPlantillaRecordatorio,
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
                cuenta.observar(),
                plantillas.observar(),
            ) { cliente, deCliente, metodos, abierta, plantilla ->
                val nombres = metodos.associate { it.id to it.nombre }
                val saldo = deCliente.saldo()
                val telefono = cliente.telefono
                val recordatorio =
                    when {
                        saldo.centimos <= 0 -> {
                            null
                        }

                        telefono == null -> {
                            Recordatorio.SinCelular
                        }

                        else -> {
                            Recordatorio.Listo(
                                telefono.paraWhatsApp,
                                plantilla.mensaje(cliente.nombre, saldo, abierta?.nombreNegocio),
                            )
                        }
                    }
                EstadoHistorial(cliente, saldo, deCliente.map { FilaHistorial(it, it.metodoCobroId?.let(nombres::get)) }, recordatorio)
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
