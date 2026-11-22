# Registro de decisiones de arquitectura

Cada decisión importante se registra aquí antes de implementarla, como un archivo `NNNN-titulo-corto.md` con este formato:

```markdown
# NNNN. Título

- Estado: propuesta | aceptada | reemplazada por NNNN
- Fecha: AAAA-MM-DD

## Contexto
Qué problema hay y qué restricciones importan.

## Decisión
Qué se decidió, en una o pocas frases.

## Consecuencias
Qué se gana, qué se pierde y qué queda obligado a partir de ahora.
```

Un ADR aceptado no se edita: si la decisión cambia, se escribe uno nuevo que lo reemplaza.

| ADR | Título | Estado |
|---|---|---|
| [0001](0001-stack-y-dos-repositorios.md) | Stack de la app y dos repositorios | Aceptada |
| [0002](0002-modelo-de-datos.md) | Modelo de datos de la libreta | Aceptada |
| [0003](0003-durabilidad-de-escrituras.md) | Durabilidad de las escrituras locales | Aceptada |
| [0004](0004-metodos-de-cobro.md) | Métodos de cobro | Aceptada |
| [0005](0005-sesion-en-la-app.md) | Cuenta y sesión en la app | Aceptada |
| [0006](0006-bloqueo-de-la-app.md) | Bloqueo de la app con la huella o el PIN del teléfono | Aceptada |
| [0007](0007-plazos-y-puntualidad.md) | Plazos de pago y puntualidad de los clientes | Aceptada |
| [0008](0008-restaurar-sin-pisar-pendientes.md) | Restaurar sin pisar los cambios pendientes del teléfono | Aceptada |
| [0009](0009-firma-y-publicacion.md) | Firma y publicación en Google Play | Aceptada |
| [0010](0010-version-1-solo-en-el-telefono.md) | La versión 1.0 guarda la libreta solo en el teléfono | Aceptada |
| [0011](0011-publicidad-y-compra-para-quitarla.md) | Publicidad con banner y compra única para quitarla | Aceptada |
