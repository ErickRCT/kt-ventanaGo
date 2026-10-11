package cl.ventanago.app.ui.navegacion

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
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
import cl.ventanago.app.datos.Rol
import cl.ventanago.app.ui.Colores

/** Equivale a components/MenuLateral: grupos plegables por rol, con la pantalla actual destacada. */
@Composable
fun MenuLateral(
    rol: Rol,
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
                        .background(Colores.marca, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(44.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("VentanaGo", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Text(if (rol == Rol.ADMIN) "Panel de administración" else "Diseña y cotiza tus ventanas", color = Colores.menuTexto, style = MaterialTheme.typography.bodySmall)
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
                            if (rol == Rol.INVITADO) "Diseña sin cuenta; ingresa para pedir tu cotización." else email,
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
                label = { Text(if (rol == Rol.INVITADO) "Ingresar o crear cuenta" else "Cerrar sesión") },
                icon = { Icon(if (rol == Rol.INVITADO) Icons.AutoMirrored.Outlined.Login else Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
                selected = false,
                onClick = if (rol == Rol.INVITADO) alIngresar else alCerrarSesion,
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
