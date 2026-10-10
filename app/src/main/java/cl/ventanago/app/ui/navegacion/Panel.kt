package cl.ventanago.app.ui.navegacion

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cl.ventanago.app.BuildConfig
import cl.ventanago.app.R
import cl.ventanago.app.datos.Sesion
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.comun.LocalAvisos
import cl.ventanago.app.ui.modulos.PantallaClientes
import cl.ventanago.app.ui.modulos.cotizaciones.PantallaCotizaciones
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.Button
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.History
import cl.ventanago.app.datos.CarritoLocal
import cl.ventanago.app.ui.comun.EstadoMensaje
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.foundation.layout.widthIn
import cl.ventanago.app.datos.Aviso
import cl.ventanago.app.datos.ClienteApi
import cl.ventanago.app.ui.modulos.cliente.PantallaCarrito
import cl.ventanago.app.ui.modulos.empresa.PantallaSolicitudesEmpresa
import cl.ventanago.app.ui.modulos.cliente.PantallaDisenar
import cl.ventanago.app.ui.modulos.cliente.PantallaMisCotizaciones

/**
 * Layout de la app: encabezado, menú lateral y el contenido de la pantalla elegida. Sin [sesion] el cliente
 * igual puede diseñar y armar su carrito; [alPedirIngreso] abre el login (con el motivo y la pantalla a la que
 * volver después de entrar). [pantallaTrasIngreso]: dónde partir si se acaba de entrar desde ahí.
 */
@Composable
fun Panel(
    sesion: Sesion?,
    alCerrarSesion: () -> Unit,
    alPedirIngreso: (motivo: String?, volverA: Pantalla?) -> Unit,
    pantallaTrasIngreso: Pantalla? = null,
) {
    val alcance = rememberCoroutineScope()
    val cajon = rememberDrawerState(DrawerValue.Closed)
    val snackbar = remember { SnackbarHostState() }
    val rol = sesion?.cuenta?.rol ?: INVITADO
    val inicial = pantallaInicial(rol)
    var pantalla by rememberSaveable { mutableStateOf(pantallaTrasIngreso?.takeIf { p -> gruposDe(rol).any { p in it.pantallas } } ?: inicial) }
    val nombre = sesion?.let { it.cuenta.nombre?.takeIf { n -> n.isNotBlank() } ?: it.cuenta.email.substringBefore('@') } ?: "Invitado"
    val email = sesion?.cuenta?.email.orEmpty()

    // Lo que se armó sin cuenta pasa al carrito de la cuenta (clientes y administrador tienen carrito).
    if (rol == "CLIENTE" || rol == "ADMIN") {
        LaunchedEffect(Unit) {
            if (CarritoLocal.items.isNotEmpty()) {
                val pasadas = CarritoLocal.pasarACuenta()
                if (pasadas > 0) {
                    pantalla = Pantalla.CARRITO
                    snackbar.showSnackbar("Tus $pasadas ${if (pasadas == 1) "ventana quedó guardada" else "ventanas quedaron guardadas"} en tu cuenta")
                }
            }
        }
    }

    val abrir: (Pantalla) -> Unit = {
        pantalla = it
        alcance.launch { cajon.close() }
    }

    BackHandler(enabled = cajon.isOpen) { alcance.launch { cajon.close() } }
    BackHandler(enabled = cajon.isClosed && pantalla != inicial) { pantalla = inicial }

    CompositionLocalProvider(LocalAvisos provides { mensaje -> alcance.launch { snackbar.showSnackbar(mensaje) } }) {
        ModalNavigationDrawer(
            drawerState = cajon,
            drawerContent = {
                MenuLateral(
                    rol = rol,
                    actual = pantalla,
                    nombre = nombre,
                    email = email,
                    alElegir = abrir,
                    alCerrarSesion = alCerrarSesion,
                    alIngresar = {
                        alcance.launch { cajon.close() }
                        alPedirIngreso(null, pantalla)
                    },
                )
            },
        ) {
            Scaffold(
                topBar = {
                    Encabezado(
                        rol = rol,
                        nombre = nombre,
                        email = email,
                        alIngresar = { alPedirIngreso(null, pantalla) },
                        alAbrirMenu = { alcance.launch { cajon.open() } },
                        alAbrirSolicitudes = { pantalla = if (rol == "CLIENTE") Pantalla.MIS_COTIZACIONES else Pantalla.SOLICITUDES },
                        alCerrarSesion = alCerrarSesion,
                    )
                },
                snackbarHost = { SnackbarHost(snackbar) },
                containerColor = MaterialTheme.colorScheme.background,
            ) { padding ->
                Box(
                    Modifier
                        .padding(padding)
                        .fillMaxSize(),
                ) {
                    Crossfade(targetState = pantalla, label = "pantalla") { actual ->
                        when (actual) {
                            Pantalla.INICIO -> PantallaInicio(nombre, abrir)
                            Pantalla.CREAR_COTIZACION -> PantallaCotizaciones(nuevaAlAbrir = true, alCerrarNueva = { pantalla = Pantalla.COTIZACIONES })
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
                                conCuenta = rol == "CLIENTE" || rol == "ADMIN",
                                alPedirIngreso = { motivo -> alPedirIngreso(motivo, Pantalla.CARRITO) },
                            )
                            Pantalla.MIS_COTIZACIONES -> if (sesion == null) {
                                EstadoMensaje(
                                    Icons.Outlined.History,
                                    "Ingresa para ver tus cotizaciones",
                                    "Aquí verás las respuestas de la empresa a tus solicitudes.",
                                    "Ingresar",
                                ) { alPedirIngreso(null, Pantalla.MIS_COTIZACIONES) }
                            } else {
                                PantallaMisCotizaciones(abrir)
                            }
                            Pantalla.SOLICITUDES -> PantallaSolicitudesEmpresa()
                            else -> PantallaProximamente(actual)
                        }
                    }
                }
            }
        }
    }
}

/** Equivale a components/Encabezado: botón de menú, marca, notificaciones y menú de la cuenta. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Encabezado(
    rol: String,
    nombre: String,
    email: String,
    alIngresar: () -> Unit,
    alAbrirMenu: () -> Unit,
    alAbrirSolicitudes: () -> Unit,
    alCerrarSesion: () -> Unit,
) {
    var avisosAbiertos by remember { mutableStateOf(false) }
    var cuentaAbierta by remember { mutableStateOf(false) }
    val alcance = rememberCoroutineScope()
    // Avisos: el cliente recibe las respuestas y la empresa las solicitudes nuevas; se revisan cada minuto (la web, cada 30 s).
    var avisos by remember { mutableStateOf<List<Aviso>>(emptyList()) }
    if (rol == "CLIENTE" || rol == "PROVEEDOR") {
        LaunchedEffect(Unit) {
            while (true) {
                try {
                    avisos = ClienteApi.avisos().sortedByDescending { it.fecha }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Sin conexión: se reintenta en el próximo ciclo.
                }
                delay(60_000)
            }
        }
    }
    val sinLeer = avisos.count { !it.leido }
    fun cerrarAvisos() {
        avisosAbiertos = false
        if (sinLeer > 0) {
            avisos = avisos.map { it.copy(leido = true) }
            alcance.launch { runCatching { ClienteApi.marcarAvisosLeidos() } }
        }
    }
    Column {
        TopAppBar(
            title = { Text("VentanaGo", fontWeight = FontWeight.Bold, color = Colores.menuFondo) },
            navigationIcon = {
                IconButton(onClick = alAbrirMenu) { Icon(Icons.Filled.Menu, contentDescription = "Abrir menú", tint = Colores.textoSecundario) }
            },
            actions = {
                if (rol == INVITADO) {
                    // Sin cuenta: en vez de avisos y menú de la cuenta, el botón para entrar.
                    Button(onClick = alIngresar, modifier = Modifier.padding(end = 8.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Ingresar")
                    }
                    return@TopAppBar
                }
                Box {
                    IconButton(onClick = { avisosAbiertos = true }) {
                        BadgedBox(badge = { if (sinLeer > 0) Badge { Text("$sinLeer") } }) {
                            Icon(Icons.Outlined.Notifications, contentDescription = "Notificaciones", tint = Colores.textoSecundario)
                        }
                    }
                    // El administrador no recibe avisos (en el front web son de clientes y empresas).
                    DropdownMenu(expanded = avisosAbiertos, onDismissRequest = { cerrarAvisos() }, modifier = Modifier.widthIn(max = 320.dp)) {
                        if (avisos.isEmpty()) {
                            DropdownMenuItem(text = { Text("Sin notificaciones") }, onClick = { avisosAbiertos = false }, enabled = false)
                        }
                        avisos.take(15).forEach { aviso ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(aviso.titulo, fontWeight = if (aviso.leido) FontWeight.Normal else FontWeight.Bold)
                                        Text(aviso.mensaje, style = MaterialTheme.typography.bodySmall, color = Colores.gris)
                                    }
                                },
                                onClick = {
                                    cerrarAvisos()
                                    alAbrirSolicitudes()
                                },
                            )
                        }
                    }
                }
                Box(Modifier.padding(end = 8.dp)) {
                    Avatar(nombre, Modifier.clickable { cuentaAbierta = true })
                    DropdownMenu(expanded = cuentaAbierta, onDismissRequest = { cuentaAbierta = false }) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(nombre, fontWeight = FontWeight.Bold)
                            Text(email, style = MaterialTheme.typography.bodySmall, color = Colores.gris)
                            Text(nombreRol(rol), style = MaterialTheme.typography.labelSmall, color = Colores.azul)
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Cerrar sesión") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
                            onClick = {
                                cuentaAbierta = false
                                alCerrarSesion()
                            },
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
        )
        HorizontalDivider(color = Colores.borde)
    }
}

@Composable
private fun Avatar(nombre: String, modifier: Modifier = Modifier, tamano: Int = 36) {
    Box(
        modifier
            .size(tamano.dp)
            .clip(CircleShape)
            .background(Colores.texto),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            nombre.take(2).uppercase(),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/** Equivale a components/MenuLateral: grupos plegables por rol, con la pantalla actual destacada. */
@Composable
private fun MenuLateral(
    rol: String,
    actual: Pantalla,
    nombre: String,
    email: String,
    alElegir: (Pantalla) -> Unit,
    alCerrarSesion: () -> Unit,
    alIngresar: () -> Unit,
) {
    // Al abrir, se despliega el grupo de la pantalla actual; después el usuario puede plegarlo o abrir otros.
    val abiertos = remember { mutableStateMapOf<String, Boolean>() }
    val grupos = gruposDe(rol)
    val grupoActual = grupos.firstOrNull { actual in it.pantallas }?.id
    val coloresItem = NavigationDrawerItemDefaults.colors(
        selectedContainerColor = Color.White.copy(alpha = 0.10f),
        unselectedContainerColor = Color.Transparent,
        selectedIconColor = Colores.menuActivo,
        unselectedIconColor = Colores.menuTexto,
        selectedTextColor = Color.White,
        unselectedTextColor = Colores.menuTexto,
    )

    ModalDrawerSheet(
        drawerContainerColor = Colores.menuFondo,
        drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
        modifier = Modifier.width(300.dp),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // Marca y cuenta
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(Colores.azul, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(44.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("VentanaGo", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Text(if (rol == "ADMIN") "Panel de administración" else "Diseña y cotiza tus ventanas", color = Colores.menuTexto, style = MaterialTheme.typography.bodySmall)
                }
            }
            Surface(
                color = Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth(),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(nombre, tamano = 40)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(nombre, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (rol == INVITADO) "Diseña sin cuenta; ingresa para pedir tu cotización." else email,
                            color = Colores.menuTexto, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            grupos.forEach { grupo ->
                val expandido = abiertos[grupo.id] ?: (grupo.id == grupoActual)
                val activo = grupo.id == grupoActual
                Row(
                    Modifier
                        .padding(horizontal = 12.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { abiertos[grupo.id] = !expandido }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(grupo.icono, contentDescription = null, tint = if (activo) Colores.menuActivo else Colores.menuTexto, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(grupo.titulo, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Icon(if (expandido) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = Colores.menuTexto)
                }
                AnimatedVisibility(expandido) {
                    Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        grupo.pantallas.forEach { p ->
                            NavigationDrawerItem(
                                label = { Text(p.titulo, fontWeight = if (p == actual) FontWeight.SemiBold else FontWeight.Normal) },
                                icon = { Icon(p.icono, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                badge = if (p.disponible) null else ({ InsigniaPronto() }),
                                selected = p == actual,
                                onClick = { alElegir(p) },
                                colors = coloresItem,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(44.dp)
                                    .padding(start = 8.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(top = 8.dp))
            NavigationDrawerItem(
                label = { Text(if (rol == INVITADO) "Ingresar o crear cuenta" else "Cerrar sesión") },
                icon = { Icon(if (rol == INVITADO) Icons.AutoMirrored.Outlined.Login else Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
                selected = false,
                onClick = if (rol == INVITADO) alIngresar else alCerrarSesion,
                colors = coloresItem,
                modifier = Modifier.padding(12.dp),
            )
            Text(
                "Versión ${BuildConfig.VERSION_NAME}",
                color = Colores.menuTexto.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 28.dp, bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun InsigniaPronto() {
    Surface(color = Color.White.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
        Text(
            "Pronto",
            color = Colores.menuTexto,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
