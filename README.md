# Fiado

[![CI](https://github.com/FIIADOAPP/FIIADO-MOBILE/actions/workflows/ci.yml/badge.svg?branch=develop)](https://github.com/FIIADOAPP/FIIADO-MOBILE/actions/workflows/ci.yml)

Libreta de fiado para bodegas pequeñas del Perú. Reemplaza el cuaderno donde el bodeguero anota quién le debe y cuánto: suma sola, recuerda los cobros por WhatsApp y funciona completa sin internet.

Es una app Android nativa con los datos en el teléfono (Room como fuente de verdad). El servidor, [FIIADO-API](https://github.com/FIIADOAPP/FIIADO-API), solo respalda y restaura.

- Plan por fases: [docs/roadmap.md](docs/roadmap.md)
- Decisiones de arquitectura: [docs/adr](docs/adr/README.md)

## Capturas

<p>
  <img src="docs/capturas/clientes-lista.png" alt="Lista de clientes con su saldo" width="240">
  <img src="docs/capturas/clientes-editar.png" alt="Edición de un cliente con la opción de archivar" width="240">
  <img src="docs/capturas/clientes-nuevo-error.png" alt="Validación al crear un cliente sin nombre" width="240">
</p>

## Estado

| Módulo | Estado |
|---|---|
| Esqueleto (Compose, Hilt, módulos, lint) | Listo |
| Clientes (lista con saldo, búsqueda, alta, edición, archivado) | Listo |
| Movimientos (fiar, cobrar, anular) | Pendiente |
| Historial y resumen | Pendiente |
| Inicio de sesión | Pendiente |
| Sincronización y restauración | Pendiente |
| Recordatorio por WhatsApp | Pendiente |
| Bloqueo con huella | Pendiente |

## Requisitos

- JDK 21
- Android SDK con la plataforma 37
- Node.js 20 o superior, solo para el hook de mensajes de commit

## Cómo ejecutarlo

```bash
npm install
```

Instala commitlint y activa el hook `commit-msg`. Un mensaje que no siga Conventional Commits, o que incluya líneas de coautoría, se rechaza.

```bash
./gradlew installDebug
```

Compila e instala la app en el dispositivo o emulador conectado.

```bash
./gradlew spotlessCheck lint test
```

Ejecuta ktlint, Android Lint y las pruebas. `./gradlew spotlessApply` corrige el formato.

## Estructura

| Módulo | Contenido |
|---|---|
| `app` | Punto de entrada: tema, navegación entre funcionalidades y configuración de Hilt. |
| `core:domain` | Modelo, reglas del negocio y contratos de repositorio. Kotlin puro, sin dependencias de Android. |
| `core:data` | Room como fuente de verdad. Implementa los contratos del dominio. |
| `feature:clientes` | Pantallas y ViewModels de clientes. |
