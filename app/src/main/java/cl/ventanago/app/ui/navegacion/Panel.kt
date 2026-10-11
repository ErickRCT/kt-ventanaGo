package cl.ventanago.app.ui.navegacion

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.ventanago.app.datos.CarritoLocal
import cl.ventanago.app.datos.ClienteApi
import cl.ventanago.app.datos.Rol
import cl.ventanago.app.datos.Sesion
import cl.ventanago.app.datos.rol
import cl.ventanago.app.notificaciones.Push
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.comun.EstadoMensaje
import cl.ventanago.app.ui.comun.LocalAvisos
import cl.ventanago.app.ui.modulos.PantallaClientes
import cl.ventanago.app.ui.modulos.PantallaColores
import cl.ventanago.app.ui.modulos.PantallaInicio
import cl.ventanago.app.ui.modulos.PantallaPautas
import cl.ventanago.app.ui.modulos.PantallaPerfiles
import cl.ventanago.app.ui.modulos.PantallaProximamente
import cl.ventanago.app.ui.modulos.PantallaQuincalleria
import cl.ventanago.app.ui.modulos.PantallaSeries
import cl.ventanago.app.ui.modulos.PantallaTiposPauta
import cl.ventanago.app.ui.modulos.PantallaTiposPerfil
import cl.ventanago.app.ui.modulos.PantallaVidrios
import cl.ventanago.app.ui.modulos.cliente.PantallaCarrito
import cl.ventanago.app.ui.modulos.cliente.PantallaDisenar
import cl.ventanago.app.ui.modulos.cliente.PantallaMisCotizaciones
import cl.ventanago.app.ui.modulos.cotizaciones.PantallaCotizaciones
import cl.ventanago.app.ui.modulos.empresa.PantallaDetalleSolicitudEmpresa
import cl.ventanago.app.ui.modulos.empresa.PantallaSolicitudesEmpresa
import cl.ventanago.app.ui.modulos.empresa.SolicitudesEmpresaViewModel
import cl.ventanago.app.ui.modulos.mercado.PantallaMiPerfil
import cl.ventanago.app.ui.modulos.mercado.PantallaProveedores
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Layout de la app: encabezado, barra inferior (cliente y empresa) o menú lateral (administrador) y la navegación
 * entre pantallas, con historial (atrás vuelve a la pantalla anterior). Sin [sesion] el cliente igual puede diseñar y armar su carrito; [alPedirIngreso] abre el
 * login (con el motivo y la pantalla a la que volver después de entrar). [pantallaTrasIngreso]: dónde ir si se
 * acaba de entrar desde ahí. [solicitudNotificada]: solicitud de la notificación que se tocó para abrir la app;
 * [alAtenderNotificacion] avisa que ya se mostró.
 */
@Composable
fun Panel(
    sesion: Sesion?,
    alCerrarSesion: () -> Unit,
    alPedirIngreso: (motivo: String?, volverA: Pantalla?) -> Unit,
    pantallaTrasIngreso: Pantalla? = null,
    solicitudNotificada: Long? = null,
    alAtenderNotificacion: () -> Unit = {},
) {
    val context = LocalContext.current
    val alcance = rememberCoroutineScope()
    val cajon = rememberDrawerState(DrawerValue.Closed)
    val snackbar = remember { SnackbarHostState() }
    val navegacion = rememberNavController()
    val rol = sesion.rol
    val inicial = pantallaInicial(rol)
    val entrada by navegacion.currentBackStackEntryAsState()
    val ruta = entrada?.destination?.route
    val pantalla = pantallaDeRuta(ruta) ?: inicial
    val enDetalle = ruta == RUTA_SOLICITUD_EMPRESA
    val enAvisos = ruta == RUTA_AVISOS
    val pestanas = pestanasDe(rol)
    val nombre = sesion?.let { it.cuenta.nombre?.takeIf { n -> n.isNotBlank() } ?: it.cuenta.email.substringBefore('@') } ?: "Invitado"
    val email = sesion?.cuenta?.email.orEmpty()
    // Solicitud de Mis cotizaciones que se muestra desplegada (la de un aviso que se tocó).
    var destacada by rememberSaveable { mutableStateOf<Long?>(null) }
    var trasIngresoAtendido by rememberSaveable { mutableStateOf(false) }
    // Cada cuenta con su propio modelo de avisos (el administrador no recibe avisos); lo usan la campana y la pantalla.
    val modeloAvisos: AvisosViewModel = viewModel(key = "avisos-$email")
    if (rol.recibeAvisos) LaunchedEffect(modeloAvisos) { modeloAvisos.sondear() }
    // Ventanas en el carrito, para el número de la pestaña: sin cuenta las del teléfono, con cuenta las del backend.
    val unidadesCarrito = if (sesion == null) CarritoLocal.items.sumOf { it.cantidad } else ClienteApi.unidadesCarrito ?: 0

    /** Espera a que la navegación tenga su primera pantalla (antes no se puede navegar). */
    suspend fun navegacionLista() = snapshotFlow { entrada }.first { it != null }

    fun abrirDesdeMenu(p: Pantalla) {
        navegacion.abrirSeccion(p)
        alcance.launch { cajon.close() }
    }

    /** Lleva a la solicitud de un aviso: el cliente a Mis cotizaciones, la empresa al detalle de la solicitud. */
    fun abrirSolicitud(numero: Long?) {
        when (rol) {
            Rol.CLIENTE -> {
                destacada = numero
                navegacion.abrirSeccion(Pantalla.MIS_COTIZACIONES)
            }
            Rol.PROVEEDOR, Rol.ADMIN -> {
                navegacion.abrirSeccion(Pantalla.SOLICITUDES)
                if (numero != null) navegacion.navigate(rutaSolicitudEmpresa(numero)) { launchSingleTop = true }
            }
            Rol.INVITADO -> Unit
        }
    }

    // Recién entrado desde una pantalla (p. ej. el carrito al enviar), se vuelve a ella.
    LaunchedEffect(Unit) {
        if (trasIngresoAtendido) return@LaunchedEffect
        trasIngresoAtendido = true
        val destino = pantallaTrasIngreso?.takeIf { it != inicial && rol.puedeVer(it) } ?: return@LaunchedEffect
        navegacionLista()
        navegacion.abrirSeccion(destino)
    }

    // Lo que se armó sin cuenta pasa al carrito de la cuenta (clientes y administrador tienen carrito).
    if (rol.tieneCarrito && sesion != null) {
        LaunchedEffect(Unit) { runCatching { ClienteApi.carrito() } }
    }
    if (rol.tieneCarrito) {
        LaunchedEffect(Unit) {
            if (CarritoLocal.items.isNotEmpty()) {
                val pasadas = CarritoLocal.pasarACuenta()
                if (pasadas > 0) {
                    navegacionLista()
                    navegacion.abrirSeccion(Pantalla.CARRITO)
                    snackbar.showSnackbar("Tus $pasadas ${if (pasadas == 1) "ventana quedó guardada" else "ventanas quedaron guardadas"} en tu cuenta")
                }
            }
        }
    }

    // Notificaciones push: este teléfono queda asociado a la cuenta y, la primera vez, se pide el permiso.
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    if (sesion != null) {
        LaunchedEffect(sesion.token) {
            Push.registrar(context, sesion.token)
            if (rol.recibeAvisos && Push.debePedirPermiso(context)) pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Se tocó una notificación: se abre su solicitud.
    LaunchedEffect(solicitudNotificada) {
        if (solicitudNotificada == null || rol == Rol.INVITADO) return@LaunchedEffect
        navegacionLista()
        abrirSolicitud(solicitudNotificada)
        alAtenderNotificacion()
    }

    CompositionLocalProvider(LocalAvisos provides { mensaje -> alcance.launch { snackbar.showSnackbar(mensaje) } }) {
        val contenido = @Composable {
            Scaffold(
                topBar = {
                    // El detalle de una solicitud tiene su propia barra con el botón para volver.
                    if (!enDetalle) {
                        Encabezado(
                            titulo = if (enAvisos) "Avisos" else pantalla.titulo,
                            rol = rol,
                            nombre = nombre,
                            email = email,
                            sinLeer = modeloAvisos.sinLeer,
                            alVolver = if (enAvisos) ({ navegacion.popBackStack() }) else null,
                            alAbrirMenu = if (pestanas == null && !enAvisos) ({ alcance.launch { cajon.open() } }) else null,
                            alAbrirAvisos = if (rol.recibeAvisos && !enAvisos) ({ navegacion.navigate(RUTA_AVISOS) { launchSingleTop = true } }) else null,
                            alIngresar = { alPedirIngreso(null, pantalla) },
                            alCerrarSesion = alCerrarSesion,
                        )
                    }
                },
                bottomBar = {
                    if (pestanas != null && !enDetalle && !enAvisos) {
                        BarraInferior(pestanas, pantalla, unidadesCarrito) { navegacion.abrirSeccion(it) }
                    }
                },
                snackbarHost = { SnackbarHost(snackbar) },
                containerColor = MaterialTheme.colorScheme.background,
            ) { padding ->
                NavHost(
                    navegacion,
                    startDestination = inicial.name,
                    modifier = Modifier
                        .padding(padding)
                        .consumeWindowInsets(padding)
                        .fillMaxSize(),
                    enterTransition = { fadeIn(tween(200)) },
                    exitTransition = { fadeOut(tween(200)) },
                ) {
                    Pantalla.entries.forEach { p ->
                        composable(p.name) { entradaPantalla ->
                            Contenido(p, sesion, rol, nombre, email, navegacion, entradaPantalla, destacada, alPedirIngreso)
                        }
                    }
                    composable(RUTA_SOLICITUD_EMPRESA, arguments = listOf(navArgument(ARG_NUMERO) { type = NavType.LongType })) { e ->
                        PantallaDetalleSolicitudEmpresa(
                            numero = e.arguments?.getLong(ARG_NUMERO) ?: 0,
                            esAdmin = rol == Rol.ADMIN,
                            empresa = modeloEmpresa(navegacion, e),
                            alVolver = { navegacion.popBackStack() },
                        )
                    }
                    composable(RUTA_AVISOS) { PantallaAvisos(modeloAvisos, alAbrir = ::abrirSolicitud) }
                }
            }
        }

        // El administrador navega con el menú lateral; el cliente y la empresa, con la barra inferior.
        if (pestanas != null) {
            contenido()
        } else {
            BackHandler(enabled = cajon.isOpen) { alcance.launch { cajon.close() } }
            ModalNavigationDrawer(
                drawerState = cajon,
                gesturesEnabled = !enDetalle || cajon.isOpen,
                drawerContent = {
                    MenuLateral(
                        rol = rol,
                        actual = pantalla,
                        nombre = nombre,
                        email = email,
                        alElegir = ::abrirDesdeMenu,
                        alCerrarSesion = alCerrarSesion,
                        alIngresar = {
                            alcance.launch { cajon.close() }
                            alPedirIngreso(null, pantalla)
                        },
                    )
                },
                content = contenido,
            )
        }
    }
}

/** Pestañas del cliente o la empresa; el carrito muestra cuántas ventanas tiene. */
@Composable
private fun BarraInferior(pestanas: List<Pantalla>, actual: Pantalla, unidadesCarrito: Int, alElegir: (Pantalla) -> Unit) {
    Column {
        HorizontalDivider(color = Colores.borde)
        NavigationBar(containerColor = Colores.superficie, tonalElevation = 0.dp) {
            pestanas.forEach { p ->
                val elegida = p == actual
                NavigationBarItem(
                    selected = elegida,
                    onClick = { alElegir(p) },
                    icon = {
                        BadgedBox(badge = {
                            if (p == Pantalla.CARRITO && unidadesCarrito > 0) Badge { Text(if (unidadesCarrito > 99) "99+" else "$unidadesCarrito") }
                        }) {
                            Icon(
                                if (elegida) p.iconoActivo else p.icono,
                                contentDescription = if (p == Pantalla.CARRITO && unidadesCarrito > 0) "$unidadesCarrito en el carrito" else null,
                            )
                        }
                    },
                    label = { Text(p.corto, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Colores.azul,
                        selectedTextColor = Colores.azul,
                        indicatorColor = Colores.azulClaro,
                        unselectedIconColor = Colores.gris,
                        unselectedTextColor = Colores.gris,
                    ),
                )
            }
        }
    }
}

/**
 * Modelo de las solicitudes de empresa, compartido por la lista y el detalle: vive en el grafo de navegación (y
 * se descarta al cambiar de cuenta, que crea una navegación nueva).
 */
@Composable
private fun modeloEmpresa(navegacion: NavHostController, entrada: NavBackStackEntry): SolicitudesEmpresaViewModel {
    val grafo = remember(entrada) { navegacion.getBackStackEntry(navegacion.graph.id) }
    return viewModel(grafo)
}

@Composable
private fun Contenido(
    p: Pantalla,
    sesion: Sesion?,
    rol: Rol,
    nombre: String,
    email: String,
    navegacion: NavHostController,
    entrada: NavBackStackEntry,
    destacada: Long?,
    alPedirIngreso: (motivo: String?, volverA: Pantalla?) -> Unit,
) {
    val abrir: (Pantalla) -> Unit = { navegacion.abrir(it) }
    if (!p.disponible) return PantallaProximamente(p)
    when (p) {
        Pantalla.INICIO -> PantallaInicio(nombre, abrir)
        Pantalla.CREAR_COTIZACION -> PantallaCotizaciones(nuevaAlAbrir = true, alCerrarNueva = { navegacion.abrirSeccion(Pantalla.COTIZACIONES) })
        Pantalla.COTIZACIONES -> PantallaCotizaciones()
        Pantalla.CLIENTES -> PantallaClientes()
        Pantalla.PAUTAS -> PantallaPautas()
        Pantalla.TIPOS_PAUTA -> PantallaTiposPauta()
        Pantalla.PERFILES -> PantallaPerfiles()
        Pantalla.TIPOS_PERFIL -> PantallaTiposPerfil()
        Pantalla.QUINCALLERIA -> PantallaQuincalleria()
        Pantalla.VIDRIOS -> PantallaVidrios()
        Pantalla.COLORES -> PantallaColores()
        Pantalla.SERIES -> PantallaSeries()
        Pantalla.DISENAR_VENTANA -> PantallaDisenar(abrir, conCuenta = sesion != null)
        Pantalla.CARRITO -> PantallaCarrito(
            emailCuenta = email,
            abrir = abrir,
            // Admin y clientes guardan el carrito en su cuenta; una empresa no tiene carrito.
            conCuenta = rol.tieneCarrito,
            alPedirIngreso = { motivo -> alPedirIngreso(motivo, Pantalla.CARRITO) },
        )
        Pantalla.MIS_COTIZACIONES -> if (sesion == null) {
            EstadoMensaje(
                Icons.Outlined.History,
                "Ingresa para ver tus cotizaciones",
                "Aquí verás las ofertas de los proveedores a tus solicitudes.",
                "Ingresar",
            ) { alPedirIngreso(null, Pantalla.MIS_COTIZACIONES) }
        } else {
            PantallaMisCotizaciones(abrir, destacar = destacada)
        }
        Pantalla.PROVEEDORES -> PantallaProveedores(esAdmin = rol == Rol.ADMIN)
        Pantalla.SOLICITUDES -> PantallaSolicitudesEmpresa(
            esAdmin = rol == Rol.ADMIN,
            vm = modeloEmpresa(navegacion, entrada),
            alAbrir = { navegacion.navigate(rutaSolicitudEmpresa(it)) },
        )
        Pantalla.MI_PERFIL -> PantallaMiPerfil()
    }
}
