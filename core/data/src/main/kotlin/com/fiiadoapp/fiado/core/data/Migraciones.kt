package com.fiiadoapp.fiado.core.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fiiadoapp.fiado.domain.MetodoCobro

/**
 * Versión 2 (ADR-0004): métodos de cobro. Crea la tabla, siembra Efectivo, Yape y Plin, y agrega
 * `metodoCobroId` a los movimientos asignando Efectivo a los pagos existentes. SQLite no agrega una
 * clave foránea a una tabla existente, así que `movimientos` se reconstruye.
 */
internal val MIGRACION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `metodos_cobro` (`id` TEXT NOT NULL, `nombre` TEXT NOT NULL, " +
                    "`creadoEn` INTEGER NOT NULL, `actualizadoEn` INTEGER NOT NULL, `archivadoEn` INTEGER, PRIMARY KEY(`id`))",
            )
            insertarMetodosIniciales(db)
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `movimientos_nueva` (`id` TEXT NOT NULL, `clienteId` TEXT NOT NULL, " +
                    "`tipo` TEXT NOT NULL, `montoCentimos` INTEGER NOT NULL, `nota` TEXT, `fecha` INTEGER NOT NULL, " +
                    "`registradoEn` INTEGER NOT NULL, `anuladoEn` INTEGER, `metodoCobroId` TEXT, PRIMARY KEY(`id`), " +
                    "FOREIGN KEY(`clienteId`) REFERENCES `clientes`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION , " +
                    "FOREIGN KEY(`metodoCobroId`) REFERENCES `metodos_cobro`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )",
            )
            db.execSQL(
                "INSERT INTO `movimientos_nueva` " +
                    "SELECT `id`, `clienteId`, `tipo`, `montoCentimos`, `nota`, `fecha`, `registradoEn`, `anuladoEn`, " +
                    "CASE WHEN `tipo` = 'PAGO' THEN '${MetodoCobro.EFECTIVO.id}' END FROM `movimientos`",
            )
            db.execSQL("DROP TABLE `movimientos`")
            db.execSQL("ALTER TABLE `movimientos_nueva` RENAME TO `movimientos`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_movimientos_clienteId` ON `movimientos` (`clienteId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_movimientos_fecha` ON `movimientos` (`fecha`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_movimientos_metodoCobroId` ON `movimientos` (`metodoCobroId`)")
        }
    }

/**
 * Versión 3 (ADR-0003 de la API): cola de operaciones pendientes. Lo registrado antes queda fuera
 * de la cola; se sube al vincular el teléfono con la cuenta, cuando se restaura.
 */
internal val MIGRACION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `operaciones_pendientes` (`orden` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`id` TEXT NOT NULL, `entidad` TEXT NOT NULL, `datos` TEXT NOT NULL)",
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_operaciones_pendientes_id` ON `operaciones_pendientes` (`id`)")
        }
    }

/** Idempotente: los iniciales tienen identificadores fijos. */
internal fun insertarMetodosIniciales(db: SupportSQLiteDatabase) {
    MetodoCobro.INICIALES.forEach {
        db.execSQL(
            "INSERT OR IGNORE INTO `metodos_cobro` (`id`, `nombre`, `creadoEn`, `actualizadoEn`, `archivadoEn`) VALUES (?, ?, ?, ?, NULL)",
            arrayOf<Any>(it.id.toString(), it.nombre, it.creadoEn.toEpochMilli(), it.actualizadoEn.toEpochMilli()),
        )
    }
}
