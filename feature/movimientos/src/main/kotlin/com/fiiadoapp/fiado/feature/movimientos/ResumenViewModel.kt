package com.fiiadoapp.fiado.feature.movimientos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiiadoapp.fiado.domain.Dinero
import com.fiiadoapp.fiado.domain.MetodoCobro
import com.fiiadoapp.fiado.domain.RepositorioMetodosCobro
import com.fiiadoapp.fiado.domain.RepositorioMovimientos
import com.fiiadoapp.fiado.domain.Resumen
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

data class EstadoResumen(
    val hoy: LocalDate,
    val porCobrar: Dinero,
    val delDia: Periodo,
    val delMes: Periodo,
)

@HiltViewModel
class ResumenViewModel
    @Inject
    constructor(
        movimientos: RepositorioMovimientos,
        metodosCobro: RepositorioMetodosCobro,
        reloj: Clock,
    ) : ViewModel() {
        /** `null` mientras se carga la primera vez. */
        val estado: StateFlow<EstadoResumen?> =
            combine(movimientos.observarTodos(), metodosCobro.observarTodos()) { todos, metodos ->
                val hoy = LocalDate.now(reloj)
                val resumen = Resumen.calcular(todos, hoy)
                EstadoResumen(
                    hoy = hoy,
                    porCobrar = resumen.porCobrar,
                    delDia = Periodo(resumen.fiadoHoy, resumen.cobradoHoy, desglose(resumen.cobradoHoyPorMetodo, metodos)),
                    delMes = Periodo(resumen.fiadoMes, resumen.cobradoMes, desglose(resumen.cobradoMesPorMetodo, metodos)),
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
