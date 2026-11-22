package com.fiiadoapp.fiado.feature.movimientos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiiadoapp.fiado.domain.AtajoDePlazo
import com.fiiadoapp.fiado.domain.Cliente
import com.fiiadoapp.fiado.domain.Dinero
import com.fiiadoapp.fiado.domain.ErrorMovimiento
import com.fiiadoapp.fiado.domain.GeneradorDeIds
import com.fiiadoapp.fiado.domain.MetodoCobro
import com.fiiadoapp.fiado.domain.Movimiento
import com.fiiadoapp.fiado.domain.NivelPuntualidad
import com.fiiadoapp.fiado.domain.Puntualidad
import com.fiiadoapp.fiado.domain.RepositorioClientes
import com.fiiadoapp.fiado.domain.RepositorioMetodosCobro
import com.fiiadoapp.fiado.domain.RepositorioMovimientos
import com.fiiadoapp.fiado.domain.RepositorioProrrogas
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
    /** Solo para un pago: métodos activos y el elegido, preseleccionado con el último usado (ADR-0004). */
    val metodos: List<MetodoCobro> = emptyList(),
    val metodoElegido: Uuid? = null,
    /** Solo para un fiado: un atajo, que se cuenta desde [fecha], u otra fecha elegida (ADR-0007). */
    val atajo: AtajoDePlazo? = null,
    val otraFecha: LocalDate? = null,
    /** Solo para un fiado: cómo paga el cliente, para avisar si está en Cuidado y sugerir un plazo (ADR-0007). */
    val puntualidad: Puntualidad? = null,
    /** El monto no se pudo leer o es cero. */
    val montoInvalido: Boolean = false,
    val error: ErrorMovimiento? = null,
    val guardando: Boolean = false,
    /** Id del movimiento guardado; la pantalla debe cerrarse. */
    val registrado: Uuid? = null,
) {
    /** `null` sin plazo. */
    val vence: LocalDate? get() = otraFecha ?: atajo?.desde(fecha)

    /** Aviso que no impide fiar: el cliente está en Cuidado. */
    val avisarCuidado: Boolean get() = puntualidad?.nivel == NivelPuntualidad.CUIDADO

    /** Días que suele tardar en pagar, para ofrecerlos como plazo. */
    val diasSugeridos: Int? get() = puntualidad?.diasSugeridos
}

@HiltViewModel
class RegistrarMovimientoViewModel
    @Inject
    constructor(
        estadoGuardado: SavedStateHandle,
        private val clientes: RepositorioClientes,
        private val movimientos: RepositorioMovimientos,
        private val metodosCobro: RepositorioMetodosCobro,
        private val prorrogas: RepositorioProrrogas,
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
                val deCliente = movimientos.observarDeCliente(clienteId).first()
                val esPago = _estado.value.tipo == TipoMovimiento.PAGO
                val metodos = if (esPago) metodosCobro.observarActivos().first() else emptyList()
                val puntualidad =
                    if (esPago) {
                        null
                    } else {
                        Puntualidad.calcular(deCliente, prorrogas.observarDeCliente(clienteId).first(), _estado.value.hoy, reloj.zone)
                    }
                val ultimo = movimientos.ultimoMetodoCobroUsado()
                cliente = encontrado
                _estado.update {
                    it.copy(
                        cargando = false,
                        nombreCliente = encontrado.nombre,
                        saldo = deCliente.saldo(),
                        metodos = metodos,
                        puntualidad = puntualidad,
                        metodoElegido = metodos.firstOrNull { metodo -> metodo.id == ultimo }?.id ?: metodos.firstOrNull()?.id,
                    )
                }
            }
        }

        fun cambiarMonto(monto: String) = _estado.update { it.copy(monto = monto, montoInvalido = false) }

        fun cambiarNota(nota: String) = _estado.update { it.copy(nota = nota, error = null) }

        fun cambiarFecha(fecha: LocalDate) = _estado.update { it.copy(fecha = fecha, error = null) }

        fun elegirMetodo(id: Uuid) = _estado.update { it.copy(metodoElegido = id, error = null) }

        /** `null` deja el fiado sin plazo. */
        fun elegirAtajo(atajo: AtajoDePlazo?) = _estado.update { it.copy(atajo = atajo, otraFecha = null, error = null) }

        fun elegirOtraFecha(fecha: LocalDate) = _estado.update { it.copy(atajo = null, otraFecha = fecha, error = null) }

        /** Plazo de los días que el cliente suele tardar, contados desde el día del fiado. */
        fun usarPlazoSugerido() {
            val dias = _estado.value.diasSugeridos ?: return
            elegirOtraFecha(_estado.value.fecha.plusDays(dias.toLong()))
        }

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
                    metodoCobro = actual.metodos.firstOrNull { it.id == actual.metodoElegido },
                    nota = actual.nota,
                    fecha = actual.fecha,
                    hoy = actual.hoy,
                    ahora = Instant.now(reloj),
                    vence = actual.vence,
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
