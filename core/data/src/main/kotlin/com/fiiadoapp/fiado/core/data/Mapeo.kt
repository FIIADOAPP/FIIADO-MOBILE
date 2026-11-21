package com.fiiadoapp.fiado.core.data

import com.fiiadoapp.fiado.domain.Cliente
import com.fiiadoapp.fiado.domain.Dinero
import com.fiiadoapp.fiado.domain.MetodoCobro
import com.fiiadoapp.fiado.domain.Movimiento
import com.fiiadoapp.fiado.domain.Telefono
import com.fiiadoapp.fiado.domain.TipoMovimiento
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import kotlin.uuid.Uuid

private val MARCAS_DIACRITICAS = Regex("""\p{Mn}+""")

/** Minúsculas y sin tildes: "Doña Rosa" → "dona rosa". */
internal fun normalizarParaBusqueda(texto: String): String =
    Normalizer.normalize(texto, Normalizer.Form.NFD).replace(MARCAS_DIACRITICAS, "").lowercase(Locale.ROOT)

/** Patrón para `LIKE ... ESCAPE '\'`: el texto del usuario se busca literalmente, sin comodines. */
internal fun patronDeBusqueda(busqueda: String): String =
    normalizarParaBusqueda(busqueda.trim())
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")

internal fun Cliente.aEntidad() =
    ClienteEntity(
        id = id.toString(),
        nombre = nombre,
        nombreBusqueda = normalizarParaBusqueda(nombre),
        telefono = telefono?.e164,
        creadoEn = creadoEn.toEpochMilli(),
        actualizadoEn = actualizadoEn.toEpochMilli(),
        archivadoEn = archivadoEn?.toEpochMilli(),
    )

internal fun ClienteEntity.aDominio() =
    Cliente(
        id = Uuid.parse(id),
        nombre = nombre,
        telefono = telefono?.let { checkNotNull(Telefono.desdeTexto(it)) { "Teléfono guardado inválido: $it" } },
        creadoEn = Instant.ofEpochMilli(creadoEn),
        actualizadoEn = Instant.ofEpochMilli(actualizadoEn),
        archivadoEn = archivadoEn?.let(Instant::ofEpochMilli),
    )

internal fun Movimiento.aEntidad() =
    MovimientoEntity(
        id = id.toString(),
        clienteId = clienteId.toString(),
        tipo = tipo.name,
        montoCentimos = monto.centimos,
        nota = nota,
        fecha = fecha.toEpochDay(),
        registradoEn = registradoEn.toEpochMilli(),
        anuladoEn = anuladoEn?.toEpochMilli(),
        metodoCobroId = metodoCobroId?.toString(),
        vence = vence?.toEpochDay(),
    )

internal fun MovimientoEntity.aDominio() =
    Movimiento(
        id = Uuid.parse(id),
        clienteId = Uuid.parse(clienteId),
        tipo = TipoMovimiento.valueOf(tipo),
        monto = Dinero(montoCentimos),
        nota = nota,
        fecha = LocalDate.ofEpochDay(fecha),
        registradoEn = Instant.ofEpochMilli(registradoEn),
        anuladoEn = anuladoEn?.let(Instant::ofEpochMilli),
        metodoCobroId = metodoCobroId?.let(Uuid::parse),
        vence = vence?.let(LocalDate::ofEpochDay),
    )

internal fun MetodoCobro.aEntidad() =
    MetodoCobroEntity(
        id = id.toString(),
        nombre = nombre,
        creadoEn = creadoEn.toEpochMilli(),
        actualizadoEn = actualizadoEn.toEpochMilli(),
        archivadoEn = archivadoEn?.toEpochMilli(),
    )

internal fun MetodoCobroEntity.aDominio() =
    MetodoCobro(
        id = Uuid.parse(id),
        nombre = nombre,
        creadoEn = Instant.ofEpochMilli(creadoEn),
        actualizadoEn = Instant.ofEpochMilli(actualizadoEn),
        archivadoEn = archivadoEn?.let(Instant::ofEpochMilli),
    )
