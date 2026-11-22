# Changelog

Todos los cambios notables de este proyecto se documentan en este archivo.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto usa [versionado semántico](https://semver.org/lang/es/).

## [Sin publicar]

### Agregado

- ADR-0011 y publicidad:
  - Banner adaptativo de AdMob encima de la barra inferior, solo en Clientes y Resumen, después del formulario de consentimiento de Google donde la ley lo exige.
  - Compra única «Quitar anuncios» con Google Play Billing desde Cuenta, con «Restaurar compra» y acceso a la privacidad de anuncios. Sin internet, quien ya compró sigue sin anuncios.
  - Los ids de AdMob llegan desde las variables `ADMOB_APP_ID` y `ADMOB_BANNER`; sin ellas se usan los anuncios de prueba de Google.

## [1.0.1] - 2026-11-22

### Corregido

- La barra inferior centra el ícono y el texto de cada destino a la altura del botón central, y reparte los cinco elementos con el mismo espacio.

## [1.0.0] - 2026-11-22

Primera versión para Google Play: la libreta se guarda solo en el teléfono y se respalda con la copia de seguridad de Android (ADR-0010).

### Agregado

- ADR-0010: la versión de la Play Store guarda la libreta solo en el teléfono, sin cuenta ni envíos al servidor; Cuenta explica dónde está la libreta y conserva el bloqueo. La libreta y las preferencias se respaldan con la copia de seguridad de Android (Google Drive y traspaso a un teléfono nuevo), sin la sesión. El build `debug` sigue ofreciendo la cuenta, y `-Pfiado.cuentaRelease=true` la devuelve al release.
- ADR-0009 y release firmado: con la clave de subida en variables de entorno, `bundleRelease` genera el AAB para Google Play. El workflow `Release` adjunta el AAB y el APK firmados a cada release de GitHub, apuntando a la API pública de `API_URL_RELEASE`.

### Cambiado

- La app espera hasta 70 segundos la respuesta de la API, para cubrir el encendido del servidor gratuito.

## [0.5.0] - 2026-11-21

El respaldo de plazos y prórrogas requiere FIIADO-API `v0.4.0` o superior.

### Corregido

- Al iniciar sesión, la restauración ya no reemplaza por la versión del servidor un cliente o un método de cobro con cambios sin subir: se conserva el del teléfono y se sube después, así el teléfono y el servidor quedan iguales (ADR-0008).

### Agregado

- ADR-0007 y plazo de pago al fiar: «¿Para cuándo?» con los atajos Mañana, 3 días, 1 semana, Fin de mes u Otra fecha. El historial muestra el plazo de cada fiado y el plazo se respalda con FIIADO-API `v0.4.0` o superior. La base de datos pasa a la versión 5.
- «Dar más plazo» al tocar un fiado con plazo en el historial: solo permite una fecha posterior al plazo vigente y queda como una prórroga aparte, sin cambiar el fiado. El historial marca los plazos prorrogados. Las prórrogas se respaldan y se restauran; la base de datos pasa a la versión 6.
- Puntualidad de cada cliente (ADR-0007): los pagos cubren primero los fiados más antiguos, y de ahí salen los días que tarda en pagar, su porcentaje de pagos a tiempo, su atraso y lo vencido. El semáforo (Puntual, Se demora, Cuidado o Sin historial) aparece en la lista junto a «Vence mañana» o «Vencido hace 3 días», y el historial suma la tarjeta «Cómo paga» y el estado de cada fiado con plazo.
- «Cómo te pagan» en el resumen (ADR-0007): cuántos clientes hay en cada nivel, cuánto tardan en pagar en promedio y quiénes están en Cuidado, con lo que deben vencido; al tocar uno se abre su historial. Al fiar a un cliente en Cuidado aparece un aviso con sus números que no impide fiar, y con dos o más fiados pagados se sugiere el plazo que suele tardar.
- Recordatorio de cobro por WhatsApp desde el historial de un cliente que debe: abre WhatsApp con el mensaje escrito. Si el cliente no tiene celular, ofrece agregarlo.
- Mensaje de cobro editable desde el menú, con `{cliente}`, `{saldo}` y `{negocio}`, vista previa y opción de restablecer. Por defecto saluda con el nombre del negocio; sin cuenta, esa frase se omite. Se guarda solo en el teléfono.
- ADR-0006 y bloqueo opcional con la huella o el PIN del teléfono, desde Cuenta: se pide al abrir la app y al volver tras un minuto o más en segundo plano. Activarlo o apagarlo pide confirmar con la huella. Mientras está activo, la vista de apps recientes no muestra saldos. Si el teléfono quita su bloqueo de pantalla, el de la app se apaga solo y avisa.
- Barra inferior flotante con Clientes, Resumen, Cobros y Cuenta, y un botón central para crear un cliente. La pantalla Cobros reúne los métodos de cobro y el mensaje de cobro.
- Resumen con un anillo del porcentaje cobrado de lo fiado en el mes y barras con lo cobrado por cada método.

### Cambiado

- «Dar más plazo» solo se ofrece en los fiados con plazo que todavía no se pagan.
- Nuevo diseño oscuro: fondo gris carbón, tarjetas redondeadas, acento lila, verde para lo cobrado y lo que está al día y coral para lo que se debe. La app ya no cambia de tema según el teléfono.
- El menú de la lista de clientes y su botón «Nuevo cliente» dan paso a la barra inferior.

## [0.4.0] - 2026-11-20

El respaldo y la restauración requieren FIIADO-API `v0.3.0` o superior.

### Agregado

- Cola de cambios pendientes de subir: cada cliente, método de cobro o movimiento que se guarda deja, en la misma transacción, una operación con su foto en el formato de FIIADO-API. La base de datos pasa a la versión 3.
- Envío de la cola a FIIADO-API en segundo plano con WorkManager: espera a tener red, sube lotes de hasta 100 operaciones en orden, quita solo lo que el servidor confirma y reintenta con esperas crecientes. Sin sesión, o si otro teléfono la tomó, la cola se conserva.
- Restauración al iniciar sesión, antes de enviar nada: trae la libreta del servidor por páginas, continúa desde la última página guardada si se corta y la une con la local sin borrar nada. Lo que existe en ambos lados queda como en el servidor, salvo la anulación, que se conserva; lo que solo existe en el teléfono se sube. La base de datos pasa a la versión 4.
- El teléfono queda vinculado a la primera cuenta con la que inicia sesión; iniciar sesión con otra se rechaza sin llamar a la API.
- Estado del respaldo en la pantalla de cuenta: todo respaldado, cuántos cambios faltan subir o si el envío se detuvo porque el servidor rechazó un cambio. Al cerrar sesión con cambios pendientes, el aviso dice cuántos son y que se quedan en el teléfono.

### Cambiado

- El aviso al ceder la cuenta ya no dice que lo no sincronizado en el otro teléfono se perderá: se queda en ese teléfono y se sube si vuelve a iniciar sesión.

## [0.3.0] - 2026-11-16

La cuenta requiere FIIADO-API `v0.2.0` o superior.

### Agregado

- ADR-0005 y cuenta opcional desde el menú: crear cuenta con el nombre del negocio, verificar el correo con un código, iniciar sesión (con aviso y confirmación si la cuenta está abierta en otro teléfono) y recuperar la contraseña.
- La sesión se guarda cifrada con una clave del Android Keystore, se renueva sola y se mantiene sin conexión; si se cierra desde fuera, la app avisa y la libreta queda intacta.

## [0.2.0] - 2026-11-16

### Agregado

- ADR-0002 sobre el modelo de datos: identificadores UUID v7, dinero en céntimos, movimientos inmutables y saldo calculado.
- Dominio de la libreta en Kotlin puro: `Dinero` con conversión desde texto, teléfono celular peruano, cliente con archivado solo en saldo cero, movimientos con registro y anulación, saldo y resumen por periodo.
- Persistencia local con Room: clientes con su saldo calculado en SQL, búsqueda sin distinguir mayúsculas ni tildes, historial de movimientos y esquema exportado para futuras migraciones.
- Gestión de clientes: lista con el saldo de cada uno, búsqueda, alta, edición y archivado (solo con saldo cero), con validación del nombre y del celular.
- Identificadores UUID v7 generados en el teléfono.
- Registro de fiados y pagos en dos toques desde la lista: monto con el teclado listo, nota opcional y fecha de hoy o de un día pasado.
- Aviso tras cada registro con la opción de deshacerlo, que anula el movimiento.
- ADR-0004 y métodos de cobro propios: la app empieza con Efectivo, Yape y Plin; se pueden crear, renombrar y archivar desde el menú de la lista.
- Cada pago guarda su método de cobro, preseleccionado con el último usado; el resumen calcula lo cobrado por método.
- Migración de la base de datos a la versión 2: los pagos registrados antes quedan como Efectivo.
- Historial por cliente: fiados y pagos del más reciente al más antiguo, con el método de cada pago; un toque anula un movimiento previa confirmación y queda tachado.
- Resumen de hoy y del mes con lo fiado, lo cobrado y lo cobrado por cada método, y la tarjeta "Por cobrar" en la lista de clientes.

### Cambiado

- El proyecto compila y prueba con JDK 21, fijado también para el daemon de Gradle.
- Paleta verde completa en el tema claro y el oscuro.

### Corregido

- Un pago o fiado registrado ya no puede perderse si el teléfono se apaga sin aviso: la base de datos usa el journal clásico en lugar de WAL (ADR-0003).

## [0.1.0] - 2026-11-15

### Agregado

- Roadmap del MVP y ADR-0001 sobre el stack y los dos repositorios.
- Validación de mensajes de commit con commitlint y un hook de husky.
- Proyecto Android con los módulos `app`, `core:domain` y `core:data`, Jetpack Compose, Material 3 y Hilt.
- Formato con ktlint mediante Spotless y Android Lint con las advertencias tratadas como errores.
- CI en GitHub Actions: commitlint, ktlint, Android Lint, pruebas y build en cada pull request.

[Sin publicar]: https://github.com/FIIADOAPP/FIIADO-MOBILE/compare/v1.0.1...develop
[1.0.1]: https://github.com/FIIADOAPP/FIIADO-MOBILE/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/FIIADOAPP/FIIADO-MOBILE/compare/v0.5.0...v1.0.0
[0.5.0]: https://github.com/FIIADOAPP/FIIADO-MOBILE/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/FIIADOAPP/FIIADO-MOBILE/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/FIIADOAPP/FIIADO-MOBILE/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/FIIADOAPP/FIIADO-MOBILE/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/FIIADOAPP/FIIADO-MOBILE/releases/tag/v0.1.0
