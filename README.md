# VentanaGo · App Android nativa (Kotlin + Jetpack Compose)

Migración de `njs-ventanaGo` a una app Android nativa. Usa el **mismo backend** (`ms-ventanaGo` en Render) y las mismas imágenes de catálogo (front en Vercel): el servidor no cambia.

## Etapa 1: panel del administrador

- Login con correo y contraseña (`/auth/login`); la sesión queda guardada hasta que vence el token.
- Layout: encabezado (menú, notificaciones, cuenta) y menú lateral agrupado como en la web.
- Inicio con totales del catálogo.
- Mantenedores: Clientes, Tipos de pauta (solo crear: el backend no tiene modificar/eliminar), Perfiles, Tipos de perfil, Quincallería, Vidrios, Colores y Series.
- Pendientes (marcados "Pronto" en el menú): Crear cotización, Cotizaciones, Pautas y las vistas de cliente y empresa.

## Compilar e instalar

Requiere JDK 17+ y el Android SDK (`local.properties` apunta a él).

```bash
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

La versión release está optimizada con R8 (Compose en debug va lento en teléfonos de gama de entrada) y se firma con la clave de depuración para instalarla directo. El `applicationId` es `cl.ventanago.movil`, distinto del de la app React Native, así que ambas pueden convivir.

## Estructura

```
app/src/main/java/cl/ventanago/app/
  datos/        Api (OkHttp + kotlinx.serialization), modelos, recursos CRUD, sesión, validaciones
  ui/comun/     Mantenedor genérico, formularios, selectores, imágenes
  ui/modulos/   Pantallas de cada módulo
  ui/navegacion Encabezado, menú lateral y panel
```
