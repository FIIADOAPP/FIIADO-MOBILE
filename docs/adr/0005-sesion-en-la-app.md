# 0005. Cuenta y sesión en la app

- Estado: aceptada
- Fecha: 2026-11-16

## Contexto

La app funciona completa sin cuenta. La cuenta (ADR-0002 de FIIADO-API) se crea o se abre desde el menú y activa el respaldo de la Fase 3. La app tiene que hablar con la API, guardar los tokens de forma segura, mantener la sesión sin conexión y renovarla sola, y reaccionar cuando la cuenta se abre en otro teléfono.

## Decisión

### Cliente HTTP

- **Retrofit 3 sobre OkHttp 5**, con kotlinx.serialization, que el proyecto ya usa. Es el estándar en Android, y OkHttp resuelve la renovación de tokens con un `Authenticator`.
- Las llamadas viven en `core:data`. El dominio solo ve un puerto `RepositorioCuenta`, cuyos resultados usan los mismos códigos de error estables de la API (`credenciales_invalidas`, `sesion_activa_en_otro_dispositivo`...).

### Tokens

- Se guardan en **DataStore**, cifrados con **AES-256-GCM** usando una clave del **Android Keystore** que no sale del hardware. No se usa `EncryptedSharedPreferences`, que está obsoleta.
- El respaldo de Android ya está desactivado (Fase 0), así que los tokens no viajan a la nube de Google.

### Renovación y fin de sesión

- Un `Authenticator` de OkHttp intercepta el `401 token_vencido`, renueva una sola vez aunque haya varias peticiones a la vez, y repite la petición.
- Si la API responde `sesion_reemplazada` o `sesion_invalida`, la app borra los tokens, avisa al usuario ("Tu cuenta se abrió en otro teléfono") y vuelve al modo sin cuenta. **Los datos locales no se borran.**
- Sin conexión, la sesión simplemente espera: nada caduca en el teléfono.

### Dirección de la API

- `BuildConfig.API_URL`, definida por tipo de build:
  - **debug**: `http://10.0.2.2:8000/`, que es tu PC vista desde el emulador. Se puede cambiar sin tocar código con la propiedad de Gradle `fiado.apiUrl`, por ejemplo `http://192.168.1.20:8000/` para un teléfono en el mismo wifi.
  - **release**: la URL pública por HTTPS, que se define al desplegar (Fase 5).
- HTTP sin cifrar (cleartext) se permite **solo en debug**, mediante una configuración de seguridad de red propia de ese build.

### Pantallas

- Menú de la lista → "Cuenta": muestra la cuenta abierta (correo y negocio) con "Cerrar sesión", o, sin cuenta, las opciones **Crear cuenta** e **Iniciar sesión**.
- Flujos: crear cuenta → código de verificación; iniciar sesión → si hay otro teléfono activo, un diálogo explica que lo no sincronizado allá se perderá y ofrece **Usar este teléfono**; "¿Olvidaste tu contraseña?" → código → contraseña nueva.

## Consecuencias

- La app depende de Retrofit, OkHttp y DataStore; las pruebas usan MockWebServer para simular la API con las respuestas reales del contrato.
- Robar el archivo de DataStore no basta para obtener los tokens: sin la clave del Keystore del mismo teléfono no se descifran.
- Probar con un teléfono físico requiere que la API escuche en la red local y pasar `fiado.apiUrl`.
- Si el usuario borra los datos de la app, pierde la sesión (y los datos locales no respaldados); es el comportamiento esperado de Android.
