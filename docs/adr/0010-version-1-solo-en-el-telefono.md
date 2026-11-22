# 0010. La versión 1.0 guarda la libreta solo en el teléfono

- Estado: aceptada
- Fecha: 2026-11-21

## Contexto

La cuenta y el respaldo en el servidor ya funcionan (ADR-0005 y ADR-0003 de FIIADO-API). Pero para la Play Store exigen tener la API desplegada y mantenida (ADR-0004 de la API), un SMTP real y declarar en Play que la app envía datos de los clientes a un servidor. Para publicar pronto se quiere una primera versión más simple, que funcione solo en el teléfono.

Sin servidor, el bodeguero que pierde o cambia el teléfono pierde su libreta. Android trae una copia de seguridad automática gratuita: guarda los datos de la app, cifrados, en el Google Drive del dueño del teléfono y los devuelve al reinstalar la app o al pasar a un teléfono nuevo. Hoy está apagada (`allowBackup="false"`).

## Decisión

- **Sin cuenta en el release:** la versión 1.0 de la Play Store no ofrece crear una cuenta ni iniciar sesión, y no programa el envío al servidor. El código se conserva, y el build `debug` lo sigue mostrando para el desarrollo.
- **Interruptor:** `BuildConfig.CUENTA_DISPONIBLE`, que se inyecta como `Funciones.cuentaDisponible`.
  - En el build `debug` es `true`, salvo que se pase `-Pfiado.cuenta=false`.
  - En el build `release` es `false`, salvo que se pase `-Pfiado.cuentaRelease=true`.
- **Workflow `Release`:** `API_URL_RELEASE` pasa a ser opcional. Si la variable existe, el release se compila con la cuenta disponible y apunta a esa API.
- **Copia de seguridad de Android:** se activa para la base de datos (`fiado.db`) y las preferencias (`datastore/preferencias.preferences_pb`), tanto en la copia a Google Drive como en el traspaso entre teléfonos. La sesión (`datastore/sesion.preferences_pb`) queda excluida.
- **Pantalla Cuenta:** conserva el bloqueo con la huella o el PIN y explica que la libreta se guarda en el teléfono y en la copia de seguridad de Google.
- **Criterio de terminado de la v1.0:** cambia en el roadmap. Se acepta con la libreta funcionando sin conexión, el release firmado instalado y la libreta recuperada tras reinstalar desde la copia de seguridad. La sincronización con el servidor queda para una versión posterior.

## Consecuencias

- Publicar no depende de Render, Neon ni Brevo, y en Play se declara que la app no envía datos a un servidor propio.
- Volver a ofrecer la cuenta es compilar con `-Pfiado.cuentaRelease=true` y la URL de la API. No hace falta escribir código.
- La copia de Google la decide el dueño del teléfono: debe tener activada la copia de seguridad de Android. Se hace más o menos una vez al día, con wifi y el teléfono cargando, así que lo registrado después de la última copia se puede perder.
- Cada app tiene un límite de 25 MB en esa copia, suficiente para muchos años de una libreta.
- El bloqueo con la huella se restaura en el teléfono nuevo. Si ese teléfono no tiene bloqueo de pantalla, el bloqueo de la app se apaga solo (ADR-0006).
