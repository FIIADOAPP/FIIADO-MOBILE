# 0008. Restaurar sin pisar los cambios pendientes del teléfono

- Estado: aceptada
- Fecha: 2026-11-21

## Contexto

El ADR-0003 de FIIADO-API dice que, al restaurar, "lo que existe en ambos lados queda como lo tiene el servidor", y que después se sube la cola de pendientes del teléfono.

En el emulador apareció un caso que esa regla no resuelve. Un cliente se editó sin sesión: se le agregó el celular, y el cambio quedó en la cola. Al iniciar sesión, la restauración reemplazó al cliente local por la versión del servidor, que todavía no tenía celular. Luego la cola subió el cambio pendiente. El servidor quedó con el celular y el teléfono sin él: las dos copias quedaron distintas y nada las vuelve a igualar.

Pasa cada vez que un registro que también está en el servidor tiene un cambio local sin subir cuando se restaura: al editar sin sesión y luego iniciarla, o en un teléfono desplazado que vuelve a entrar.

## Decisión

- Si un registro tiene un cambio pendiente en la cola al restaurar, **se conserva la versión del teléfono** y no se reemplaza por la del servidor. Ese cambio es el más reciente del dueño y la cola lo sube a continuación, así teléfono y servidor terminan iguales.
- Aplica a clientes y métodos de cobro, los únicos registros que se editan. Un movimiento no cambia salvo al anularse, así que se sigue combinando como dice el ADR-0003 de FIIADO-API: la anulación se conserva venga de donde venga, y teléfono y servidor quedan iguales.
- En el primer vínculo, cuando la cola se compacta, los registros conservados por tener cambios pendientes se vuelven a encolar con su foto actual, junto con lo que solo existe en el teléfono.
- Esto precisa el ADR-0003 de FIIADO-API sin cambiar la API: el servidor sigue aplicando las fotos que le llegan, en el orden en que llegan.

## Consecuencias

- Un teléfono nunca queda mostrando una versión distinta de la que termina en el servidor.
- Se mantiene el riesgo que ya aceptaba el ADR-0003: si un teléfono desplazado vuelve con ediciones viejas de un cliente o de un método de cobro, esas ediciones reemplazan las que se hicieron después en el otro teléfono. Ahora el teléfono, además, las sigue mostrando, en lugar de mostrar otra cosa.
- Para saber qué registros tienen cambios pendientes, la restauración lee la cola completa en cada página. Para el tamaño de la cola de una bodega no se nota.
