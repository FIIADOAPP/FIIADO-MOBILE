# 0006. Bloqueo de la app con la huella o el PIN del teléfono

- Estado: aceptada
- Fecha: 2026-11-20

## Contexto

La libreta muestra cuánto debe cada vecino. El teléfono suele quedar sobre el mostrador o en manos de un ayudante, así que el bodeguero puede querer que nadie más la abra. Al planificar el MVP se decidió no tener un PIN propio de la app: usar el bloqueo que el teléfono ya tiene (huella, rostro, PIN, patrón o contraseña), que el bodeguero ya recuerda y que no hace falta recuperar.

El bloqueo no debe dejar a nadie fuera de su propia libreta, ni interrumpir cada vez que sale un momento a WhatsApp para enviar un recordatorio. La app funciona desde Android 8 (API 26).

## Decisión

### Cuándo se pide

- El bloqueo es **opcional y viene apagado**. Se activa y se desactiva desde el menú. Las dos acciones piden autenticarse una vez para confirmar que quien lo cambia es el dueño.
- Con el bloqueo activo, se pide al abrir la app y al volver a ella después de **1 minuto o más en segundo plano**. Una salida corta, como enviar un recordatorio por WhatsApp, no lo vuelve a pedir.
- El tiempo se mide con el reloj monótono del sistema (`elapsedRealtime`), no con la hora del teléfono, que se puede cambiar.

### Cómo se pide

- `BiometricPrompt` de AndroidX con `BIOMETRIC_WEAK | DEVICE_CREDENTIAL`: acepta cualquier biometría del teléfono y siempre ofrece el PIN, patrón o contraseña como alternativa. Esa combinación funciona en todas las versiones desde la API 26. No hace falta biometría fuerte, porque el bloqueo no protege una clave criptográfica: solo controla quién ve la pantalla.
- Mientras la app está bloqueada, una pantalla propia cubre todo y no se ve nada de la libreta. Si el usuario cancela, la pantalla queda con un botón para intentar de nuevo.
- Con el bloqueo activo, la ventana usa `FLAG_SECURE`, así la vista previa de apps recientes no muestra saldos.

### Casos límite

- Solo se puede activar si el teléfono tiene un bloqueo de pantalla configurado. Si no lo tiene, la app explica que hay que configurarlo en los ajustes de Android.
- Si el bloqueo está activo y el teléfono deja de tener bloqueo de pantalla, la app se desactiva sola en lugar de quedar inaccesible, y lo avisa una vez.
- La preferencia vive solo en este teléfono, en DataStore, y no se respalda en el servidor.
- El envío en segundo plano (WorkManager) sigue funcionando con la app bloqueada: no muestra nada en pantalla.

## Consecuencias

- No hay un PIN propio que recordar ni un flujo para recuperarlo. La seguridad es la del teléfono: quien conoce su PIN puede abrir la libreta.
- Con el bloqueo activo no se pueden tomar capturas de pantalla de la app, por `FLAG_SECURE`. Es el precio de no mostrar saldos en apps recientes.
- La base de datos local no se cifra; eso sigue fuera del MVP. El bloqueo protege la pantalla, no el archivo.
- Volver a la app antes de un minuto no pide nada: es una ventana corta en la que alguien podría verla si el teléfono quedó desbloqueado.
