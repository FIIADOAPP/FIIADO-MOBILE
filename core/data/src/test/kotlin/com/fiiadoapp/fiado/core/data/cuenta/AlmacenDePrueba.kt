package com.fiiadoapp.fiado.core.data.cuenta

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.util.Base64

/** Invierte los bytes: suficiente para comprobar que nada se guarda en claro. */
private class CifradorDePrueba : Cifrador {
    override fun cifrar(datos: ByteArray) = datos.reversedArray()

    override fun descifrar(datos: ByteArray) = datos.reversedArray()
}

/** Almacén real sobre un archivo temporal, sin Keystore ni `android.util.Base64`. */
fun almacenDePrueba(
    carpeta: File,
    alcance: CoroutineScope,
): AlmacenSesion =
    AlmacenSesion(
        PreferenceDataStoreFactory.create(scope = alcance) { File(carpeta, "sesion.preferences_pb") },
        CifradorDePrueba(),
        aTexto = { Base64.getEncoder().encodeToString(it) },
        aBytes = { Base64.getDecoder().decode(it) },
    )
