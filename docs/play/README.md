# Publicación en Google Play

Todo lo que pide Play Console para publicar **Fiadito**, listo para copiar. Los gráficos están en esta carpeta.

| Dato | Valor |
|---|---|
| Nombre de la app | Fiadito |
| Paquete | `com.fiiadoapp.fiado` (no se puede cambiar después) |
| Nombre del desarrollador | Fiado |
| Correo de contacto | CORREO_DE_CONTACTO |
| Política de privacidad | https://github.com/FIIADOAPP/FIIADO-MOBILE/blob/main/docs/privacidad.md |
| Categoría | Empresa |
| Precio | Gratis, con anuncios y compras en la app |
| Producto en la app | `quitar_anuncios`, «Quitar anuncios», S/ 9.90, compra única |

## Ficha de Play Store

**Nombre** (30 caracteres como máximo)

```
Fiadito: libreta de fiados
```

**Descripción breve** (80 caracteres como máximo)

```
Anota lo que fías, cobra a tiempo y sabe quién te paga puntual. Sin internet.
```

**Descripción completa**

```
Fiadito es la libreta de fiados de tu bodega, en tu celular. Anota en segundos lo que fías y lo que te pagan, y ten siempre a la mano cuánto te debe cada cliente.

FIAR Y COBRAR EN DOS TOQUES
• Cada cliente muestra lo que debe y dos botones: Fiar y Cobrar.
• Registra pagos en efectivo, Yape, Plin o el método que agregues.
• ¿Te equivocaste? Toca Deshacer al instante o anula el movimiento desde el historial.

PLAZOS: ¿PARA CUÁNDO TE PAGA?
• Al fiar elige Mañana, 3 días, 1 semana, Fin de mes u otra fecha.
• La lista te avisa lo que vence hoy, mañana o lo que ya está vencido.
• ¿Te pidió más tiempo? Dale más plazo sin perder el historial.

SABE QUIÉN TE PAGA A TIEMPO
• Cada cliente tiene su semáforo: Puntual, Se demora o Cuidado.
• Mira cuántos días tarda en pagarte y qué porcentaje paga a tiempo.
• Al fiarle a alguien en Cuidado, Fiadito te avisa con sus números para que decidas.
• Te sugiere el plazo que ese cliente suele necesitar.

RESUMEN DE TU NEGOCIO
• Cuánto te deben en total, lo fiado y cobrado hoy y en el mes.
• Lo cobrado por cada método de pago.
• Cómo te pagan tus clientes y quiénes necesitan atención.

RECUERDA POR WHATSAPP
• Envía un recordatorio amable con el monto ya escrito.
• Personaliza el mensaje con el nombre del cliente, el saldo y tu negocio.

FUNCIONA SIN INTERNET Y CUIDA TU INFORMACIÓN
• Todo se guarda en tu celular y funciona sin conexión.
• Activa la copia de seguridad de Google para recuperar tu libreta si cambias de teléfono.
• Protege la app con tu huella o PIN.

Fiadito es gratis con un anuncio pequeño. Si prefieres, quítalo para siempre con un pago único.
```

**Gráficos**

| Recurso | Archivo |
|---|---|
| Ícono (512 × 512) | [icono-512.png](icono-512.png) |
| Gráfico destacado (1024 × 500) | [grafico-destacado-1024x500.png](grafico-destacado-1024x500.png) |
| Capturas del teléfono (1080 × 2160) | [capturas/](capturas/) |

Las capturas usan una libreta de ejemplo con nombres inventados.

## Formularios de «Contenido de la app»

**Política de privacidad:** la URL de arriba.

**Anuncios:** Sí, la app contiene anuncios.

**Acceso a la app:** Todas las funciones están disponibles sin restricciones (no hay inicio de sesión).

**Clasificación del contenido** (cuestionario IARC)
- Categoría: *Utilidades, productividad, comunicación u otras*.
- Violencia, sexualidad, lenguaje, drogas, apuestas: **No** en todo.
- ¿Los usuarios interactúan o comparten contenido entre sí? **No**.
- ¿Comparte la ubicación del usuario? **No**.
- ¿Permite compras digitales? **Sí** (quitar anuncios).
- Resultado esperado: apta para todos (PEGI 3 / Para todos).

**Público objetivo:** 18 años o más. No está diseñada para niños.

**Funciones financieras:** la app **no** ofrece préstamos, pagos, inversiones ni criptomonedas. Elige *Mi app no ofrece ninguna de estas funciones financieras*: es una libreta que solo anota deudas entre el negocio y sus clientes.

**Aplicación gubernamental:** No.

**Salud:** No.

**Seguridad de los datos**

| Pregunta | Respuesta |
|---|---|
| ¿Recoge o comparte alguno de los tipos de datos de usuario obligatorios? | **Sí** (por AdMob) |
| ¿Todos los datos recogidos están cifrados en tránsito? | **Sí** |
| ¿Ofreces una forma de que los usuarios pidan que se eliminen sus datos? | **No** hace falta cuenta: los datos de la libreta no salen del teléfono. Responde *No* y explica en la política que se borran desde el teléfono. |

Tipos de datos que se declaran (los recoge y comparte el SDK de Google Mobile Ads):

| Tipo | Recogido | Compartido | Para qué | Obligatorio |
|---|---|---|---|---|
| Identificadores del dispositivo u otros (ID de publicidad) | Sí | Sí | Publicidad o marketing, Analíticas, Prevención de fraudes | Obligatorio |
| Interacciones con la app | Sí | Sí | Publicidad o marketing, Analíticas | Obligatorio |
| Registros de fallos y diagnósticos | Sí | Sí | Analíticas | Obligatorio |
| Ubicación aproximada (por la IP) | Sí | Sí | Publicidad o marketing | Obligatorio |

La libreta (clientes, montos, teléfonos) **no se declara**: no sale del teléfono. La copia de seguridad de Google la gestiona Android, no la app.

## Producto en la app

En **Monetizar → Productos → Productos únicos → Crear producto**:

| Campo | Valor |
|---|---|
| ID del producto | `quitar_anuncios` (exactamente así: la app lo busca con ese id) |
| Nombre | Quitar anuncios |
| Descripción | Quita el anuncio de Fiadito para siempre con un pago único. |
| Precio | S/ 9.90 |

Para que la opción aparezca en la app, el producto debe estar **Activo** y la app instalada desde un track de prueba de Play.
