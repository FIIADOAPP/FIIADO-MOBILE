package com.fiiadoapp.fiado.core.data.sincronizacion

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.withTransaction
import com.fiiadoapp.fiado.core.data.ClienteEntity
import com.fiiadoapp.fiado.core.data.FiadoDatabase
import com.fiiadoapp.fiado.core.data.MetodoCobroEntity
import com.fiiadoapp.fiado.core.data.MovimientoEntity
import com.fiiadoapp.fiado.core.data.ProrrogaEntity
import com.fiiadoapp.fiado.domain.GeneradorDeIds
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

/**
 * Cuenta a la que pertenece la libreta de este teléfono (ADR-0003): una sola fila. Se crea al primer
 * inicio de sesión y no se borra al cerrarla. [cursor] guarda por dónde va la restauración, en la
 * misma transacción que cada página, para continuar si se corta.
 */
@Entity(tableName = "vinculo")
data class VinculoEntity(
    @PrimaryKey val id: Int = UNICO,
    val correo: String,
    val cursor: String?,
    val restaurada: Boolean,
    /** Primera vez que este teléfono se vincula: lo que hay en la cola nunca salió de aquí. */
    val primera: Boolean,
) {
    companion object {
        const val UNICO = 1
    }
}

/** Registros que llegaron del servidor en la restauración en curso; el resto solo existe aquí. */
@Entity(tableName = "ids_restaurados", primaryKeys = ["entidad", "id"])
data class IdRestauradoEntity(
    val entidad: String,
    val id: String,
)

@Dao
interface RestauracionDao {
    @Query("SELECT * FROM vinculo WHERE id = 1")
    suspend fun vinculo(): VinculoEntity?

    @Upsert
    suspend fun guardarVinculo(vinculo: VinculoEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun anotarRestaurados(ids: List<IdRestauradoEntity>)

    @Query("SELECT * FROM ids_restaurados")
    suspend fun restaurados(): List<IdRestauradoEntity>

    @Query("DELETE FROM ids_restaurados")
    suspend fun olvidarRestaurados()

    @Query("DELETE FROM operaciones_pendientes")
    suspend fun vaciarCola()

    @Query("SELECT * FROM metodos_cobro WHERE id NOT IN (SELECT id FROM ids_restaurados WHERE entidad = 'metodo_cobro') ORDER BY id")
    suspend fun metodosSoloLocales(): List<MetodoCobroEntity>

    @Query("SELECT * FROM clientes WHERE id NOT IN (SELECT id FROM ids_restaurados WHERE entidad = 'cliente') ORDER BY id")
    suspend fun clientesSoloLocales(): List<ClienteEntity>

    @Query("SELECT * FROM movimientos WHERE id NOT IN (SELECT id FROM ids_restaurados WHERE entidad = 'movimiento') ORDER BY id")
    suspend fun movimientosSoloLocales(): List<MovimientoEntity>

    @Query("SELECT * FROM prorrogas WHERE id NOT IN (SELECT id FROM ids_restaurados WHERE entidad = 'prorroga') ORDER BY id")
    suspend fun prorrogasSoloLocales(): List<ProrrogaEntity>
}

@Serializable
data class RegistroDto(
    val entidad: String,
    val datos: JsonObject,
)

@Serializable
data class PaginaDto(
    val registros: List<RegistroDto>,
    val cursor: String?,
)

/** Lo que el inicio de sesión necesita saber del vínculo. */
interface Vinculacion {
    /** Correo de la cuenta a la que pertenece este teléfono, o `null` si nunca se vinculó. */
    suspend fun correoVinculado(): String?

    /** Tras iniciar sesión: la próxima sincronización restaura antes de enviar. */
    suspend fun prepararRestauracion(correo: String)
}

/**
 * Trae la libreta del servidor y la une con la local sin borrar nada (ADR-0003): lo que existe en
 * ambos lados queda como en el servidor, salvo la anulación, que se conserva si cualquiera la tiene;
 * lo que solo existe aquí se encola para subir.
 */
class Restaurador
    @Inject
    constructor(
        private val db: FiadoDatabase,
        private val api: ApiSincronizacion,
        private val ids: GeneradorDeIds,
        private val programador: ProgramadorDeEnvio,
    ) : Vinculacion {
        private val dao = db.restauracionDao()

        override suspend fun correoVinculado(): String? = dao.vinculo()?.correo

        override suspend fun prepararRestauracion(correo: String) {
            db.withTransaction {
                val anterior = dao.vinculo()
                dao.olvidarRestaurados()
                dao.guardarVinculo(VinculoEntity(correo = correo, cursor = null, restaurada = false, primera = anterior == null))
            }
            programador.programar()
        }

        /**
         * Restaura si falta, página por página. Devuelve `null` cuando la libreta ya está restaurada, o
         * el motivo por el que no se pudo terminar; lo avanzado queda guardado.
         */
        suspend fun restaurarSiFalta(correoSesion: String): ResultadoEnvio? {
            // Una sesión abierta antes de existir el vínculo (versión 0.3.0) se vincula aquí.
            var vinculo =
                dao.vinculo() ?: VinculoEntity(correo = correoSesion, cursor = null, restaurada = false, primera = true)
                    .also { dao.guardarVinculo(it) }
            while (!vinculo.restaurada) {
                val pagina =
                    when (val respuesta = llamarApi { api.restaurar(vinculo.cursor, TAMANO_PAGINA) }) {
                        is Respuesta.Exito -> respuesta.valor
                        is Respuesta.Fallo -> return respuesta.motivo
                    }
                val registros =
                    try {
                        pagina.registros.map(::decodificar)
                    } catch (_: IllegalArgumentException) {
                        return ResultadoEnvio.RECHAZADO
                    }
                vinculo =
                    db.withTransaction {
                        val pendientes = conCambiosPendientes()
                        // Un movimiento siempre se combina: la anulación se conserva venga de donde venga.
                        registros
                            .filterNot { it !is Restaurado.Movimiento && (it.tipo.valor to it.id) in pendientes }
                            .forEach { aplicar(it) }
                        dao.anotarRestaurados(registros.map { IdRestauradoEntity(it.tipo.valor, it.id) })
                        val siguiente = vinculo.copy(cursor = pagina.cursor)
                        if (pagina.cursor == null) terminar(siguiente) else siguiente.also { dao.guardarVinculo(it) }
                    }
            }
            return null
        }

        private suspend fun aplicar(registro: Restaurado) {
            when (registro) {
                is Restaurado.Metodo -> {
                    db.metodoCobroDao().guardar(registro.fila)
                }

                is Restaurado.Cliente -> {
                    db.clienteDao().guardar(registro.fila)
                }

                is Restaurado.Movimiento -> {
                    val local = db.movimientoDao().obtener(registro.fila.id)
                    db.movimientoDao().guardar(registro.fila.copy(anuladoEn = local?.anuladoEn ?: registro.fila.anuladoEn))
                }

                is Restaurado.Prorroga -> {
                    db.prorrogaDao().guardar(registro.fila)
                }
            }
        }

        /**
         * Registros con un cambio en la cola: al restaurar se conserva la versión del teléfono, que es la más
         * reciente y se sube después (ADR-0008).
         */
        private suspend fun conCambiosPendientes(): Set<Pair<String, String>> =
            db
                .colaDao()
                .todas()
                .map {
                    it.entidad to
                        jsonDeSincronizacion
                            .parseToJsonElement(it.datos)
                            .jsonObject
                            .getValue("id")
                            .jsonPrimitive.content
                }.toSet()

        /**
         * La primera vez, la cola se reemplaza por una foto de lo que solo existe aquí y de lo que se conservó
         * por tener cambios pendientes: es lo mismo, sin repetidos.
         */
        private suspend fun terminar(vinculo: VinculoEntity): VinculoEntity {
            val cola = db.colaDao()
            if (vinculo.primera) {
                val conservados = conCambiosPendientes()
                dao.vaciarCola()
                reencolar(conservados)
            }

            suspend fun encolar(
                tipo: TipoDeRegistro,
                foto: String,
            ) = cola.agregar(OperacionPendienteEntity(id = ids.nuevo().toString(), entidad = tipo.valor, datos = foto))
            dao.metodosSoloLocales().forEach { encolar(TipoDeRegistro.METODO_COBRO, it.foto()) }
            dao.clientesSoloLocales().forEach { encolar(TipoDeRegistro.CLIENTE, it.foto()) }
            dao.movimientosSoloLocales().forEach { encolar(TipoDeRegistro.MOVIMIENTO, it.foto()) }
            dao.prorrogasSoloLocales().forEach { encolar(TipoDeRegistro.PRORROGA, it.foto()) }
            dao.olvidarRestaurados()
            return vinculo.copy(cursor = null, restaurada = true, primera = false).also { dao.guardarVinculo(it) }
        }

        /** Vuelve a encolar la foto actual de los registros conservados que también están en el servidor. */
        private suspend fun reencolar(conservados: Set<Pair<String, String>>) {
            val restaurados = dao.restaurados().map { it.entidad to it.id }.toSet()
            conservados.filter { it in restaurados }.forEach { (entidad, id) ->
                val foto =
                    when (entidad) {
                        TipoDeRegistro.METODO_COBRO.valor -> db.metodoCobroDao().obtener(id)?.foto()
                        TipoDeRegistro.CLIENTE.valor -> db.clienteDao().obtener(id)?.foto()
                        TipoDeRegistro.PRORROGA.valor -> db.prorrogaDao().obtener(id)?.foto()
                        else -> db.movimientoDao().obtener(id)?.foto()
                    } ?: return@forEach
                db.colaDao().agregar(OperacionPendienteEntity(id = ids.nuevo().toString(), entidad = entidad, datos = foto))
            }
        }

        private companion object {
            const val TAMANO_PAGINA = 500
        }
    }
