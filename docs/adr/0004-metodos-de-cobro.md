# 0004. Métodos de cobro

- Estado: aceptada
- Fecha: 2026-11-15

## Contexto

Los bodegueros cobran en efectivo, por Yape, por Plin o por transferencia, y al cerrar el día necesitan saber cuánto entró por cada vía para cuadrar la caja con lo que ven en sus aplicaciones. Registrar un pago debe seguir tomando dos toques. El dato se sincronizará en la Fase 3 y se restaurará en otro teléfono, así que no puede duplicarse.

## Decisión

- **Método de cobro** es una entidad propia: `id` (UUID), `nombre` (de 1 a 30 caracteres, único entre los métodos activos sin distinguir mayúsculas ni tildes), `creadoEn`, `actualizadoEn` y `archivadoEn`. Se puede crear, renombrar y archivar; no se borra, porque los pagos pasados lo referencian.
- Siempre queda al menos un método activo: no se puede archivar el último.
- Cada **pago** lleva obligatoriamente un `metodoCobroId`; un **fiado** nunca lo lleva. Como el movimiento es inmutable (ADR-0002), corregir el método es anular el pago y registrarlo de nuevo.
- Al cobrar, el método viene preseleccionado con el del último pago registrado, o con el primero activo si no hay pagos.
- La app empieza con **Efectivo, Yape y Plin**. Estos tres tienen identificadores fijos, iguales en todos los teléfonos, para que crearlos al instalar y restaurarlos desde el servidor no los duplique.
- La base de datos pasa a la versión 2 con una migración: crea la tabla, inserta los tres métodos iniciales y asigna Efectivo a los pagos ya registrados.
- El resumen de hoy y del mes desglosa lo cobrado por método.

## Consecuencias

- Cerrar caja por método no requiere anotaciones aparte.
- La sincronización tendrá una entidad más. Gracias a los identificadores fijos, los métodos iniciales se reconcilian sin lógica especial.
- Renombrar un método cambia el nombre que se muestra en todos sus pagos pasados; es lo esperado, porque sigue siendo la misma vía de cobro.
- Los pagos registrados antes de la versión 2 quedan como Efectivo. Es una suposición razonable para datos de prueba previos a la primera versión publicada.
