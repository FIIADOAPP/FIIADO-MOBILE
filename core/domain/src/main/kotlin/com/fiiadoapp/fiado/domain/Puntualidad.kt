package com.fiiadoapp.fiado.domain

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.uuid.Uuid

/** Nivel del semáforo de un cliente (ADR-0007), del mejor al peor. */
enum class NivelPuntualidad { SIN_HISTORIAL, PUNTUAL, SE_DEMORA, CUIDADO }

/** Cómo está un fiado vigente: cuánto falta pagarle, cuándo quedó pagado y cuánto se atrasó. */
data class EstadoFiado(
    val fiado: Movimiento,
    /** Plazo vigente, con prórrogas; `null` sin plazo. */
    val plazo: LocalDate?,
    val pendiente: Dinero,
    /** Día del pago que completó su monto; `null` si sigue pendiente. */
    val pagadoEl: LocalDate?,
    /** Días después del plazo en que siguió sin pagarse, hasta que se pagó o hasta hoy. 0 sin plazo. */
    val atraso: Long,
) {
    val pagado: Boolean get() = pagadoEl != null

    /** Días entre fiar y quedar pagado; `null` si sigue pendiente. */
    val diasParaPagar: Long? get() = pagadoEl?.let { ChronoUnit.DAYS.between(fiado.fecha, it) }

    /** Días que lleva vencido sin pagarse; 0 si no está vencido. */
    fun vencidoHace(hoy: LocalDate): Long = if (pagado || plazo == null || !hoy.isAfter(plazo)) 0 else ChronoUnit.DAYS.between(plazo, hoy)
}

data class Puntualidad(
    val nivel: NivelPuntualidad,
    /** Promedio de días entre fiar y quedar pagado, de los últimos fiados pagados. */
    val diasPromedioParaPagar: Double?,
    /** Promedio de días de atraso de los últimos fiados pagados que tenían plazo. */
    val atrasoPromedio: Double?,
    /** Porcentaje de esos fiados pagados con hasta [DIAS_TOLERADOS] de atraso. */
    val porcentajeATiempo: Int?,
    /** Lo que se debe de fiados con el plazo ya pasado, y desde hace cuántos días el más antiguo. */
    val vencido: Dinero,
    val diasVencido: Long,
    /** Plazo más cercano de lo que aún no vence. */
    val proximoVencimiento: LocalDate?,
    val fiados: List<EstadoFiado>,
) {
    fun estadoDe(fiadoId: Uuid): EstadoFiado? = fiados.firstOrNull { it.fiado.id == fiadoId }

    /** Sus últimos [FIADOS_RECIENTES] fiados pagados, del más reciente al más antiguo: de ellos salen sus números. */
    val pagadosRecientes: List<EstadoFiado>
        get() = fiados.filter { it.pagado }.sortedByDescending { it.pagadoEl }.take(FIADOS_RECIENTES)

    /**
     * Plazo que suele necesitar, en días y al menos uno, para sugerirlo al fiarle; `null` con menos de
     * [MINIMO_PARA_JUZGAR] fiados pagados.
     */
    val diasSugeridos: Int?
        get() =
            diasPromedioParaPagar
                ?.takeIf { pagadosRecientes.size >= MINIMO_PARA_JUZGAR }
                ?.let { maxOf(1, Math.round(it).toInt()) }

    companion object {
        /** Un día de atraso es normal; dos o más ya cuentan como demora. */
        const val DIAS_TOLERADOS = 1L
        const val DIAS_DEMORA = 2L
        const val DIAS_CUIDADO = 7L

        /** Para reflejar cómo paga ahora, solo cuentan sus últimos fiados pagados. */
        const val FIADOS_RECIENTES = 10

        /** Con menos fiados con plazo ya pagados no se juzga. */
        const val MINIMO_PARA_JUZGAR = 2

        /**
         * Reparte los pagos entre los fiados del más antiguo al más reciente y mide cómo paga el cliente
         * (ADR-0007). [zona] convierte el momento de cada prórroga en el día en que se dio.
         */
        fun calcular(
            movimientos: List<Movimiento>,
            prorrogas: List<Prorroga>,
            hoy: LocalDate,
            zona: ZoneId,
        ): Puntualidad {
            val fiados =
                repartirPagos(movimientos.filter { it.estaVigente }).map { (fiado, pendiente, pagadoEl) ->
                    val propias = prorrogas.filter { it.movimientoId == fiado.id }
                    EstadoFiado(fiado, plazoVigente(fiado, propias), pendiente, pagadoEl, atraso(fiado, propias, pagadoEl ?: hoy, zona))
                }
            val recientes = fiados.filter { it.pagado }.sortedByDescending { it.pagadoEl }.take(FIADOS_RECIENTES)
            val recientesConPlazo = recientes.filter { it.plazo != null }
            val vencidos = fiados.filter { it.vencidoHace(hoy) > 0 }
            val diasVencido = vencidos.maxOfOrNull { it.vencidoHace(hoy) } ?: 0
            val atrasoPromedio = recientesConPlazo.takeIf { it.isNotEmpty() }?.map { it.atraso }?.average()
            val juzgable = recientesConPlazo.size >= MINIMO_PARA_JUZGAR
            val nivel =
                when {
                    !juzgable && vencidos.isEmpty() -> NivelPuntualidad.SIN_HISTORIAL
                    (juzgable && atrasoPromedio!! >= DIAS_CUIDADO) || diasVencido >= DIAS_CUIDADO -> NivelPuntualidad.CUIDADO
                    (juzgable && atrasoPromedio!! >= DIAS_DEMORA) || diasVencido >= DIAS_DEMORA -> NivelPuntualidad.SE_DEMORA
                    else -> NivelPuntualidad.PUNTUAL
                }
            return Puntualidad(
                nivel = nivel,
                diasPromedioParaPagar = recientes.takeIf { it.isNotEmpty() }?.map { it.diasParaPagar!! }?.average(),
                atrasoPromedio = atrasoPromedio,
                porcentajeATiempo =
                    recientesConPlazo.takeIf { it.isNotEmpty() }?.let { lista ->
                        lista.count { it.atraso <= DIAS_TOLERADOS } * 100 / lista.size
                    },
                vencido = vencidos.fold(Dinero.CERO) { total, it -> total + it.pendiente },
                diasVencido = diasVencido,
                proximoVencimiento = fiados.filter { !it.pagado && it.plazo != null && !it.plazo.isBefore(hoy) }.minOfOrNull { it.plazo!! },
                fiados = fiados,
            )
        }

        /**
         * Recorre fiados y pagos en el orden en que ocurrieron. Cada pago cubre lo pendiente de los
         * fiados más antiguos; lo que sobra queda a favor y cubre los siguientes fiados el día en que se fían.
         */
        private fun repartirPagos(vigentes: List<Movimiento>): List<Triple<Movimiento, Dinero, LocalDate?>> {
            val enOrden = vigentes.sortedWith(compareBy({ it.fecha }, { it.registradoEn }, { it.id.toString() }))
            val pendientes = ArrayDeque<Pair<Movimiento, Long>>()
            val resultado = LinkedHashMap<Uuid, Triple<Movimiento, Dinero, LocalDate?>>()
            var aFavor = 0L
            for (movimiento in enOrden) {
                when (movimiento.tipo) {
                    TipoMovimiento.FIADO -> {
                        val cubierto = minOf(aFavor, movimiento.monto.centimos)
                        aFavor -= cubierto
                        val resta = movimiento.monto.centimos - cubierto
                        if (resta == 0L) {
                            resultado[movimiento.id] = Triple(movimiento, Dinero.CERO, movimiento.fecha)
                        } else {
                            pendientes.addLast(movimiento to resta)
                            resultado[movimiento.id] = Triple(movimiento, Dinero(resta), null)
                        }
                    }

                    TipoMovimiento.PAGO -> {
                        var disponible = movimiento.monto.centimos
                        while (disponible > 0 && pendientes.isNotEmpty()) {
                            val (fiado, resta) = pendientes.removeFirst()
                            val pagado = minOf(disponible, resta)
                            disponible -= pagado
                            if (pagado == resta) {
                                resultado[fiado.id] = Triple(fiado, Dinero.CERO, movimiento.fecha)
                            } else {
                                pendientes.addFirst(fiado to resta - pagado)
                                resultado[fiado.id] = Triple(fiado, Dinero(resta - pagado), null)
                            }
                        }
                        aFavor += disponible
                    }
                }
            }
            return resultado.values.toList()
        }

        /**
         * Días después del plazo en que siguió sin pagarse hasta [fin]. Con prórrogas, cada plazo cuenta
         * sus días de atraso hasta que se dio la siguiente: prorrogar no borra el atraso acumulado.
         */
        private fun atraso(
            fiado: Movimiento,
            prorrogas: List<Prorroga>,
            fin: LocalDate,
            zona: ZoneId,
        ): Long {
            var plazo = fiado.vence ?: return 0
            var total = 0L
            for (prorroga in prorrogas.sortedBy { it.registradaEn }) {
                if (!prorroga.vence.isAfter(plazo)) continue
                val dadaEl = prorroga.registradaEn.atZone(zona).toLocalDate()
                val hasta = minOf(dadaEl, fin)
                if (hasta.isAfter(plazo)) total += ChronoUnit.DAYS.between(plazo, hasta)
                plazo = prorroga.vence
            }
            if (fin.isAfter(plazo)) total += ChronoUnit.DAYS.between(plazo, fin)
            return total
        }
    }
}

/** Cómo le pagan al negocio: cuántos clientes hay en cada nivel y cuánto tardan todos en pagar (ADR-0007). */
data class PuntualidadDelNegocio(
    val clientesPorNivel: Map<NivelPuntualidad, Int>,
    /** Promedio de días para pagar de los últimos fiados pagados de cada cliente; `null` si nadie ha pagado. */
    val diasPromedioParaPagar: Double?,
    /** Clientes en Cuidado: primero el que más debe vencido y, a igual monto, el que más se atrasa. */
    val enCuidado: List<Uuid>,
) {
    fun clientesEn(nivel: NivelPuntualidad): Int = clientesPorNivel[nivel] ?: 0

    companion object {
        fun de(porCliente: Map<Uuid, Puntualidad>): PuntualidadDelNegocio {
            val dias = porCliente.values.flatMap { it.pagadosRecientes }.map { it.diasParaPagar!! }
            return PuntualidadDelNegocio(
                clientesPorNivel = porCliente.values.groupingBy { it.nivel }.eachCount(),
                diasPromedioParaPagar = dias.takeIf { it.isNotEmpty() }?.average(),
                enCuidado =
                    porCliente.entries
                        .filter { it.value.nivel == NivelPuntualidad.CUIDADO }
                        .sortedWith(
                            compareByDescending<Map.Entry<Uuid, Puntualidad>> { it.value.vencido }
                                .thenByDescending { it.value.atrasoPromedio ?: 0.0 },
                        ).map { it.key },
            )
        }
    }
}
