# Fiado

Libreta de fiado para bodegas pequeñas del Perú. Reemplaza el cuaderno donde el bodeguero anota quién le debe y cuánto: suma sola, recuerda los cobros por WhatsApp y funciona completa sin internet.

Es una app Android nativa con los datos en el teléfono (Room como fuente de verdad). El servidor, [FIIADO-API](https://github.com/FIIADOAPP/FIIADO-API), solo respalda y restaura.

- Plan por fases: [docs/roadmap.md](docs/roadmap.md)
- Decisiones de arquitectura: [docs/adr](docs/adr/README.md)

## Capturas

Se agregan cuando existan las primeras pantallas (Fase 1).

## Estado

| Módulo | Estado |
|---|---|
| Esqueleto (Compose, Hilt, módulos, lint) | Listo |
| Clientes | Pendiente |
| Movimientos (fiar, cobrar, anular) | Pendiente |
| Historial y resumen | Pendiente |
| Inicio de sesión | Pendiente |
| Sincronización y restauración | Pendiente |
| Recordatorio por WhatsApp | Pendiente |
| Bloqueo con huella | Pendiente |

## Requisitos

- JDK 17
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
| `app` | Interfaz con Jetpack Compose y Material 3, navegación e inyección con Hilt. |
| `core:domain` | Modelo y reglas del negocio. Kotlin puro, sin dependencias de Android. |
| `core:data` | Persistencia local y sincronización. Implementa los contratos del dominio. |
