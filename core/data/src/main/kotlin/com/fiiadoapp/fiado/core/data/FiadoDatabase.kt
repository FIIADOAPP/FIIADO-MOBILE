package com.fiiadoapp.fiado.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ClienteEntity::class, MovimientoEntity::class], version = 1)
abstract class FiadoDatabase : RoomDatabase() {
    abstract fun clienteDao(): ClienteDao

    abstract fun movimientoDao(): MovimientoDao

    companion object {
        /** Journal clásico en lugar de WAL, para que una escritura confirmada sobreviva a un apagado (ADR-0003). */
        fun construir(
            contexto: Context,
            nombre: String = "fiado.db",
        ): FiadoDatabase =
            Room
                .databaseBuilder(contexto, FiadoDatabase::class.java, nombre)
                .setJournalMode(JournalMode.TRUNCATE)
                .build()
    }
}
