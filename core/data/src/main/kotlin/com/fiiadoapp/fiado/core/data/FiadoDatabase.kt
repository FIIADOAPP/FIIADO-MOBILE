package com.fiiadoapp.fiado.core.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ClienteEntity::class, MovimientoEntity::class], version = 1)
abstract class FiadoDatabase : RoomDatabase() {
    abstract fun clienteDao(): ClienteDao

    abstract fun movimientoDao(): MovimientoDao
}
