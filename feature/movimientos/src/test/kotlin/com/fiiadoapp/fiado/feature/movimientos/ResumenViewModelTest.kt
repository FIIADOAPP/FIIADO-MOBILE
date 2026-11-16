package com.fiiadoapp.fiado.feature.movimientos

import com.fiiadoapp.fiado.core.testing.LibretaFalsa
import com.fiiadoapp.fiado.core.testing.ReglaDispatcherPrincipal
import com.fiiadoapp.fiado.domain.Dinero
import com.fiiadoapp.fiado.domain.MetodoCobro
import com.fiiadoapp.fiado.domain.Movimiento
import com.fiiadoapp.fiado.domain.TipoMovimiento.FIADO
import com.fiiadoapp.fiado.domain.TipoMovimiento.PAGO
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class ResumenViewModelTest {
    @get:Rule
    val dispatcher = ReglaDispatcherPrincipal()

    /** 15 de noviembre a las 22:00 en Lima; en UTC ya es 16. */
    private val ahora = Instant.parse("2026-11-16T03:00:00Z")
    private val hoy = LocalDate.of(2026, 11, 15)
    private val libreta = LibretaFalsa()
    private val rosa = Uuid.random()

    private fun movimiento(
        tipo: com.fiiadoapp.fiado.domain.TipoMovimiento,
        centimos: Long,
        fecha: LocalDate = hoy,
        metodo: MetodoCobro? = null,
    ) = Movimiento(Uuid.random(), rosa, tipo, Dinero(centimos), null, fecha, ahora, null, metodo?.id)

    @Test
    fun `resume lo de hoy y del mes en hora de Lima, con lo cobrado por metodo`() =
        runTest {
            libreta.metodos.value += MetodoCobro.PLIN.id to MetodoCobro.PLIN.copy(archivadoEn = ahora)
            libreta.movimientos.value =
                listOf(
                    movimiento(FIADO, 5000),
                    movimiento(PAGO, 1000, metodo = MetodoCobro.EFECTIVO),
                    movimiento(PAGO, 1500, metodo = MetodoCobro.YAPE),
                    movimiento(PAGO, 2000, fecha = LocalDate.of(2026, 11, 3), metodo = MetodoCobro.PLIN),
                ).associateBy { it.id }
            val viewModel =
                ResumenViewModel(
                    libreta.repositorioMovimientos,
                    libreta.repositorioMetodosCobro,
                    Clock.fixed(ahora, ZoneId.of("America/Lima")),
                )
            viewModel.estado.launchIn(backgroundScope + UnconfinedTestDispatcher(testScheduler))

            val estado = viewModel.estado.value

            assertEquals(hoy, estado?.hoy)
            assertEquals(Dinero(500), estado?.porCobrar)
            assertEquals(
                Periodo(
                    Dinero(5000),
                    Dinero(2500),
                    listOf(CobradoPorMetodo("Yape", Dinero(1500)), CobradoPorMetodo("Efectivo", Dinero(1000))),
                ),
                estado?.delDia,
            )
            assertEquals(
                listOf(
                    CobradoPorMetodo("Plin", Dinero(2000)),
                    CobradoPorMetodo("Yape", Dinero(1500)),
                    CobradoPorMetodo("Efectivo", Dinero(1000)),
                ),
                estado?.delMes?.porMetodo,
            )
        }
}
