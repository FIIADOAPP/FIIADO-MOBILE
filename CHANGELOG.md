# Changelog

Todos los cambios notables de este proyecto se documentan en este archivo.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto usa [versionado semántico](https://semver.org/lang/es/).

## [Sin publicar]

### Agregado

- ADR-0002 sobre el modelo de datos: identificadores UUID v7, dinero en céntimos, movimientos inmutables y saldo calculado.
- Dominio de la libreta en Kotlin puro: `Dinero` con conversión desde texto, teléfono celular peruano, cliente con archivado solo en saldo cero, movimientos con registro y anulación, saldo y resumen por periodo.
- Persistencia local con Room: clientes con su saldo calculado en SQL, búsqueda sin distinguir mayúsculas ni tildes, historial de movimientos y esquema exportado para futuras migraciones.
- Gestión de clientes: lista con el saldo de cada uno, búsqueda, alta, edición y archivado (solo con saldo cero), con validación del nombre y del celular.
- Identificadores UUID v7 generados en el teléfono.
- Registro de fiados y pagos en dos toques desde la lista: monto con el teclado listo, nota opcional y fecha de hoy o de un día pasado.
- Aviso tras cada registro con la opción de deshacerlo, que anula el movimiento.

### Cambiado

- El proyecto compila y prueba con JDK 21, fijado también para el daemon de Gradle.
- Paleta verde completa en el tema claro y el oscuro.

## [0.1.0] - 2026-11-15

### Agregado

- Roadmap del MVP y ADR-0001 sobre el stack y los dos repositorios.
- Validación de mensajes de commit con commitlint y un hook de husky.
- Proyecto Android con los módulos `app`, `core:domain` y `core:data`, Jetpack Compose, Material 3 y Hilt.
- Formato con ktlint mediante Spotless y Android Lint con las advertencias tratadas como errores.
- CI en GitHub Actions: commitlint, ktlint, Android Lint, pruebas y build en cada pull request.

[Sin publicar]: https://github.com/FIIADOAPP/FIIADO-MOBILE/compare/v0.1.0...develop
[0.1.0]: https://github.com/FIIADOAPP/FIIADO-MOBILE/releases/tag/v0.1.0
