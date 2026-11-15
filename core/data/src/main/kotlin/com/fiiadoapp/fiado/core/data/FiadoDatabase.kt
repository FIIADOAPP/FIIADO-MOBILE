package com.fiiadoapp.fiado.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ClienteEntity::class, MovimientoEntity::class, MetodoCobroEntity::class], version = 2)
abstract class FiadoDatabase : RoomDatabase() {
    abstract fun clienteDao(): ClienteDao

    abstract fun movimientoDao(): MovimientoDao

    abstract fun metodoCobroDao(): MetodoCobroDao

    companion object {
        /** Journal clásico en lugar de WAL, para que una escritura confirmada sobreviva a un apagado (ADR-0003). */
        fun construir(
            contexto: Context,
            nombre: String = "fiado.db",
        ): FiadoDatabase =
            Room
                .databaseBuilder(contexto, FiadoDatabase::class.java, nombre)
                .setJournalMode(JournalMode.TRUNCATE)
                .addMigrations(MIGRACION_1_2)
                .addCallback(SembrarMetodosIniciales)
                .build()
    }
}

/** En una instalación nueva Room crea la versión 2 directamente; la migración no corre y los iniciales se siembran aquí. */
internal object SembrarMetodosIniciales : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) = insertarMetodosIniciales(db)
}
