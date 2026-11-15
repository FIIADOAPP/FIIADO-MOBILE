# 0003. Durabilidad de las escrituras locales

- Estado: aceptada
- Fecha: 2026-11-15

## Contexto

Room abre SQLite en modo WAL por defecto, y Android configura WAL con `synchronous=NORMAL`. Según la documentación de SQLite, en esa combinación una transacción ya confirmada puede deshacerse si el equipo pierde energía antes del siguiente checkpoint. Para una libreta de deudas, perder un pago que el bodeguero vio registrado es inaceptable, y un celular que se apaga por batería es un caso común en una bodega.

Fijar `PRAGMA synchronous = FULL` sobre WAL no es confiable aquí: el ajuste vale por conexión, Room usa varias conexiones y Android las configura por su cuenta al abrirlas.

## Decisión

La base de datos usa el journal clásico en modo `TRUNCATE` en lugar de WAL. En ese modo cada transacción se escribe en disco antes de confirmarse, y Android abre la conexión con `synchronous=FULL`.

## Consecuencias

- Un movimiento que la app mostró como guardado sobrevive a un apagado inesperado.
- Las lecturas esperan mientras hay una escritura en curso. Con el volumen de una bodega (pocas escrituras pequeñas) no se nota.
- Una prueba verifica el modo de journal y el nivel de sincronización, para que un cambio de configuración no lo revierta sin aviso.
