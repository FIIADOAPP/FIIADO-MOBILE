package com.fiiadoapp.fiado.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fiiadoapp.fiado.core.data.sincronizacion.ColaDao
import com.fiiadoapp.fiado.core.data.sincronizacion.IdRestauradoEntity
import com.fiiadoapp.fiado.core.data.sincronizacion.OperacionPendienteEntity
import com.fiiadoapp.fiado.core.data.sincronizacion.RestauracionDao
import com.fiiadoapp.fiado.core.data.sincronizacion.VinculoEntity

@Database(
    entities = [
        ClienteEntity::class,
        MovimientoEntity::class,
        MetodoCobroEntity::class,
        OperacionPendienteEntity::class,
        VinculoEntity::class,
        IdRestauradoEntity::class,
    ],
    version = 5,
)
abstract class FiadoDatabase : RoomDatabase() {
    abstract fun clienteDao(): ClienteDao

    abstract fun movimientoDao(): MovimientoDao

    abstract fun metodoCobroDao(): MetodoCobroDao

    abstract fun colaDao(): ColaDao

    abstract fun restauracionDao(): RestauracionDao

    companion object {
        /** Journal clásico en lugar de WAL, para que una escritura confirmada sobreviva a un apagado (ADR-0003). */
        fun construir(
            contexto: Context,
            nombre: String = "fiado.db",
        ): FiadoDatabase =
            Room
                .databaseBuilder(contexto, FiadoDatabase::class.java, nombre)
                .setJournalMode(JournalMode.TRUNCATE)
                .addMigrations(MIGRACION_1_2, MIGRACION_2_3, MIGRACION_3_4, MIGRACION_4_5)
                .addCallback(SembrarMetodosIniciales)
                .build()
    }
}

/** En una instalación nueva Room crea la última versión directamente; las migraciones no corren y los iniciales se siembran aquí. */
internal object SembrarMetodosIniciales : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) = insertarMetodosIniciales(db)
}
