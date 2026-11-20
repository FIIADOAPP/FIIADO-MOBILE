# Changelog

Todos los cambios notables de este proyecto se documentan en este archivo.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto usa [versionado semántico](https://semver.org/lang/es/).

## [Sin publicar]

### Agregado

- Cola de cambios pendientes de subir: cada cliente, método de cobro o movimiento que se guarda deja, en la misma transacción, una operación con su foto en el formato de FIIADO-API. La base de datos pasa a la versión 3.

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

[Sin publicar]: https://github.com/FIIADOAPP/FIIADO-MOBILE/compare/v0.3.0...develop
[0.3.0]: https://github.com/FIIADOAPP/FIIADO-MOBILE/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/FIIADOAPP/FIIADO-MOBILE/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/FIIADOAPP/FIIADO-MOBILE/releases/tag/v0.1.0
