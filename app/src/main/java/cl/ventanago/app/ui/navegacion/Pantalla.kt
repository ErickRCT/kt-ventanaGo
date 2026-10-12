package cl.ventanago.app.ui.navegacion

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.DesignServices
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material.icons.outlined.Window
import androidx.compose.ui.graphics.vector.ImageVector
import cl.ventanago.app.datos.Rol

/**
 * Pantallas del menú (cada una es un destino de la navegación); las que aún no se migran ([disponible] = false)
 * muestran un aviso. [corto] e [iconoActivo]: cómo se ven en la barra inferior.
 */
enum class Pantalla(
    val titulo: String,
    val icono: ImageVector,
    val corto: String = titulo,
    val iconoActivo: ImageVector = icono,
    val disponible: Boolean = true,
) {
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
    DISENAR_VENTANA("Diseñar ventana", Icons.Outlined.ViewInAr, "Diseñar", Icons.Filled.ViewInAr),
    CARRITO("Carrito", Icons.Outlined.ShoppingCart, iconoActivo = Icons.Filled.ShoppingCart),
    MIS_COTIZACIONES("Mis cotizaciones", Icons.Outlined.History, "Cotizaciones", Icons.Filled.History),
    PROVEEDORES("Proveedores", Icons.Outlined.Storefront, iconoActivo = Icons.Filled.Storefront),
    SOLICITUDES("Solicitudes", Icons.Outlined.Inbox, iconoActivo = Icons.Filled.Inbox),
    CATALOGO_EMPRESA("Catálogo", Icons.Outlined.Inventory2, iconoActivo = Icons.Filled.Inventory2),
    MI_PERFIL("Mi perfil", Icons.Outlined.Badge, iconoActivo = Icons.Filled.Badge),
}

/** [roles]: quienes ven el grupo en el menú. */
class GrupoMenu(val id: String, val titulo: String, val icono: ImageVector, val roles: Set<Rol>, val pantallas: List<Pantalla>)

/** Mismos grupos que el menú lateral del front web, en el mismo orden: el administrador ve todos. */
val gruposMenu = listOf(
    GrupoMenu(
        "administrador", "Administrador", Icons.Outlined.AdminPanelSettings, setOf(Rol.ADMIN),
        listOf(
            Pantalla.INICIO, Pantalla.CREAR_COTIZACION, Pantalla.COTIZACIONES, Pantalla.CLIENTES,
            Pantalla.PAUTAS, Pantalla.TIPOS_PAUTA, Pantalla.PERFILES, Pantalla.TIPOS_PERFIL,
            Pantalla.QUINCALLERIA, Pantalla.VIDRIOS, Pantalla.COLORES, Pantalla.SERIES,
        ),
    ),
    GrupoMenu(
        "cliente", "Cliente", Icons.Outlined.Person, setOf(Rol.ADMIN, Rol.CLIENTE, Rol.INVITADO),
        listOf(Pantalla.DISENAR_VENTANA, Pantalla.CARRITO, Pantalla.MIS_COTIZACIONES, Pantalla.PROVEEDORES),
    ),
    GrupoMenu(
        "empresa", "Empresa", Icons.Outlined.Business, setOf(Rol.ADMIN, Rol.PROVEEDOR),
        listOf(Pantalla.SOLICITUDES, Pantalla.CATALOGO_EMPRESA, Pantalla.MI_PERFIL),
    ),
)

/** Mi perfil y Catálogo son solo de los proveedores: el administrador no oferta y edita el catálogo desde su menú. */
fun gruposDe(rol: Rol) = gruposMenu.filter { rol in it.roles }.map { g ->
    if (rol == Rol.ADMIN && g.id == "empresa") GrupoMenu(g.id, g.titulo, g.icono, g.roles, g.pantallas - Pantalla.MI_PERFIL - Pantalla.CATALOGO_EMPRESA) else g
}

/** Partes del catálogo que la empresa consulta desde su pestaña Catálogo (solo lectura: lo administra VentanaGo). */
val catalogoEmpresa = listOf(
    Pantalla.SERIES, Pantalla.PAUTAS, Pantalla.TIPOS_PAUTA, Pantalla.PERFILES, Pantalla.TIPOS_PERFIL,
    Pantalla.QUINCALLERIA, Pantalla.VIDRIOS, Pantalla.COLORES,
)

/** Para la empresa, cada parte del catálogo es una subpantalla de su pestaña Catálogo (con flecha para volver, sin barra inferior). */
fun Rol.esCatalogoEmpresa(p: Pantalla) = this == Rol.PROVEEDOR && p in catalogoEmpresa

fun Rol.puedeVer(p: Pantalla) = gruposDe(this).any { p in it.pantallas } || esCatalogoEmpresa(p)

/** Pestañas de la barra inferior: el cliente y la empresa navegan con ella; el administrador (null), con el menú lateral. */
fun pestanasDe(rol: Rol): List<Pantalla>? = if (rol == Rol.ADMIN) null else gruposDe(rol).flatMap { it.pantallas }

/** Pantalla con la que parte cada rol (como RUTA_INICIAL de la web). */
fun pantallaInicial(rol: Rol) = when (rol) {
    Rol.CLIENTE, Rol.INVITADO -> Pantalla.DISENAR_VENTANA
    Rol.PROVEEDOR -> Pantalla.SOLICITUDES
    Rol.ADMIN -> Pantalla.INICIO
}
