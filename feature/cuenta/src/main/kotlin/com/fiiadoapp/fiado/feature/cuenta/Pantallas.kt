package com.fiiadoapp.fiado.feature.cuenta

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fiiadoapp.fiado.core.ui.Avatar
import com.fiiadoapp.fiado.core.ui.LocalAutenticador
import com.fiiadoapp.fiado.core.ui.LocalControlDeAnuncios
import com.fiiadoapp.fiado.core.ui.Tarjeta
import com.fiiadoapp.fiado.domain.EstadoQuitarAnuncios
import com.fiiadoapp.fiado.domain.EstadoRespaldo

@Composable
internal fun CuentaPantalla(
    onCrearCuenta: () -> Unit,
    onIniciarSesion: () -> Unit,
    viewModel: CuentaViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val bloqueoActivo by viewModel.bloqueoActivo.collectAsStateWithLifecycle()
    var confirmandoCierre by remember { mutableStateOf(false) }
    PantallaDeCuenta(stringResource(R.string.cuenta_titulo), onVolver = null) {
        when (val actual = estado) {
            EstadoCuenta.Cargando -> {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }

            EstadoCuenta.SinCuenta if !viewModel.cuentaDisponible -> {
                Tarjeta(Modifier.fillMaxWidth(), relleno = 20.dp) {
                    Text(stringResource(R.string.cuenta_solo_telefono_titulo), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.cuenta_solo_telefono_explicacion),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            EstadoCuenta.SinCuenta -> {
                Text(stringResource(R.string.cuenta_sin_cuenta_explicacion), style = MaterialTheme.typography.bodyLarge)
                BotonPrincipal(stringResource(R.string.cuenta_crear), enviando = false, onClick = onCrearCuenta)
                FilledTonalButton(onClick = onIniciarSesion, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(stringResource(R.string.cuenta_iniciar_sesion))
                }
            }

            is EstadoCuenta.Abierta -> {
                Tarjeta(Modifier.fillMaxWidth(), relleno = 20.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(actual.cuenta.nombreNegocio, style = MaterialTheme.typography.titleLarge)
                            Text(
                                actual.cuenta.correo,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Avatar(actual.cuenta.nombreNegocio, tamano = 56.dp)
                    }
                }
                Tarjeta(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.cuenta_respaldo), style = MaterialTheme.typography.titleMedium)
                    EstadoDelRespaldo(actual.respaldo)
                }
                FilledTonalButton(onClick = { confirmandoCierre = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(stringResource(R.string.cuenta_cerrar_sesion))
                }
            }
        }
        if (estado != EstadoCuenta.Cargando) TarjetaDeSeguridad(bloqueoActivo, viewModel::cambiarBloqueo)
        TarjetaDeAnuncios()
    }
    if (confirmandoCierre) {
        AlertDialog(
            onDismissRequest = { confirmandoCierre = false },
            title = { Text(stringResource(R.string.cuenta_cerrar_sesion_titulo)) },
            text = {
                val pendientes = (estado as? EstadoCuenta.Abierta)?.respaldo?.pendientes ?: 0
                Text(
                    if (pendientes == 0) {
                        stringResource(R.string.cuenta_cerrar_sesion_explicacion)
                    } else {
                        pluralStringResource(R.plurals.cuenta_cerrar_sesion_con_pendientes, pendientes, pendientes)
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmandoCierre = false
                    viewModel.cerrarSesion()
                }) { Text(stringResource(R.string.cuenta_cerrar_sesion)) }
            },
            dismissButton = { TextButton(onClick = { confirmandoCierre = false }) { Text(stringResource(R.string.cuenta_cancelar)) } },
        )
    }
}

/** Si todo está en el servidor, cuánto falta subir o si el envío se detuvo (ADR-0003 de FIIADO-API). */
@Composable
private fun EstadoDelRespaldo(respaldo: EstadoRespaldo) {
    val (texto, color) =
        when {
            respaldo.detenido -> {
                stringResource(R.string.cuenta_respaldo_detenido) to MaterialTheme.colorScheme.error
            }

            respaldo.pendientes == 0 -> {
                stringResource(R.string.cuenta_respaldo_al_dia) to MaterialTheme.colorScheme.onSurfaceVariant
            }

            else -> {
                pluralStringResource(R.plurals.cuenta_respaldo_pendientes, respaldo.pendientes, respaldo.pendientes) to
                    MaterialTheme.colorScheme.onSurfaceVariant
            }
        }
    Text(texto, color = color, style = MaterialTheme.typography.bodyMedium)
}

@Composable
internal fun CrearCuentaPantalla(
    onVolver: () -> Unit,
    onCreada: (correo: String) -> Unit,
    viewModel: CrearCuentaViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.creada) { estado.creada?.let(onCreada) }
    PantallaDeCuenta(stringResource(R.string.cuenta_crear), onVolver) {
        Campo(estado.nombreNegocio, viewModel::cambiarNombreNegocio, stringResource(R.string.cuenta_nombre_negocio))
        Campo(estado.correo, viewModel::cambiarCorreo, stringResource(R.string.cuenta_correo), KeyboardType.Email)
        Campo(
            estado.contrasena,
            viewModel::cambiarContrasena,
            stringResource(R.string.cuenta_contrasena_nueva),
            KeyboardType.Password,
            secreto = true,
        )
        MensajeDeError(estado.error)
        BotonPrincipal(stringResource(R.string.cuenta_crear), estado.enviando, onClick = viewModel::crear)
    }
}

@Composable
internal fun VerificarCorreoPantalla(
    onVolver: () -> Unit,
    onVerificado: (correo: String) -> Unit,
    viewModel: VerificarCorreoViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.verificado) { if (estado.verificado) onVerificado(estado.correo) }
    PantallaDeCuenta(stringResource(R.string.cuenta_verificar_titulo), onVolver) {
        Text(stringResource(R.string.cuenta_verificar_explicacion, estado.correo), style = MaterialTheme.typography.bodyLarge)
        Campo(estado.codigo, viewModel::cambiarCodigo, stringResource(R.string.cuenta_codigo), KeyboardType.NumberPassword)
        MensajeDeError(estado.error)
        BotonPrincipal(
            stringResource(R.string.cuenta_verificar),
            estado.enviando,
            habilitado = estado.codigo.length == 6,
            onClick = viewModel::verificar,
        )
        BotonReenviar(estado.esperaReenvio, estado.reenviado, viewModel::reenviar)
    }
}

@Composable
private fun BotonReenviar(
    esperaReenvio: Int,
    reenviado: Boolean,
    onReenviar: () -> Unit,
) {
    TextButton(onClick = onReenviar, enabled = esperaReenvio == 0, modifier = Modifier.fillMaxWidth()) {
        Text(
            when {
                esperaReenvio > 0 -> stringResource(R.string.cuenta_reenviar_en, esperaReenvio)
                reenviado -> stringResource(R.string.cuenta_reenviar_otra_vez)
                else -> stringResource(R.string.cuenta_reenviar)
            },
        )
    }
}

@Composable
internal fun IniciarSesionPantalla(
    onVolver: () -> Unit,
    onIniciada: () -> Unit,
    onVerificar: (correo: String) -> Unit,
    onRecuperar: (correo: String) -> Unit,
    viewModel: IniciarSesionViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.iniciada) { if (estado.iniciada) onIniciada() }
    LaunchedEffect(estado.faltaVerificar) {
        if (estado.faltaVerificar) {
            viewModel.verificacionAtendida()
            onVerificar(estado.correo.trim())
        }
    }
    PantallaDeCuenta(stringResource(R.string.cuenta_iniciar_sesion), onVolver) {
        Campo(estado.correo, viewModel::cambiarCorreo, stringResource(R.string.cuenta_correo), KeyboardType.Email)
        Campo(
            estado.contrasena,
            viewModel::cambiarContrasena,
            stringResource(R.string.cuenta_contrasena),
            KeyboardType.Password,
            secreto = true,
        )
        MensajeDeError(estado.error)
        BotonPrincipal(stringResource(R.string.cuenta_iniciar_sesion), estado.enviando, onClick = viewModel::iniciar)
        TextButton(onClick = { onRecuperar(estado.correo.trim()) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.cuenta_olvide_contrasena))
        }
    }
    if (estado.pidiendoCeder) {
        AlertDialog(
            onDismissRequest = viewModel::cancelarCeder,
            title = { Text(stringResource(R.string.cuenta_ceder_titulo)) },
            text = { Text(stringResource(R.string.cuenta_ceder_explicacion)) },
            confirmButton = { TextButton(onClick = viewModel::cederElOtroTelefono) { Text(stringResource(R.string.cuenta_ceder)) } },
            dismissButton = { TextButton(onClick = viewModel::cancelarCeder) { Text(stringResource(R.string.cuenta_cancelar)) } },
        )
    }
}

@Composable
internal fun RecuperarContrasenaPantalla(
    onVolver: () -> Unit,
    onRestablecida: (correo: String) -> Unit,
    viewModel: RecuperarContrasenaViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.restablecida) { if (estado.restablecida) onRestablecida(estado.correo.trim()) }
    PantallaDeCuenta(stringResource(R.string.cuenta_recuperar_titulo), onVolver) {
        if (!estado.codigoEnviado) {
            Text(stringResource(R.string.cuenta_recuperar_explicacion), style = MaterialTheme.typography.bodyLarge)
            Campo(estado.correo, viewModel::cambiarCorreo, stringResource(R.string.cuenta_correo), KeyboardType.Email)
            MensajeDeError(estado.error)
            BotonPrincipal(stringResource(R.string.cuenta_enviar_codigo), estado.enviando, onClick = viewModel::pedirCodigo)
        } else {
            Text(stringResource(R.string.cuenta_recuperar_codigo_enviado, estado.correo.trim()), style = MaterialTheme.typography.bodyLarge)
            Campo(estado.codigo, viewModel::cambiarCodigo, stringResource(R.string.cuenta_codigo), KeyboardType.NumberPassword)
            Campo(
                estado.contrasenaNueva,
                viewModel::cambiarContrasenaNueva,
                stringResource(R.string.cuenta_contrasena_nueva),
                KeyboardType.Password,
                secreto = true,
            )
            MensajeDeError(estado.error)
            BotonPrincipal(stringResource(R.string.cuenta_restablecer), estado.enviando, onClick = viewModel::restablecer)
        }
    }
}

/** Se muestra sobre cualquier pantalla cuando la API cerró la sesión desde fuera. */
@Composable
fun AvisoSesionTerminada(viewModel: AvisoSesionViewModel = hiltViewModel()) {
    val mostrar by viewModel.mostrar.collectAsStateWithLifecycle()
    if (!mostrar) return
    AlertDialog(
        onDismissRequest = viewModel::entendido,
        title = { Text(stringResource(R.string.cuenta_sesion_terminada_titulo)) },
        text = { Text(stringResource(R.string.cuenta_sesion_terminada_explicacion)) },
        confirmButton = { TextButton(onClick = viewModel::entendido) { Text(stringResource(R.string.cuenta_entendido)) } },
    )
}

/** Quitar los anuncios con un pago único y, donde la ley lo exige, cambiar el consentimiento (ADR-0011). */
@Composable
private fun TarjetaDeAnuncios() {
    val control = LocalControlDeAnuncios.current
    val estado by control.estado.collectAsStateWithLifecycle()
    val opcionesDePrivacidad by control.opcionesDePrivacidadRequeridas.collectAsStateWithLifecycle()
    Tarjeta(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.cuenta_anuncios_titulo), style = MaterialTheme.typography.titleMedium)
        val explicacion =
            when (estado) {
                EstadoQuitarAnuncios.Comprado -> R.string.cuenta_anuncios_comprado
                EstadoQuitarAnuncios.Pendiente -> R.string.cuenta_anuncios_pendiente
                EstadoQuitarAnuncios.NoDisponible -> R.string.cuenta_anuncios_no_disponible
                else -> R.string.cuenta_anuncios_explicacion
            }
        Text(stringResource(explicacion), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        (estado as? EstadoQuitarAnuncios.Disponible)?.let { disponible ->
            BotonPrincipal(stringResource(R.string.cuenta_anuncios_quitar, disponible.precio), enviando = false, onClick = control::comprar)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (estado != EstadoQuitarAnuncios.Comprado) {
                TextButton(onClick = control::restaurar) { Text(stringResource(R.string.cuenta_anuncios_restaurar)) }
            }
            if (opcionesDePrivacidad) {
                TextButton(onClick = control::abrirOpcionesDePrivacidad) { Text(stringResource(R.string.cuenta_anuncios_privacidad)) }
            }
        }
    }
}

/** Activar o apagar el bloqueo pide la huella o el PIN, para que solo el dueño lo cambie (ADR-0006). */
@Composable
private fun TarjetaDeSeguridad(
    activo: Boolean,
    onCambiar: (Boolean) -> Unit,
) {
    val autenticador = LocalAutenticador.current
    var sinBloqueoDePantalla by remember { mutableStateOf(false) }
    val titulo = stringResource(if (activo) R.string.cuenta_bloqueo_confirmar_apagar else R.string.cuenta_bloqueo_confirmar_activar)
    val cambiar = { nuevo: Boolean ->
        if (autenticador.disponible()) {
            sinBloqueoDePantalla = false
            autenticador.autenticar(titulo) { onCambiar(nuevo) }
        } else {
            sinBloqueoDePantalla = true
        }
    }
    Tarjeta(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.cuenta_bloqueo_titulo), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.cuenta_bloqueo_explicacion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = activo, onCheckedChange = cambiar)
        }
        if (sinBloqueoDePantalla) {
            Text(
                stringResource(R.string.cuenta_bloqueo_sin_bloqueo_de_pantalla),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
