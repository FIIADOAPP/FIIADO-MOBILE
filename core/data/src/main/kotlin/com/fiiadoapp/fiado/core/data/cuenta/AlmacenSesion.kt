package com.fiiadoapp.fiado.core.data.cuenta

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SesionGuardada(
    val tokenAcceso: String,
    val tokenRefresco: String,
    val correo: String,
    val nombreNegocio: String,
)

/** Cifra y descifra con una clave que no sale del teléfono (ADR-0005). */
interface Cifrador {
    fun cifrar(datos: ByteArray): ByteArray

    fun descifrar(datos: ByteArray): ByteArray
}

/** La sesión vive en DataStore, cifrada: el archivo solo no basta para leer los tokens. */
class AlmacenSesion(
    private val dataStore: DataStore<Preferences>,
    private val cifrador: Cifrador,
    private val aTexto: (ByteArray) -> String = { Base64.encodeToString(it, Base64.NO_WRAP) },
    private val aBytes: (String) -> ByteArray = { Base64.decode(it, Base64.NO_WRAP) },
) {
    val sesion: Flow<SesionGuardada?> = dataStore.data.map { it[SESION]?.let(::descifrar) }

    /** `true` si la API cerró la sesión desde fuera; queda hasta que el usuario vea el aviso. */
    val terminadaDesdeFuera: Flow<Boolean> = dataStore.data.map { it[TERMINADA] ?: false }

    suspend fun actual(): SesionGuardada? = sesion.first()

    suspend fun guardar(sesion: SesionGuardada) {
        val cifrada = aTexto(cifrador.cifrar(Json.encodeToString(sesion).encodeToByteArray()))
        dataStore.edit {
            it[SESION] = cifrada
            it[TERMINADA] = false
        }
    }

    suspend fun actualizarTokens(
        acceso: String,
        refresco: String,
    ) {
        actual()?.let { guardar(it.copy(tokenAcceso = acceso, tokenRefresco = refresco)) }
    }

    suspend fun borrar(terminadaDesdeFuera: Boolean) {
        dataStore.edit {
            it.remove(SESION)
            it[TERMINADA] = terminadaDesdeFuera
        }
    }

    suspend fun descartarAviso() {
        dataStore.edit { it[TERMINADA] = false }
    }

    /** Si no se puede descifrar (por ejemplo, la clave del Keystore se perdió), se trata como sin sesión. */
    private fun descifrar(texto: String): SesionGuardada? =
        runCatching { Json.decodeFromString<SesionGuardada>(cifrador.descifrar(aBytes(texto)).decodeToString()) }.getOrNull()

    private companion object {
        val SESION = stringPreferencesKey("sesion")
        val TERMINADA = booleanPreferencesKey("sesion_terminada_desde_fuera")
    }
}
