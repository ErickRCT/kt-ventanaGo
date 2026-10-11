package cl.ventanago.app.ui.navegacion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cl.ventanago.app.BuildConfig
import cl.ventanago.app.datos.Rol
import cl.ventanago.app.ui.BotonTema
import cl.ventanago.app.ui.Colores

/**
 * Barra superior: el título de la pantalla actual, el botón de modo claro/oscuro, la campana de avisos y el menú
 * de la cuenta (o el botón para entrar, sin cuenta). [alVolver]: en una pantalla interna (p. ej. Avisos) se
 * muestra la flecha para volver; [alAbrirMenu]: el administrador abre el menú lateral. [alAbrirAvisos] null
 * oculta la campana.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Encabezado(
    titulo: String,
    rol: Rol,
    nombre: String,
    email: String,
    sinLeer: Int,
    alVolver: (() -> Unit)?,
    alAbrirMenu: (() -> Unit)?,
    alAbrirAvisos: (() -> Unit)?,
    alIngresar: () -> Unit,
    alCerrarSesion: () -> Unit,
) {
    var cuentaAbierta by remember { mutableStateOf(false) }
    Column {
        TopAppBar(
            title = { Text(titulo, style = MaterialTheme.typography.titleLarge, color = Colores.textoFuerte, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = {
                when {
                    alVolver != null -> IconButton(onClick = alVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Colores.textoSecundario)
                    }
                    alAbrirMenu != null -> IconButton(onClick = alAbrirMenu) {
                        Icon(Icons.Filled.Menu, contentDescription = "Abrir menú", tint = Colores.textoSecundario)
                    }
                }
            },
            actions = {
                BotonTema()
                if (rol == Rol.INVITADO) {
                    // Sin cuenta: en vez de avisos y menú de la cuenta, el botón para entrar.
                    FilledTonalButton(onClick = alIngresar, contentPadding = PaddingValues(horizontal = 14.dp), modifier = Modifier.padding(end = 8.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Ingresar")
                    }
                    return@TopAppBar
                }
                alAbrirAvisos?.let {
                    IconButton(onClick = it) {
                        BadgedBox(badge = { if (sinLeer > 0) Badge { Text(if (sinLeer > 9) "9+" else "$sinLeer") } }) {
                            Icon(
                                Icons.Outlined.Notifications,
                                contentDescription = if (sinLeer > 0) "Avisos, $sinLeer sin leer" else "Avisos",
                                tint = Colores.textoSecundario,
                            )
                        }
                    }
                }
                Box(Modifier.padding(end = 4.dp)) {
                    IconButton(onClick = { cuentaAbierta = true }) { Avatar(nombre, tamano = 32) }
                    DropdownMenu(expanded = cuentaAbierta, onDismissRequest = { cuentaAbierta = false }) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(nombre, fontWeight = FontWeight.Bold)
                            Text(email, style = MaterialTheme.typography.bodySmall, color = Colores.gris)
                            Text(rol.nombre, style = MaterialTheme.typography.labelSmall, color = Colores.azul)
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
                        Text(
                            "Versión ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Colores.gris,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Colores.superficie),
        )
        HorizontalDivider(color = Colores.borde)
    }
}

@Composable
internal fun Avatar(nombre: String, modifier: Modifier = Modifier, tamano: Int = 36) {
    Box(
        modifier
            .size(tamano.dp)
            .clip(CircleShape)
            .background(Colores.azul),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            nombre.take(2).uppercase(),
            color = Colores.sobreAzul,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
