package com.fiiadoapp.fiado.core.data.cuenta

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM con una clave del Android Keystore: se genera en el teléfono y nunca se puede exportar.
 * El resultado es el vector de inicialización (12 bytes) seguido del texto cifrado con su etiqueta.
 */
class CifradorKeystore : Cifrador {
    override fun cifrar(datos: ByteArray): ByteArray {
        val cifrado = Cipher.getInstance(TRANSFORMACION).apply { init(Cipher.ENCRYPT_MODE, clave()) }
        return cifrado.iv + cifrado.doFinal(datos)
    }

    override fun descifrar(datos: ByteArray): ByteArray {
        val iv = datos.copyOfRange(0, LARGO_IV)
        val descifrado =
            Cipher.getInstance(TRANSFORMACION).apply {
                init(Cipher.DECRYPT_MODE, clave(), GCMParameterSpec(BITS_ETIQUETA, iv))
            }
        return descifrado.doFinal(datos, LARGO_IV, datos.size - LARGO_IV)
    }

    private fun clave(): SecretKey {
        val almacen = KeyStore.getInstance(PROVEEDOR).apply { load(null) }
        (almacen.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVEEDOR)
        generador.init(
            KeyGenParameterSpec
                .Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generador.generateKey()
    }

    private companion object {
        const val PROVEEDOR = "AndroidKeyStore"
        const val ALIAS = "fiado_sesion"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
        const val LARGO_IV = 12
        const val BITS_ETIQUETA = 128
    }
}
