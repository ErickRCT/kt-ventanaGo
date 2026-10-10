# VentanaGo · App Android nativa (Kotlin + Jetpack Compose)

Migración de `njs-ventanaGo` a una app Android nativa. Usa el **mismo backend** (`ms-ventanaGo` en Render) y las mismas imágenes de catálogo (front en Vercel).

## Qué incluye

- **Sin cuenta:** la app abre en Diseñar ventana. Se puede elegir el modelo, ver el precio referencial, mirarla en la pared y armar el carrito (guardado en el teléfono). La cuenta se pide recién al enviar la solicitud; al entrar, el carrito y los datos de contacto pasan a la cuenta.
- **Cliente** (entra con un código por correo): Diseñar ventana, Carrito, Mis cotizaciones y avisos de las respuestas.
- **Empresa / proveedor** (correo y contraseña): Solicitudes de los clientes, para aceptarlas con precio, modificarlas o rechazarlas.
- **Administrador** (correo y contraseña): Inicio, Crear cotización (PDF de cotización y orden de trabajo), Cotizaciones, Clientes, Pautas, Tipos de pauta, Perfiles, Tipos de perfil, Quincallería, Vidrios, Colores y Series, además de las vistas de cliente y empresa.
- **Realidad aumentada** con ARCore y un renderizador OpenGL propio. Si el teléfono no puede (sin ARCore, poca memoria, sin permiso de cámara), la app cae a la vista con cámara de tamaño aproximado o al dibujo, explicando el motivo.

## Compilar

Requiere JDK 17+ y el Android SDK (`local.properties` apunta a él).

```bash
./gradlew assembleRelease
```

El APK queda en `app/build/outputs/apk/release/app-release.apk`. Para instalarlo por cable: `adb install -r app/build/outputs/apk/release/app-release.apk`.

La versión release está optimizada con R8 (Compose en debug va lento en teléfonos de gama de entrada). El `applicationId` es `cl.ventanago.movil`, distinto del de la app React Native, así que ambas pueden convivir.

## Clave de firma

La versión release se firma con la clave propia del proyecto, en la carpeta `firma/` (**fuera de git**):

- `firma/ventanago-release.jks`: la clave.
- `firma/keystore.properties`: su contraseña y alias.

Guarda una copia de esa carpeta en un lugar seguro (por ejemplo, Google Drive privado). **Sin ella no se pueden publicar actualizaciones**: un APK firmado con otra clave no se instala encima de la app ya instalada, y Google Play tampoco lo acepta. Para compilar en otro computador, copia la carpeta `firma/` a la raíz del proyecto.

Sin la carpeta (por ejemplo, recién clonado el repositorio), la release se firma con la clave de depuración de ese computador: sirve para probar, pero no para compartir.

Huella del certificado (para Google Sign-In y Google Play):

- SHA-1: `A9:0B:28:DF:14:35:54:A3:03:E5:11:9C:17:94:B6:25:BF:72:6B:B7`
- SHA-256: `53:EF:4A:5D:C4:9F:85:7E:5B:55:F0:6F:4A:96:73:6B:FC:52:35:25:77:C4:17:E3:E6:72:CD:87:08:5B:27:42`

## Versión de depuración

`./gradlew assembleDebug` genera una versión que permite abrir la realidad aumentada directo desde adb (para probarla sin iniciar sesión):

```bash
adb shell am start -n cl.ventanago.movil/cl.ventanago.app.ar.RealidadAumentadaActivity --ei ancho 1500 --ei alto 1200 --es modelo CORREDERA
```

## Estructura

```
app/src/main/java/cl/ventanago/app/
  datos/          Api (OkHttp + kotlinx.serialization), modelos, sesión, carrito local, validaciones
  ventana/        Geometría y dibujo de las ventanas, reglas del catálogo
  ar/             Realidad aumentada: detección del teléfono, renderizador, vista con cámara
  ui/comun/       Mantenedor genérico, formularios, selectores, imágenes
  ui/modulos/     Pantallas de cada módulo (administrador, cotizaciones, cliente, empresa)
  ui/navegacion/  Encabezado, menú lateral y panel por rol
```
