# 0007. Plazos de pago y puntualidad de los clientes

- Estado: aceptada
- Fecha: 2026-11-20

## Contexto

El bodeguero decide a quién fiarle por intuición. Quiere saber quién paga a tiempo, cuánto demora cada cliente en promedio y recibir una advertencia antes de fiarle otra vez a alguien que suele atrasarse. Por ejemplo: si le fía a Juan "de hoy para mañana", un día de atraso es normal; dos o más ya son una señal.

Hoy un fiado no tiene fecha de pago, y un pago no se asocia a ningún fiado: los dos solo mueven el saldo (ADR-0002). Los movimientos son inmutables salvo la anulación, y se sincronizan como fotos completas que se unen por `id` (ADR-0003 de FIIADO-API).

Decisiones ya tomadas con el dueño del producto:

- El plazo es opcional al fiar.
- El semáforo tiene tres niveles más "sin historial", con un día de atraso tolerado.
- Un plazo se puede extender, pero solo hacia adelante, y el atraso que ya ocurrió sigue contando.
- La función entra antes de la v1.0.

## Decisión

### Plazo al fiar

- Un fiado puede llevar `vence`, una fecha sin hora, igual o posterior a su `fecha`. Un pago nunca la lleva.
- Al fiar, el campo "¿Para cuándo?" ofrece atajos: *Mañana*, *En 3 días*, *En una semana*, *Fin de mes* y *Otra fecha*. Por defecto queda sin plazo.
- `vence` es un campo más del fiado y, como el resto, no cambia después de guardarlo.

### Prórrogas

- "Dar más plazo" crea una **prórroga**: un registro propio, inmutable, con `id`, `movimientoId`, `vence` y `registradaEn`. Su `vence` debe ser posterior al plazo vigente.
- El plazo vigente de un fiado es el `vence` de su prórroga más reciente o, si no tiene, el original.
- Una prórroga se sincroniza como una entidad más. Como nunca cambia, las copias se unen por `id` sin conflictos. Anular el fiado deja sin efecto sus prórrogas.
- Solo se puede prorrogar un fiado vigente con plazo que todavía no esté pagado.

### A qué fiado paga cada pago

- Los pagos de un cliente cubren sus fiados **del más antiguo al más reciente**, igual que en un cuaderno. Los fiados se ordenan por `fecha`, `registradoEn` e `id`, y los pagos, igual. Solo cuentan los movimientos vigentes.
- Un fiado queda **pagado** el día (`fecha`) del pago que completa su monto. Si el cliente tenía saldo a favor, un fiado nuevo queda pagado el mismo día.
- Todo se calcula en el teléfono a partir de los movimientos y las prórrogas. Nada de esto se guarda ni se sincroniza: se recalcula si se anula un movimiento.

### Cómo se mide

- **Días para pagar** de un fiado pagado: los días desde su `fecha` hasta el día en que quedó pagado. Sirve aunque no tenga plazo.
- **Atraso** de un fiado con plazo: los días después del plazo vigente en que siguió sin pagarse, hasta que se pagó (o hasta hoy, si sigue pendiente). Con prórrogas, se suman los días de atraso de cada plazo hasta que se registró la siguiente prórroga. Así, prorrogar un fiado que ya estaba vencido no borra el atraso acumulado.
- Por cliente, con sus **últimos 10 fiados pagados**, para reflejar cómo paga ahora:
  - *Tiempo promedio de pago*: el promedio de días para pagar.
  - *Atraso promedio*: el promedio del atraso de los que tenían plazo.
  - *A tiempo*: el porcentaje de los que tenían plazo y se pagaron con hasta 1 día de atraso.
- **Vencido**: un fiado con plazo, sin pagar y con el plazo ya pasado. Se muestran los días y el monto pendiente.

### Semáforo

| Nivel | Cuándo |
|---|---|
| ⚪ Sin historial | Menos de 2 fiados con plazo ya pagados y nada vencido. No se juzga. |
| 🔴 Cuidado | Atraso promedio de 7 días o más, o algo vencido hace 7 días o más. |
| 🟡 Se demora | Atraso promedio de 2 a 6 días, o algo vencido hace 2 a 6 días. |
| 🟢 Puntual | En los demás casos: paga a tiempo o con hasta 1 día de atraso. |

Se evalúa de arriba abajo; lo vencido cuenta aunque el cliente no tenga historial.

### Dónde se ve y qué recomienda

- **Lista de clientes**: el semáforo y una etiqueta con lo más urgente: "Vence hoy", "Vence mañana" o "Vencido hace 3 días".
- **Historial**: una tarjeta "Cómo paga" con el nivel, el tiempo promedio de pago, el porcentaje a tiempo y lo vencido. Cada fiado muestra su plazo, y los pendientes ofrecen "Dar más plazo".
- **Resumen**: una sección "Cómo te pagan" con cuántos clientes hay en cada nivel, el tiempo promedio de pago de todo el negocio y la lista de quienes están en Cuidado.
- **Al fiar**:
  - A un cliente en Cuidado, un aviso que no bloquea, con los números: "Juan suele pagar 5 días tarde y tiene S/ 40 vencidos desde hace 9 días. Considera cobrar lo pendiente antes de fiarle más."
  - Con 2 o más fiados pagados, un plazo sugerido según su costumbre: "Juan suele pagar en 4 días".
- Las recomendaciones son texto informativo: la app nunca impide fiar.

### Datos y sincronización

- **Teléfono**: base de datos a la versión 5. `movimientos` gana `vence` (días desde la época, nulo) y se crea la tabla `prorrogas`. Las fotos de la cola incluyen `vence` y la nueva entidad `prorroga`.
- **FIIADO-API v0.4.0**:
  - `movimiento` acepta `vence`, una fecha opcional que es inmutable como el resto de sus campos. Si falta, vale nulo, así una app anterior sigue sincronizando.
  - Nueva entidad `prorroga`, con referencia al movimiento.
  - La restauración entrega métodos de cobro, clientes, movimientos y, al final, prórrogas.
- **Orden de despliegue**: primero la API. Una app nueva contra una API anterior tendría el envío rechazado y el respaldo quedaría detenido, como prevé el ADR-0003, hasta actualizar la API.

## Consecuencias

- El bodeguero ve quién le paga a tiempo sin anotar nada nuevo, salvo el plazo cuando quiera usarlo. El tiempo promedio de pago funciona incluso sin plazos.
- La regla "del más antiguo al más reciente" es una convención: si un cliente pagó un fiado reciente "a propósito", la app lo contará contra el más antiguo. Es lo esperable en un cuaderno y mantiene los cálculos simples y repetibles.
- Prorrogar un fiado vencido no borra el atraso acumulado, así que el semáforo no se puede "limpiar" dando más plazo.
- Calcular todo en el teléfono recorre los movimientos de cada cliente. Para el volumen de una bodega es inmediato, y no agrega datos derivados que sincronizar ni que puedan quedar desactualizados.
- La v1.0 se retrasa unas entregas, y la app nueva exige FIIADO-API v0.4.0 o superior para el respaldo.
- Quedan fuera: notificaciones del teléfono cuando algo vence, límite de crédito por cliente y `{vence}` en la plantilla del recordatorio.
