# 0009. Firma y publicación en Google Play

- Estado: aceptada
- Fecha: 2026-11-21

## Contexto

La v1.0 se publica en Google Play. Play exige un Android App Bundle (AAB) firmado con una clave de subida. Si esa clave se pierde o se filtra, hay que pedirle a Google que la cambie, así que no puede vivir en el repositorio ni en un solo lugar sin respaldo. Además, el APK de cada release en GitHub debe poder instalarse para la prueba de aceptación.

La API corre en el plan gratuito de Render (ADR-0004 de la API). Se apaga si pasa 15 minutos sin peticiones, y la primera petición tarda hasta un minuto en encenderla. Hoy la app espera 15 segundos, así que el primer intento fallaría.

## Decisión

- **Play App Signing:** Google guarda la clave con la que se firma la app que se instala. Nosotros solo tenemos la **clave de subida**.
- **Quién la genera:** la persona dueña del proyecto, con `keytool`, en un `.jks` fuera del repositorio y con un respaldo aparte. Las contraseñas nunca pasan por el chat ni por el repositorio.
- **Cómo la lee Gradle:** la firma `release` toma las variables de entorno `FIRMA_ARCHIVO`, `FIRMA_CLAVE`, `FIRMA_ALIAS` y `FIRMA_CLAVE_ALIAS`. Sin ellas, el release se compila sin firmar, y las compilaciones de los pull requests no las necesitan.
- **CI:** al publicar un release en GitHub, el workflow `Release` decodifica el `.jks` desde el secreto `FIRMA_KEYSTORE_BASE64`. Luego compila el AAB y el APK firmados y los adjunta al release de GitHub.
- **URL de la API en el release:** sale de la variable `API_URL_RELEASE` del repositorio y llega a Gradle como `fiado.apiUrlRelease`. El workflow falla si falta, para que nunca se publique un APK que apunte a una URL inválida.
- **Tiempo de espera:** la app espera hasta **70 segundos** la respuesta de la API, para cubrir el encendido del servicio gratuito.

## Consecuencias

- La clave de subida nunca está en el repositorio. Si se pierde, Google permite cambiarla sin perder la app publicada.
- Cada release de GitHub trae el AAB para Play y el APK para instalar a mano.
- Al iniciar sesión con la API apagada, la pantalla puede quedarse cargando hasta un minuto. La sincronización en segundo plano no se ve afectada.
- Publicar en Play sigue siendo un paso manual en la consola: subir el AAB, la ficha y el formulario de seguridad de datos.
