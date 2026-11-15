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
