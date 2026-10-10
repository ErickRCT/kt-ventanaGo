package cl.ventanago.app.ui.navegacion

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.DesignServices
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material.icons.outlined.Window
import androidx.compose.ui.graphics.vector.ImageVector

/** Pantallas del menú lateral; las que aún no se migran ([disponible] = false) muestran un aviso. */
enum class Pantalla(val titulo: String, val icono: ImageVector, val disponible: Boolean = true) {
    INICIO("Inicio", Icons.Outlined.Home),
    CREAR_COTIZACION("Crear cotización", Icons.Outlined.AddBox),
    COTIZACIONES("Cotizaciones", Icons.AutoMirrored.Outlined.Assignment),
    CLIENTES("Clientes", Icons.Outlined.Group),
    PAUTAS("Pautas", Icons.Outlined.ViewAgenda),
    TIPOS_PAUTA("Tipos de pauta", Icons.Outlined.Category),
    PERFILES("Perfiles", Icons.Outlined.CropFree),
    TIPOS_PERFIL("Tipos de perfil", Icons.Outlined.AccountTree),
    QUINCALLERIA("Quincallería", Icons.Outlined.DesignServices),
    VIDRIOS("Vidrios", Icons.Outlined.Window),
    COLORES("Colores", Icons.Outlined.Palette),
    SERIES("Series", Icons.AutoMirrored.Outlined.ViewList),

    // Vistas de cliente y empresa, que el administrador también ve en el front web.
    DISENAR_VENTANA("Diseñar ventana", Icons.Outlined.ViewInAr),
    CARRITO("Carrito", Icons.Outlined.ShoppingCart),
    MIS_COTIZACIONES("Mis cotizaciones", Icons.Outlined.History),
    PROVEEDORES("Proveedores", Icons.Outlined.Storefront),
    SOLICITUDES("Solicitudes", Icons.Outlined.Inbox),
    MI_PERFIL("Mi perfil", Icons.Outlined.Badge),
}

/** Quien usa la app sin haber entrado: puede diseñar, ver en su pared y armar el carrito. */
const val INVITADO = "INVITADO"

/** [roles]: roles del backend (ADMIN, CLIENTE, PROVEEDOR) o [INVITADO], que ven el grupo. */
class GrupoMenu(val id: String, val titulo: String, val icono: ImageVector, val roles: Set<String>, val pantallas: List<Pantalla>)

/** Mismos grupos que el menú lateral del front web, en el mismo orden: el administrador ve todos. */
val gruposMenu = listOf(
    GrupoMenu(
        "administrador", "Administrador", Icons.Outlined.AdminPanelSettings, setOf("ADMIN"),
        listOf(
            Pantalla.INICIO, Pantalla.CREAR_COTIZACION, Pantalla.COTIZACIONES, Pantalla.CLIENTES,
            Pantalla.PAUTAS, Pantalla.TIPOS_PAUTA, Pantalla.PERFILES, Pantalla.TIPOS_PERFIL,
            Pantalla.QUINCALLERIA, Pantalla.VIDRIOS, Pantalla.COLORES, Pantalla.SERIES,
        ),
    ),
    GrupoMenu(
        "cliente", "Cliente", Icons.Outlined.Person, setOf("ADMIN", "CLIENTE", INVITADO),
        listOf(Pantalla.DISENAR_VENTANA, Pantalla.CARRITO, Pantalla.MIS_COTIZACIONES, Pantalla.PROVEEDORES),
    ),
    GrupoMenu("empresa", "Empresa", Icons.Outlined.Business, setOf("ADMIN", "PROVEEDOR"), listOf(Pantalla.SOLICITUDES, Pantalla.MI_PERFIL)),
)

/** Mi perfil es solo de los proveedores: el administrador no oferta. */
fun gruposDe(rol: String) = gruposMenu.filter { rol in it.roles }.map { g ->
    if (rol == "ADMIN" && Pantalla.MI_PERFIL in g.pantallas) GrupoMenu(g.id, g.titulo, g.icono, g.roles, g.pantallas - Pantalla.MI_PERFIL) else g
}

/** Pantalla con la que parte cada rol (como RUTA_INICIAL de la web). */
fun pantallaInicial(rol: String) = when (rol) {
    "CLIENTE", INVITADO -> Pantalla.DISENAR_VENTANA
    "PROVEEDOR" -> Pantalla.SOLICITUDES
    else -> Pantalla.INICIO
}

fun nombreRol(rol: String) = when (rol) {
    "ADMIN" -> "Administrador"
    "PROVEEDOR" -> "Empresa"
    INVITADO -> "Sin cuenta"
    else -> "Cliente"
}
