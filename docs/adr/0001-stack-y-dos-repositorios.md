# 0001. Stack de la app y dos repositorios

- Estado: aceptada
- Fecha: 2026-11-14

## Contexto

Fiado la usa una sola persona, muchas veces sin internet. Los datos tienen que vivir en el teléfono, y el servidor solo respalda y restaura. El proyecto forma parte de un portafolio cuyo foco es la sincronización sin conexión en una app nativa.

## Decisión

- La app es nativa en Kotlin, con Jetpack Compose y Material 3.
- Room es la única fuente de verdad: la interfaz solo lee de Room y nunca espera a la red para mostrar o guardar algo.
- WorkManager sincroniza en segundo plano cuando hay red. Hilt inyecta dependencias, las corrutinas y Flow manejan la asincronía y Navigation Compose maneja la navegación.
- Capas de dominio, datos y presentación. El dominio es un módulo Gradle de Kotlin puro (`core:domain`), de modo que el compilador impide que dependa de Android.
- El código está en dos repositorios: `FIIADO-MOBILE` (app) y `FIIADO-API` (API en FastAPI con PostgreSQL). El contrato entre ellos es la especificación OpenAPI de la API.

## Consecuencias

- La app funciona completa sin servidor; la red solo afecta al respaldo.
- Cada repositorio tiene su propio CI, `CHANGELOG.md` y versiones. Un cambio en el protocolo de sincronización requiere pull requests coordinados en ambos, y el README de la app declara la versión de la API compatible.
- El CI de la app prueba la sincronización contra un servidor simulado que sigue el contrato. La prueba de punta a punta con la API real es manual y se documenta en la Fase 5.
