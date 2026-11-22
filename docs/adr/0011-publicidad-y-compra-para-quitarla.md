# 0011. Publicidad con banner y compra única para quitarla

- Estado: aceptada
- Fecha: 2026-11-22

## Contexto

Fiado se publica gratis en Google Play y debe generar ingresos. Es una herramienta de trabajo: si un anuncio tapa la pantalla mientras se fía o se cobra, el bodeguero pierde tiempo y confianza. La libreta vive solo en el teléfono y no hay servidor propio (ADR-0010), así que la compra no se puede verificar en un backend.

## Decisión

- **Banner adaptativo de AdMob** (`play-services-ads`), fijo justo encima de la barra inferior. Solo aparece en las pantallas principales Clientes y Resumen. Nunca sale en diálogos, formularios ni en el historial, y no hay anuncios a pantalla completa.
- **Consentimiento:** antes de pedir anuncios se usa el formulario estándar de Google (User Messaging Platform). Solo se muestra donde la ley lo exige. Si no hay consentimiento, no se cargan anuncios.
- **Compra única «Quitar anuncios»**, con Google Play Billing (`billing-ktx`):
  - Es un producto no consumible con el id `quitar_anuncios`. El precio se fija en Play Console.
  - La compra se reconoce (*acknowledge*) al recibirla; si no, Google la devuelve a los 3 días.
  - Al abrir la app se consultan las compras con `queryPurchasesAsync`. Así la compra vuelve al reinstalar o al cambiar de teléfono con la misma cuenta de Google.
  - El último estado conocido se guarda en las preferencias. Sin internet, quien ya compró sigue sin anuncios.
  - Sin servidor, la compra se verifica solo en el teléfono. Se acepta el riesgo de que alguien la falsifique en un teléfono modificado: el monto es bajo.
- **Dónde se compra:** en *Cuenta*, con el botón «Quitar anuncios» y el precio que devuelve Play. Hay un «Restaurar compra» que vuelve a consultar.
- **Identificadores:** el App ID y el id del bloque de anuncios de AdMob llegan por las propiedades de Gradle `fiado.admobAppId` y `fiado.admobBanner`. En el workflow `Release` salen de las variables `ADMOB_APP_ID` y `ADMOB_BANNER`.
  - El build `debug`, y un release sin esas variables, usan los ids de prueba oficiales de Google. Así nunca se hacen clics inválidos sobre anuncios reales.
- **Datos y privacidad:**
  - En Play Console se declara «Contiene anuncios».
  - En *Seguridad de los datos* se declara que AdMob recoge el identificador de publicidad y datos de diagnóstico.
  - La política de privacidad menciona AdMob y Google Play Billing.

## Consecuencias

- La app genera ingresos sin interrumpir el trabajo, y quien paga una vez deja de ver anuncios para siempre.
- Se suman tres bibliotecas de Google y el permiso `AD_ID`. La app deja de ser «sin datos compartidos».
- La compra solo se prueba de verdad con la app subida a un track de prueba de Play Console y con cuentas de *license testing*. Las pruebas automáticas cubren la lógica, con Play Billing simulado.
- Si más adelante vuelve el servidor (ADR-0010), la compra se puede verificar allí sin cambiar el producto.
