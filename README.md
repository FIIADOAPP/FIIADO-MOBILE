# Fiado

[![CI](https://github.com/FIIADOAPP/FIIADO-MOBILE/actions/workflows/ci.yml/badge.svg?branch=develop)](https://github.com/FIIADOAPP/FIIADO-MOBILE/actions/workflows/ci.yml)

Libreta de fiado para bodegas pequeñas del Perú. Reemplaza el cuaderno donde el bodeguero anota quién le debe y cuánto: suma sola, recuerda los cobros por WhatsApp y funciona completa sin internet.

Es una app Android nativa con los datos en el teléfono (Room como fuente de verdad). El servidor, [FIIADO-API](https://github.com/FIIADOAPP/FIIADO-API), solo respalda y restaura.

- Plan por fases: [docs/roadmap.md](docs/roadmap.md)
- Decisiones de arquitectura: [docs/adr](docs/adr/README.md)

## Capturas

<p>
  <img src="docs/capturas/lista.png" alt="Lista de clientes con su saldo, lo pendiente por cobrar y los botones Fiar y Cobrar" width="240">
  <img src="docs/capturas/movimientos-dialogo.png" alt="Registro de un fiado con el monto enfocado" width="240">
  <img src="docs/capturas/cobro-metodos.png" alt="Cobro con el método preseleccionado" width="240">
  <img src="docs/capturas/historial.png" alt="Historial de un cliente con un pago anulado" width="240">
  <img src="docs/capturas/resumen.png" alt="Resumen de hoy y del mes con lo cobrado por método" width="240">
  <img src="docs/capturas/metodos-lista.png" alt="Métodos de cobro propios" width="240">
</p>

## Estado

| Módulo | Estado |
|---|---|
| Esqueleto (Compose, Hilt, módulos, lint) | Listo |
| Clientes (lista con saldo, búsqueda, alta, edición, archivado) | Listo |
| Movimientos: fiar y cobrar en dos toques, deshacer al instante | Listo |
| Métodos de cobro propios, con el método obligatorio en cada pago | Listo |
| Historial por cliente con anulación, y resumen de hoy y del mes con lo cobrado por método | Listo |
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

```bash
./gradlew connectedDebugAndroidTest
```

Ejecuta las pruebas instrumentadas en el dispositivo o emulador conectado. Verifican lo que Robolectric no puede emular, como el modo de journal real de SQLite. No corren en el CI.

## Estructura

| Módulo | Contenido |
|---|---|
| `app` | Punto de entrada: tema, navegación entre funcionalidades y configuración de Hilt. |
| `core:domain` | Modelo, reglas del negocio y contratos de repositorio. Kotlin puro, sin dependencias de Android. |
| `core:data` | Room como fuente de verdad. Implementa los contratos del dominio. |
| `core:ui` | Componentes de interfaz compartidos entre funcionalidades. |
| `core:testing` | Repositorios falsos en memoria para las pruebas de los ViewModels. |
| `feature:clientes` | Pantallas y ViewModels de clientes. |
| `feature:movimientos` | Registro de fiados y pagos, historial por cliente, resumen y métodos de cobro. |
