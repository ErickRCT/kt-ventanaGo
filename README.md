# VentanaGo · App Android nativa (Kotlin + Jetpack Compose)

Migración de `njs-ventanaGo` a una app Android nativa. Usa el **mismo backend** (`ms-ventanaGo` en Render) y las mismas imágenes de catálogo (front en Vercel).

## Qué incluye

- **Sin cuenta:** la app abre en Diseñar ventana. Se puede elegir el modelo, ver el precio referencial, mirarla en la pared y armar el carrito (guardado en el teléfono). La cuenta se pide recién al enviar la solicitud; al entrar, el carrito y los datos de contacto pasan a la cuenta.
- **Cliente** (entra con un código por correo): Diseñar ventana, Carrito, Mis cotizaciones y avisos de las respuestas.
- **Mercado de ofertas:** la solicitud llega a los proveedores de la comuna y servicios pedidos (o solo a los que elija el cliente). Cada proveedor envía su oferta (precio, plazo, cambios); el cliente las compara, conversa por chat con cada uno, elige una y al final valora el trabajo. El proveedor ve el contacto del cliente recién cuando su oferta es elegida. También se pueden pedir servicios sin ventanas (reparación, cambio de vidrio, mantención, visita técnica) y adjuntar fotos.
- **Proveedores:** listado público por comuna y servicio, con valoraciones y sello de verificado (lo marca el administrador).
- **Empresa / proveedor** (correo y contraseña): Solicitudes (por ofertar, ofertadas, ganadas, cerradas) y Mi perfil (servicios y comunas donde trabaja).
- **Administrador** (correo y contraseña): Inicio, Crear cotización (PDF de cotización y orden de trabajo), Cotizaciones, Clientes, Pautas, Tipos de pauta, Perfiles, Tipos de perfil, Quincallería, Vidrios, Colores y Series, además de las vistas de cliente y empresa.
- **Realidad aumentada** con ARCore y un renderizador OpenGL propio. Si el teléfono no puede (sin ARCore, poca memoria, sin permiso de cámara), la app cae a la vista con cámara de tamaño aproximado o al dibujo, explicando el motivo.

## Compilar

Requiere JDK 17+ y el Android SDK (`local.properties` apunta a él).

```bash
./gradlew assembleRelease
```

Para probar contra un backend local (el emulador ve el computador en `10.0.2.2`): `./gradlew assembleRelease -Pventanago.apiUrl=http://10.0.2.2:7099`.

El APK queda en `app/build/outputs/apk/release/app-release.apk`. Para instalarlo por cable: `adb install -r app/build/outputs/apk/release/app-release.apk`.

La versión release está optimizada con R8 (Compose en debug va lento en teléfonos de gama de entrada). El `applicationId` es `cl.ventanago.movil`, distinto del de la app React Native, así que ambas pueden convivir.

## Ingreso con Google

Los clientes pueden entrar (y crear su cuenta) con "Continuar con Google". Se configura una vez en [Google Cloud Console](https://console.cloud.google.com/) > APIs y servicios:

1. **Pantalla de consentimiento de OAuth**: tipo Externo, nombre VentanaGo y correo de soporte. Mientras esté en modo Prueba, solo entran los correos agregados como usuarios de prueba.
2. **Credenciales > Crear ID de cliente > Aplicación web**: su client ID (`…apps.googleusercontent.com`) va en:
   - el backend: `app.google.client-id` (local) o la variable `APP_GOOGLE_CLIENT_ID` (Render);
   - la app: `ventanago.googleClientId` en `gradle.properties`.
3. **Credenciales > Crear ID de cliente > Android**: paquete `cl.ventanago.movil` y la huella SHA-1 de la clave de firma (más abajo). No se copia a ningún lado: Google lo usa para reconocer la app.

Sin el client ID en `gradle.properties`, la app no muestra el botón.

## Notificaciones push

Cada aviso de la campana (oferta nueva, mensaje del chat, oferta elegida, solicitud nueva para la empresa…) llega también como notificación al teléfono, aunque la app esté cerrada. Al tocarla se abre la solicitud. Se usa Firebase Cloud Messaging y se configura una vez en la [consola de Firebase](https://console.firebase.google.com/):

1. **Crear un proyecto** (puede ser el mismo proyecto de Google Cloud del ingreso con Google).
2. **Agregar una app Android** con el paquete `cl.ventanago.movil` y descargar `google-services.json` a la carpeta `app/`. Sin ese archivo la app compila igual, pero no recibe notificaciones.
3. **Configuración del proyecto > Cuentas de servicio > Generar nueva clave privada**: el JSON descargado va en la variable de entorno `FIREBASE_CREDENCIALES` del backend en Render (el contenido del archivo tal cual, o en base64). Ese archivo **sí es secreto**: no lo subas a git.

Al iniciar sesión, la app registra el teléfono en el backend (`POST /dispositivos`) y, en Android 13 o superior, pide permiso para mostrar notificaciones (solo la primera vez). Al cerrar sesión el teléfono deja de recibir los avisos de esa cuenta. Con la app abierta, la campana, Mis cotizaciones, las solicitudes de la empresa y el chat se actualizan apenas llega un aviso.

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
  datos/            Api (OkHttp + kotlinx.serialization), modelos, roles, sesión, carrito local, validaciones
  notificaciones/   Notificaciones push (Firebase Cloud Messaging)
  ventana/          Geometría y dibujo de las ventanas, reglas del catálogo
  ar/               Realidad aumentada: detección del teléfono, sesión de ARCore, interfaz, renderizador, vista con cámara
  ui/comun/         Mantenedor genérico, formularios, selectores, imágenes, ModeloBase de los ViewModel
  ui/modulos/       Pantallas de cada módulo (administrador, cotizaciones, cliente, empresa, mercado)
  ui/navegacion/    Navegación (Navigation Compose), encabezado, menú lateral y panel por rol
```

Cada pantalla del menú es un destino de la navegación, con historial: atrás vuelve a la pantalla anterior, y al volver a una sección desde el menú se recupera cómo quedó. El detalle de una solicitud de empresa es su propio destino (`solicitud/{numero}`), al que también llevan las notificaciones. Las pantallas con más lógica (Carrito, Mis cotizaciones, Solicitudes de empresa y su oferta, campana de avisos) guardan su estado y llamadas al backend en un ViewModel, así sobreviven a girar el teléfono; el editor de cotizaciones usa una clase de estado (`EstadoEditorCotizacion`) porque vive en un diálogo.
