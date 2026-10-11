package cl.ventanago.app.ui.navegacion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.ventanago.app.datos.Rol
import cl.ventanago.app.ui.Colores

/**
 * Equivale a components/Encabezado: botón de menú, marca, notificaciones y menú de la cuenta.
 * [alAbrirAviso] recibe el número de la solicitud del aviso tocado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Encabezado(
    rol: Rol,
    nombre: String,
    email: String,
    alIngresar: () -> Unit,
    alAbrirMenu: () -> Unit,
    alAbrirAviso: (Long?) -> Unit,
    alCerrarSesion: () -> Unit,
) {
    var avisosAbiertos by remember { mutableStateOf(false) }
    var cuentaAbierta by remember { mutableStateOf(false) }
    // Cada cuenta con su propio modelo de avisos (el administrador no recibe avisos).
    val modeloAvisos: AvisosViewModel = viewModel(key = "avisos-$email")
    if (rol.recibeAvisos) LaunchedEffect(modeloAvisos) { modeloAvisos.sondear() }
    val avisos = modeloAvisos.avisos
    val sinLeer = modeloAvisos.sinLeer
    fun cerrarAvisos() {
        avisosAbiertos = false
        modeloAvisos.marcarLeidos()
    }
    Column {
        TopAppBar(
            title = { Text("VentanaGo", fontWeight = FontWeight.Bold, color = Colores.menuFondo) },
            navigationIcon = {
                IconButton(onClick = alAbrirMenu) { Icon(Icons.Filled.Menu, contentDescription = "Abrir menú", tint = Colores.textoSecundario) }
            },
            actions = {
                if (rol == Rol.INVITADO) {
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
                                    alAbrirAviso(aviso.solicitudNumero)
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
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
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

