package com.fiiadoapp.fiado.feature.movimientos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiiadoapp.fiado.domain.Cliente
import com.fiiadoapp.fiado.domain.Dinero
import com.fiiadoapp.fiado.domain.ErrorMovimiento
import com.fiiadoapp.fiado.domain.GeneradorDeIds
import com.fiiadoapp.fiado.domain.Movimiento
import com.fiiadoapp.fiado.domain.RepositorioClientes
import com.fiiadoapp.fiado.domain.RepositorioMovimientos
import com.fiiadoapp.fiado.domain.Resultado
import com.fiiadoapp.fiado.domain.TipoMovimiento
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
import java.time.LocalDate
import javax.inject.Inject
import kotlin.uuid.Uuid

data class EstadoRegistro(
    val tipo: TipoMovimiento,
    val hoy: LocalDate,
    val fecha: LocalDate = hoy,
    val cargando: Boolean = true,
    val nombreCliente: String = "",
    val saldo: Dinero = Dinero.CERO,
    val monto: String = "",
    val nota: String = "",
    /** El monto no se pudo leer o es cero. */
    val montoInvalido: Boolean = false,
    val error: ErrorMovimiento? = null,
    val guardando: Boolean = false,
    /** Id del movimiento guardado; la pantalla debe cerrarse. */
    val registrado: Uuid? = null,
)

@HiltViewModel
class RegistrarMovimientoViewModel
    @Inject
    constructor(
        estadoGuardado: SavedStateHandle,
        private val clientes: RepositorioClientes,
        private val movimientos: RepositorioMovimientos,
        private val generadorDeIds: GeneradorDeIds,
        private val reloj: Clock,
    ) : ViewModel() {
        private val clienteId = Uuid.parse(checkNotNull(estadoGuardado.get<String>(RegistrarMovimiento::clienteId.name)))
        private var cliente: Cliente? = null

        private val _estado =
            MutableStateFlow(
                EstadoRegistro(
                    tipo = TipoMovimiento.valueOf(checkNotNull(estadoGuardado.get<String>(RegistrarMovimiento::tipo.name))),
                    hoy = LocalDate.now(reloj),
                ),
            )
        val estado: StateFlow<EstadoRegistro> = _estado.asStateFlow()

        init {
            viewModelScope.launch {
                val encontrado = checkNotNull(clientes.observar(clienteId).first()) { "Cliente inexistente: $clienteId" }
                val saldo = movimientos.observarDeCliente(clienteId).first().saldo()
                cliente = encontrado
                _estado.update { it.copy(cargando = false, nombreCliente = encontrado.nombre, saldo = saldo) }
            }
        }

        fun cambiarMonto(monto: String) = _estado.update { it.copy(monto = monto, montoInvalido = false) }

        fun cambiarNota(nota: String) = _estado.update { it.copy(nota = nota, error = null) }

        fun cambiarFecha(fecha: LocalDate) = _estado.update { it.copy(fecha = fecha, error = null) }

        fun guardar() {
            val actual = _estado.value
            val cliente = cliente ?: return
            if (actual.guardando) return
            val monto = Dinero.desdeTexto(actual.monto)
            if (monto == null) {
                _estado.update { it.copy(montoInvalido = true) }
                return
            }
            val resultado =
                Movimiento.registrar(
                    id = generadorDeIds.nuevo(),
                    cliente = cliente,
                    tipo = actual.tipo,
                    monto = monto,
                    nota = actual.nota,
                    fecha = actual.fecha,
                    hoy = actual.hoy,
                    ahora = Instant.now(reloj),
                )
            when (resultado) {
                is Resultado.Fallo -> {
                    _estado.update {
                        if (resultado.error ==
                            ErrorMovimiento.MontoNoPositivo
                        ) {
                            it.copy(montoInvalido = true)
                        } else {
                            it.copy(error = resultado.error)
                        }
                    }
                }

                is Resultado.Exito -> {
                    viewModelScope.launch {
                        _estado.update { it.copy(guardando = true) }
                        movimientos.guardar(resultado.valor)
                        _estado.update { it.copy(guardando = false, registrado = resultado.valor.id) }
                    }
                }
            }
        }
    }
