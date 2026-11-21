package com.fiiadoapp.fiado.bloqueo

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.fiiadoapp.fiado.core.ui.Autenticador

/**
 * Cualquier biometría del teléfono, con su PIN, patrón o contraseña como alternativa. Esta combinación
 * funciona desde la API 26; no hace falta biometría fuerte porque no protege ninguna clave (ADR-0006).
 */
class AutenticadorBiometrico(
    private val actividad: FragmentActivity,
) : Autenticador {
    override fun disponible(): Boolean = BiometricManager.from(actividad).canAuthenticate(PERMITIDOS) == BiometricManager.BIOMETRIC_SUCCESS

    override fun autenticar(
        titulo: String,
        onExito: () -> Unit,
    ) {
        val respuesta =
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(resultado: BiometricPrompt.AuthenticationResult) = onExito()
            }
        val pedido =
            BiometricPrompt.PromptInfo
                .Builder()
                .setTitle(titulo)
                .setAllowedAuthenticators(PERMITIDOS)
                .build()
        BiometricPrompt(actividad, ContextCompat.getMainExecutor(actividad), respuesta).authenticate(pedido)
    }

    private companion object {
        const val PERMITIDOS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL
    }
}
