package com.fiiadoapp.fiado.feature.movimientos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiiadoapp.fiado.domain.Cliente
import com.fiiadoapp.fiado.domain.Dinero
import com.fiiadoapp.fiado.domain.MetodoCobro
import com.fiiadoapp.fiado.domain.Movimiento
import com.fiiadoapp.fiado.domain.Prorroga
import com.fiiadoapp.fiado.domain.Puntualidad
import com.fiiadoapp.fiado.domain.PuntualidadDelNegocio
import com.fiiadoapp.fiado.domain.RepositorioClientes
import com.fiiadoapp.fiado.domain.RepositorioMetodosCobro
import com.fiiadoapp.fiado.domain.RepositorioMovimientos
import com.fiiadoapp.fiado.domain.RepositorioProrrogas
import com.fiiadoapp.fiado.domain.Resumen
import com.fiiadoapp.fiado.domain.TipoMovimiento
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlin.uuid.Uuid

data class CobradoPorMetodo(
    val nombre: String,
    val monto: Dinero,
)

data class Periodo(
    val fiado: Dinero,
    val cobrado: Dinero,
    /** Del método que más cobró al que menos. */
    val porMetodo: List<CobradoPorMetodo>,
)

data class ClienteEnCuidado(
    val id: Uuid,
    val nombre: String,
    val puntualidad: Puntualidad,
)

/** Cómo le pagan al negocio (ADR-0007), con nombre y números de cada cliente en Cuidado. */
data class ComoTePagan(
    val negocio: PuntualidadDelNegocio,
    val enCuidado: List<ClienteEnCuidado>,
)

data class EstadoResumen(
    val hoy: LocalDate,
    val porCobrar: Dinero,
    val delDia: Periodo,
    val delMes: Periodo,
    /** `null` si ningún cliente activo tiene fiados. */
    val comoTePagan: ComoTePagan? = null,
)

@HiltViewModel
class ResumenViewModel
    @Inject
    constructor(
        movimientos: RepositorioMovimientos,
        metodosCobro: RepositorioMetodosCobro,
        clientes: RepositorioClientes,
        prorrogas: RepositorioProrrogas,
        private val reloj: Clock,
    ) : ViewModel() {
        /** `null` mientras se carga la primera vez. */
        val estado: StateFlow<EstadoResumen?> =
            combine(
                movimientos.observarTodos(),
                metodosCobro.observarTodos(),
                clientes.observarActivos(""),
                prorrogas.observarTodas(),
            ) { todos, metodos, activos, todasLasProrrogas ->
                val hoy = LocalDate.now(reloj)
                val resumen = Resumen.calcular(todos, hoy)
                EstadoResumen(
                    hoy = hoy,
                    porCobrar = resumen.porCobrar,
                    delDia = Periodo(resumen.fiadoHoy, resumen.cobradoHoy, desglose(resumen.cobradoHoyPorMetodo, metodos)),
                    delMes = Periodo(resumen.fiadoMes, resumen.cobradoMes, desglose(resumen.cobradoMesPorMetodo, metodos)),
                    comoTePagan = comoTePagan(todos, todasLasProrrogas, activos.map { it.cliente }, hoy),
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        /** Solo clientes activos con algún fiado vigente. */
        private fun comoTePagan(
            todos: List<Movimiento>,
            todasLasProrrogas: List<Prorroga>,
            activos: List<Cliente>,
            hoy: LocalDate,
        ): ComoTePagan? {
            val porCliente = todos.groupBy { it.clienteId }
            val puntualidades =
                activos
                    .filter { cliente -> porCliente[cliente.id].orEmpty().any { it.tipo == TipoMovimiento.FIADO && it.estaVigente } }
                    .associate { it.id to Puntualidad.calcular(porCliente.getValue(it.id), todasLasProrrogas, hoy, reloj.zone) }
            if (puntualidades.isEmpty()) return null
            val nombres = activos.associate { it.id to it.nombre }
            val negocio = PuntualidadDelNegocio.de(puntualidades)
            return ComoTePagan(negocio, negocio.enCuidado.map { ClienteEnCuidado(it, nombres.getValue(it), puntualidades.getValue(it)) })
        }

        private fun desglose(
            porMetodo: Map<Uuid, Dinero>,
            metodos: List<MetodoCobro>,
        ): List<CobradoPorMetodo> {
            val nombres = metodos.associate { it.id to it.nombre }
            return porMetodo
                .map { (id, monto) -> CobradoPorMetodo(nombres.getValue(id), monto) }
                .sortedByDescending { it.monto }
        }
    }
