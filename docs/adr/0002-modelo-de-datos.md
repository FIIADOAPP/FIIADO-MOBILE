# 0002. Modelo de datos de la libreta

- Estado: aceptada
- Fecha: 2026-11-15

## Contexto

Los clientes y los movimientos se crean en el teléfono, casi siempre sin conexión, y más tarde se suben al servidor. También se restauran completos en otro teléfono. Por eso:

- un registro necesita un identificador estable desde el momento en que se crea, antes de que el servidor lo conozca;
- subir dos veces el mismo cambio no puede duplicar nada;
- un saldo equivocado es el peor error posible para un bodeguero, así que ni los decimales ni las ediciones pueden descuadrarlo.

## Decisión

### Identificadores

- Cada cliente y cada movimiento tiene un UUID v7 generado en el teléfono al crearlo. Es su identificador definitivo, también en el servidor: nunca se reasigna ni se traduce.
- Se usa la versión 7 porque ordena por fecha de creación, lo que mantiene compactos los índices en SQLite y en PostgreSQL.

### Dinero

- Los montos son enteros en céntimos de sol (`Long`), envueltos en un tipo `Dinero` del dominio. No existe ningún `Double`, `Float` ni `BigDecimal` en el camino del dinero.
- Lo que escribe el usuario ("12.5", "12,50") se convierte a céntimos analizando el texto, sin pasar por punto flotante. Se muestra como "S/ 12.50".
- Una sola moneda: soles.

### Movimientos

- Un movimiento tiene: `id`, `clienteId`, `tipo` (`FIADO` o `PAGO`), `monto` (mayor que cero; el signo lo da el tipo), `nota` opcional (hasta 140 caracteres), `fecha` (el día en que ocurrió), `registradoEn` (instante en que se guardó) y `anuladoEn` (vacío mientras esté vigente).
- Un movimiento es inmutable. Para corregir un error se anula y se registra uno nuevo. Anular solo puede ir de vigente a anulado, nunca al revés, así que dos versiones del mismo movimiento siempre se combinan sin ambigüedad: gana la anulada.
- Los movimientos no se borran. El anulado sigue visible en el historial, tachado.

### Saldo

- El saldo de un cliente es la suma de sus fiados vigentes menos la suma de sus pagos vigentes. Se calcula siempre con una consulta y nunca se guarda.
- Un saldo positivo es deuda. Un saldo negativo es saldo a favor del cliente.

### Clientes

- Un cliente tiene: `id`, `nombre` (obligatorio, de 1 a 60 caracteres, sin espacios sobrantes), `telefono` opcional, `creadoEn`, `actualizadoEn` y `archivadoEn`.
- El teléfono es un celular peruano. Se guarda normalizado como `+519XXXXXXXX`, sin importar si se escribió con espacios, guiones o el prefijo 51. Un número que no sea un celular peruano válido se rechaza.
- Dos clientes pueden llamarse igual. Se distinguen por su `id`.
- Los clientes no se borran: se archivan. **Solo se puede archivar un cliente con saldo cero**, para que ninguna deuda desaparezca de la vista. Un cliente archivado no admite movimientos nuevos y se puede desarchivar.

### Fechas

- `fecha` es una fecha local, sin hora, elegida por el usuario: hoy por defecto, cualquier día pasado permitido y ninguno futuro. "Hoy" y "este mes" se calculan con la zona horaria `America/Lima`.
- `registradoEn`, `creadoEn`, `actualizadoEn` y `archivadoEn` son instantes en UTC. Ordenan los registros del mismo día, pero no deciden ningún conflicto: el reloj del teléfono puede estar mal.

## Consecuencias

- La sincronización no necesita traducir identificadores. Reenviar un registro es idempotente: el servidor lo reconoce por su `id`.
- Ningún conflicto puede alterar un saldo. Los movimientos nunca cambian, salvo la anulación, que solo avanza en un sentido, y el saldo se recalcula a partir de ellos.
- El historial completo, errores incluidos, queda como registro de lo que pasó. La base de datos crece sin borrar nada; para el volumen de una bodega no es un problema.
- Un fiado equivocado obliga a anularlo y registrar otro, en lugar de editarlo. La interfaz debe hacer que eso tome pocos toques.
- Archivar un cliente que aún debe exige registrar antes el pago o anular sus movimientos.
