# Roadmap de Fiado

Libreta de fiado para bodegas pequeñas del Perú. La usa una sola persona, el dueño, de pie y muchas veces sin internet. Las dos prioridades son registrar un movimiento en dos toques y funcionar completa sin conexión.

## Decisiones de alcance

| Tema | Decisión |
|---|---|
| Usuarios | Una sola persona por cuenta, un negocio por cuenta. |
| Dispositivos | Un solo dispositivo activo por cuenta. Iniciar sesión en otro teléfono cede la sesión: el anterior deja de sincronizar. El servidor funciona como respaldo y restauración. |
| Registrar | Tocar "Fiar" o "Cobrar", escribir el monto y tocar "Guardar". Nota opcional. Fecha por defecto hoy; se permiten fechas pasadas, no futuras. |
| Movimientos | Inmutables. No se editan; se anulan, y el anulado queda visible en el historial. |
| Clientes | Se archivan, no se borran. |
| Pago mayor a la deuda | Se permite y queda como saldo a favor del cliente. La app registra lo que pasó; no lo impide. |
| Métodos de cobro | Lista propia (crear, renombrar, archivar); empieza con Efectivo, Yape y Plin. Cada pago lleva un método, preseleccionado con el último usado. |
| Resumen | "Hoy" y "Este mes": lo fiado y lo cobrado en el periodo, en hora de Lima. "Total": deuda pendiente actual. |
| Montos | Céntimos como enteros (`Long`), nunca decimales. |
| Bloqueo | `BiometricPrompt` con huella o, si no hay, el bloqueo del propio teléfono. Sin PIN propio de la app. Protege la pantalla, no cifra los datos. |
| Cuenta | Opcional: la app funciona completa sin cuenta y la cuenta activa el respaldo. Se crea con correo, contraseña y nombre del negocio; el correo se verifica con un código y la contraseña se recupera con otro código, ambos enviados por correo (SMTP). |
| WhatsApp | Enlace `wa.me` con el número normalizado a +51 y el mensaje con el monto ya escrito. |
| Firma del APK | Keystore en GitHub Secrets; el APK firmado se genera en CI. Nunca se sube al repositorio. |

Fuera del MVP: inventario, varios empleados, varios dispositivos activos a la vez, pagos en línea, reportes en PDF, iOS y cifrado de la base local.

## Repositorios

| Repositorio | Contenido |
|---|---|
| [FIIADO-MOBILE](https://github.com/FIIADOAPP/FIIADO-MOBILE) | App Android, este roadmap, ADR de la app. |
| [FIIADO-API](https://github.com/FIIADOAPP/FIIADO-API) | API, Docker Compose, ADR del servidor (autenticación y protocolo de sincronización). |

El contrato entre ambos es la especificación OpenAPI de la API. Cada repositorio tiene sus propias versiones, `CHANGELOG.md`, etiquetas y releases. El README de la app indica qué versión de la API necesita.

## Flujo de trabajo

- `main` solo recibe merges de versiones; `develop` es la rama de integración.
- Cada entrega es una rama `tipo/descripcion-corta` desde `develop`, que entra por pull request con el CI en verde.
- Al cerrar una fase: pull request de `develop` a `main`, etiqueta `vX.Y.Z`, release en GitHub con las notas del changelog y `develop` al día con `main`.

## Fases

Cada entrega indica su repositorio: **M** (mobile) o **A** (API).

### Fase 0: Cimientos → `v0.1.0`

| Rama | Repo | Entrega |
|---|---|---|
| `docs/roadmap-y-adr-0001` | M | Este roadmap y el ADR-0001 (stack y dos repositorios). |
| `chore/commitlint` | M, A | commitlint con hook de `commit-msg`. |
| `chore/esqueleto-android` | M | Módulos `app`, `core:domain` (Kotlin puro), `core:data`, `feature:*`; catálogo de versiones; ktlint y Android Lint; minSdk 26. |
| `chore/esqueleto-api` | A | FastAPI con endpoint de salud, Docker Compose con Postgres, Alembic, ruff, mypy, pytest, `.env.example` y ADR-0001 de la API. |
| `chore/configurar-ci` | M, A | GitHub Actions: lint, pruebas y commitlint en cada pull request. |

### Fase 1: Libreta local sin red → `v0.2.0`

| Rama | Repo | Entrega |
|---|---|---|
| `docs/adr-modelo-de-datos` | M | IDs UUID generados en el teléfono, céntimos, movimientos inmutables, saldo calculado, archivado. |
| `feat/dominio-libreta` | M | `Dinero`, `Cliente`, `Movimiento`, reglas de saldo y resumen, con pruebas unitarias. |
| `feat/persistencia-room` | M | Entidades, DAOs y repositorios, probados con Robolectric. |
| `feat/clientes` | M | Lista, búsqueda, crear, editar, archivar y saldo por cliente. |
| `feat/registro-de-movimientos` | M | Fiar o cobrar en dos toques, y deshacer al instante. |
| `fix/durabilidad-sqlite` | M | ADR-0003: journal clásico para que una escritura confirmada sobreviva a un apagado. |
| `feat/metodos-de-cobro` | M | ADR-0004: métodos de cobro propios (Efectivo, Yape y Plin de inicio) y método obligatorio en cada pago. |
| `feat/historial-y-resumen` | M | Historial por cliente con anulación y método de cada pago; resumen de hoy, el mes y el total, con lo cobrado por método. |

### Fase 2: Cuenta → API `v0.2.0`, app `v0.3.0`

| Rama | Repo | Entrega |
|---|---|---|
| `docs/adr-autenticacion` | A | ADR-0002: tokens de acceso y de refresco con rotación, argon2id, códigos por correo, dispositivo activo. |
| `feat/registro-y-verificacion` | A | Registro con nombre del negocio, envío de códigos por SMTP (Mailpit en local y en el CI) y verificación del correo. |
| `feat/sesiones` | A | Inicio de sesión, refresco con rotación y detección de reutilización, cesión del dispositivo activo y cierre de sesión. |
| `feat/recuperar-contrasena` | A | Pedir y usar un código para restablecer la contraseña, cerrando todas las sesiones. |
| `feat/cuenta` | M | Crear cuenta, verificar el correo, iniciar sesión (con aviso al ceder el dispositivo) y recuperar la contraseña desde el menú; tokens guardados de forma segura; la sesión se mantiene sin conexión. |

### Fase 3: Sincronización → API `v0.3.0`, app `v0.4.0`

| Rama | Repo | Entrega |
|---|---|---|
| `docs/adr-sincronizacion` | A | Cola de pendientes, envíos idempotentes con ID por operación, restauración paginada con cursor, recuperación de cortes y cesión del dispositivo. |
| `feat/sincronizacion` | A | Endpoints para subir cambios, restaurar y ceder el dispositivo, con pruebas de integración. |
| `feat/cola-de-pendientes` | M | La entidad y su operación pendiente se escriben en la misma transacción de Room (con migración). |
| `feat/sincronizador` | M | WorkManager con restricción de red y reintentos con espera creciente. |
| `feat/restaurar-al-iniciar-sesion` | M | Al iniciar sesión, descarga la libreta del servidor de forma reanudable, la une con la local sin borrar nada y sube lo que falta. El teléfono queda ligado a esa cuenta. |
| `test/sincronizacion-casos-dificiles` | M, A | Sin conexión, corte a la mitad (enviado pero sin confirmación), reenvío duplicado, restauración interrumpida y sesión reemplazada. |
| `feat/estado-de-sincronizacion` | M | Indicador de pendientes y aviso al cerrar sesión o al ceder el dispositivo con cambios sin enviar, que se conservan en el teléfono. |

### Fase 4: Cobro, seguridad y puntualidad → API `v0.4.0`, app `v0.5.0`

| Rama | Repo | Entrega |
|---|---|---|
| `feat/recordatorio-whatsapp` | M | Mensaje de cobro con el monto ya escrito. |
| `feat/rediseno-oscuro` | M | Diseño oscuro con tarjetas, gráficos en el resumen y barra inferior flotante, sin cambiar la lógica. |
| `feat/bloqueo-biometrico` | M | Bloqueo al abrir y al volver de segundo plano. |
| `docs/adr-plazos` | M | ADR-0007: plazo al fiar, prórrogas, a qué fiado paga cada pago, cómo se mide la puntualidad y el semáforo. |
| `feat/plazos` | A | `vence` en los movimientos y la entidad `prorroga`, con migración y pruebas (API `v0.4.0`). |
| `feat/plazo-al-fiar` | M | "¿Para cuándo?" con atajos al fiar, plazo visible en el historial y migración a la versión 5. |
| `feat/prorrogas` | M | "Dar más plazo" a un fiado pendiente, solo hacia adelante. |
| `feat/puntualidad` | M | Cálculo en el dominio (pagos del más antiguo al más reciente, atraso, semáforo), tarjeta "Cómo paga" y etiquetas de vencimiento en la lista. |
| `feat/como-te-pagan` | M | Sección "Cómo te pagan" en el resumen, aviso al fiar a un cliente en Cuidado y plazo sugerido. |

### Fase 5: Publicación → `v1.0.0` en ambos

| Rama | Repo | Entrega |
|---|---|---|
| `chore/firma-release` | M | APK firmado generado en CI con el keystore de GitHub Secrets. |
| `feat/version-solo-local` | M | ADR-0010: el release sale sin cuenta, la libreta vive en el teléfono y se respalda con la copia de seguridad de Android. |
| `docs/aceptacion` | M | Lista de verificación de la prueba de aceptación, ejecutada, con capturas y README final. |
| `chore/despliegue-api` | A | Opcional: despliegue público de la API para el portafolio. |

## Criterio de terminado

La versión 1.0 guarda la libreta solo en el teléfono ([ADR-0010](adr/0010-version-1-solo-en-el-telefono.md)). La sincronización con el servidor ya está hecha y probada, pero queda para una versión posterior.

1. Instalo el APK release firmado en un teléfono y registro clientes, fiados y pagos en modo avión.
2. Cierro y vuelvo a abrir la app, y todo sigue ahí.
3. Hago una copia de seguridad de Android, desinstalo, reinstalo y la libreta vuelve sin datos perdidos ni duplicados.
4. La app tiene `v1.0.0` publicada, con el AAB y el APK firmados y el CI en verde.
