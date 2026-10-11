package cl.ventanago.app.ui.navegacion

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

// Rutas de la navegación: cada [Pantalla] del menú es un destino con su nombre como ruta, más el detalle de una
// solicitud para la empresa (al que también se llega tocando una notificación).

const val RUTA_SOLICITUD_EMPRESA = "solicitud/{numero}"
const val ARG_NUMERO = "numero"

/** Avisos de la cuenta (la campana del encabezado). */
const val RUTA_AVISOS = "avisos"

fun rutaSolicitudEmpresa(numero: Long) = "solicitud/$numero"

/** Detalle de una solicitud del cliente (desde Mis cotizaciones o al tocar un aviso). */
const val RUTA_MI_SOLICITUD = "cotizacion/{numero}"

fun rutaMiSolicitud(numero: Long) = "cotizacion/$numero"

/** Pantalla del menú que corresponde a una ruta (el detalle de una solicitud cuenta como su lista). */
fun pantallaDeRuta(ruta: String?): Pantalla? = when {
    ruta == null -> null
    ruta == RUTA_SOLICITUD_EMPRESA -> Pantalla.SOLICITUDES
    ruta == RUTA_MI_SOLICITUD -> Pantalla.MIS_COTIZACIONES
    else -> Pantalla.entries.firstOrNull { it.name == ruta }
}

/**
 * Desde el menú lateral: cada sección se abre sobre la pantalla inicial (atrás vuelve a ella) y recuerda dónde
 * quedó si se vuelve a abrir.
 */
fun NavController.abrirSeccion(p: Pantalla) = navigate(p.name) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/**
 * Desde el contenido (p. ej. "Diseñar ventana" en el carrito): se apila, así atrás vuelve a donde se estaba. Si
 * la pantalla ya está en el historial se vuelve a ella en vez de repetirla.
 */
fun NavController.abrir(p: Pantalla) = navigate(p.name) {
    popUpTo(p.name) { inclusive = true }
    launchSingleTop = true
}

/** Las pantallas internas (detalle, avisos) entran desde la derecha y al volver salen hacia ella. */
fun AnimatedContentTransitionScope<NavBackStackEntry>.entraDesdeLaDerecha(): EnterTransition =
    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(250)) + fadeIn(tween(250))

fun AnimatedContentTransitionScope<NavBackStackEntry>.saleHaciaLaDerecha(): ExitTransition =
    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(250)) + fadeOut(tween(250))
