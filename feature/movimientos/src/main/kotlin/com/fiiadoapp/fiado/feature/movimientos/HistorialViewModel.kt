package com.fiiadoapp.fiado.feature.movimientos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiiadoapp.fiado.domain.Cliente
import com.fiiadoapp.fiado.domain.Dinero
import com.fiiadoapp.fiado.domain.GeneradorDeIds
import com.fiiadoapp.fiado.domain.Movimiento
import com.fiiadoapp.fiado.domain.Prorroga
import com.fiiadoapp.fiado.domain.RepositorioClientes
import com.fiiadoapp.fiado.domain.RepositorioCuenta
import com.fiiadoapp.fiado.domain.RepositorioMetodosCobro
import com.fiiadoapp.fiado.domain.RepositorioMovimientos
import com.fiiadoapp.fiado.domain.RepositorioPlantillaRecordatorio
import com.fiiadoapp.fiado.domain.RepositorioProrrogas
import com.fiiadoapp.fiado.domain.Resultado
import com.fiiadoapp.fiado.domain.paraWhatsApp
import com.fiiadoapp.fiado.domain.plazoVigente
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

/**
 * Un movimiento del historial, con el nombre de su método si es un pago y, si es un fiado con plazo,
 * su plazo vigente (ADR-0007).
 */
data class FilaHistorial(
    val movimiento: Movimiento,
    val nombreMetodo: String?,
    val plazo: LocalDate? = null,
    val prorrogado: Boolean = false,
    /** Fiado vigente con plazo de un cliente que aún debe: se le puede dar más plazo. */
    val puedeProrrogar: Boolean = false,
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
        private val prorrogas: RepositorioProrrogas,
        metodosCobro: RepositorioMetodosCobro,
        cuenta: RepositorioCuenta,
        plantillas: RepositorioPlantillaRecordatorio,
        private val generadorDeIds: GeneradorDeIds,
        private val reloj: Clock,
    ) : ViewModel() {
        private val clienteId = Uuid.parse(checkNotNull(estadoGuardado.get<String>(HistorialCliente::clienteId.name)))

        val hoy: LocalDate = LocalDate.now(reloj)

        /** `null` mientras se carga la primera vez. */
        val estado: StateFlow<EstadoHistorial?> =
            combine(
                clientes.observar(clienteId).filterNotNull(),
                combine(movimientos.observarDeCliente(clienteId), prorrogas.observarDeCliente(clienteId), ::Pair),
                metodosCobro.observarTodos(),
                cuenta.observar(),
                plantillas.observar(),
            ) { cliente, (deCliente, deProrrogas), metodos, abierta, plantilla ->
                prorrogasActuales = deProrrogas
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
                val filas =
                    deCliente.map { movimiento ->
                        val plazo = plazoVigente(movimiento, deProrrogas)
                        FilaHistorial(
                            movimiento = movimiento,
                            nombreMetodo = movimiento.metodoCobroId?.let(nombres::get),
                            plazo = plazo,
                            prorrogado = plazo != null && plazo != movimiento.vence,
                            puedeProrrogar = plazo != null && movimiento.estaVigente && saldo.esPositivo,
                        )
                    }
                EstadoHistorial(cliente, saldo, filas, recordatorio)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        private var prorrogasActuales: List<Prorroga> = emptyList()

        private val _opciones = MutableStateFlow<FilaHistorial?>(null)

        /** Fiado con plazo tocado: se elige entre dar más plazo o anularlo. */
        val opciones: StateFlow<FilaHistorial?> = _opciones.asStateFlow()

        private val _porProrrogar = MutableStateFlow<FilaHistorial?>(null)

        /** Fiado que espera la fecha nueva de su plazo. */
        val porProrrogar: StateFlow<FilaHistorial?> = _porProrrogar.asStateFlow()

        /** Un fiado que se puede prorrogar ofrece las dos acciones; el resto va directo a anular. */
        fun tocar(fila: FilaHistorial) {
            if (fila.puedeProrrogar) _opciones.value = fila else pedirAnulacion(fila.movimiento)
        }

        fun elegirProrrogar() {
            _porProrrogar.value = _opciones.value
            _opciones.value = null
        }

        fun elegirAnular() {
            _opciones.value?.let { pedirAnulacion(it.movimiento) }
            _opciones.value = null
        }

        fun cancelarOpciones() {
            _opciones.value = null
            _porProrrogar.value = null
        }

        fun darMasPlazo(vence: LocalDate) {
            val fila = _porProrrogar.value ?: return
            _porProrrogar.value = null
            val resultado = Prorroga.dar(generadorDeIds.nuevo(), fila.movimiento, prorrogasActuales, vence, Instant.now(reloj))
            if (resultado is Resultado.Exito) viewModelScope.launch { prorrogas.guardar(resultado.valor) }
        }

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
