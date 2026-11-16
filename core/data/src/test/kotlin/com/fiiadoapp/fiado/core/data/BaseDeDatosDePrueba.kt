package com.fiiadoapp.fiado.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fiiadoapp.fiado.domain.Cliente
import com.fiiadoapp.fiado.domain.Dinero
import com.fiiadoapp.fiado.domain.MetodoCobro
import com.fiiadoapp.fiado.domain.Movimiento
import com.fiiadoapp.fiado.domain.Telefono
import com.fiiadoapp.fiado.domain.TipoMovimiento
import org.junit.After
import java.time.Instant
import java.time.LocalDate
import kotlin.uuid.Uuid

abstract class BaseDeDatosDePrueba {
    protected val db: FiadoDatabase =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), FiadoDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(SembrarMetodosIniciales)
            .build()

    protected val clientes = RepositorioClientesRoom(db.clienteDao())
    protected val movimientos = RepositorioMovimientosRoom(db.movimientoDao())
    protected val metodos = RepositorioMetodosCobroRoom(db.metodoCobroDao())

    protected val ahora: Instant = Instant.parse("2026-11-15T15:30:00.123Z")
    protected val hoy: LocalDate = LocalDate.of(2026, 11, 15)

    @After
    fun cerrar() = db.close()

    protected fun cliente(
        nombre: String,
        telefono: String? = null,
        archivadoEn: Instant? = null,
    ) = Cliente(Uuid.random(), nombre, telefono?.let(Telefono::desdeTexto), ahora, ahora, archivadoEn)

    protected fun movimiento(
        cliente: Cliente,
        tipo: TipoMovimiento,
        centimos: Long,
        fecha: LocalDate = hoy,
        registradoEn: Instant = ahora,
        nota: String? = null,
        metodo: MetodoCobro = MetodoCobro.EFECTIVO,
    ) = Movimiento(
        Uuid.random(),
        cliente.id,
        tipo,
        Dinero(centimos),
        nota,
        fecha,
        registradoEn,
        anuladoEn = null,
        metodoCobroId = metodo.id.takeIf { tipo == TipoMovimiento.PAGO },
    )
}
