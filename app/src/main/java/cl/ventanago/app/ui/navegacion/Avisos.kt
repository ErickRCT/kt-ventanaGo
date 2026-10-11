package cl.ventanago.app.ui.navegacion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.ventanago.app.datos.Aviso
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.comun.EstadoCargando
import cl.ventanago.app.ui.comun.EstadoMensaje
import cl.ventanago.app.ui.comun.fechaHoraCorta

/**
 * Avisos de la cuenta a pantalla completa: los nuevos arriba y los anteriores debajo. Al salir de la pantalla
 * quedan todos como leídos. [alAbrir] recibe el número de la solicitud del aviso tocado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAvisos(modelo: AvisosViewModel, alAbrir: (Long?) -> Unit) {
    DisposableEffect(modelo) { onDispose { modelo.marcarLeidos() } }
    val avisos = modelo.avisos
    when {
        !modelo.cargados && avisos.isEmpty() -> EstadoCargando("Cargando tus avisos…")
        else -> PullToRefreshBox(isRefreshing = modelo.refrescando, onRefresh = modelo::refrescar, modifier = Modifier.fillMaxSize()) {
            if (avisos.isEmpty()) {
                EstadoMensaje(
                    Icons.Outlined.NotificationsNone,
                    "Sin avisos por ahora",
                    "Aquí verás las ofertas, los mensajes y las novedades de tus solicitudes.",
                )
                return@PullToRefreshBox
            }
            val (nuevos, anteriores) = avisos.partition { !it.leido }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (nuevos.isNotEmpty()) grupo("Nuevos", nuevos, alAbrir)
                if (anteriores.isNotEmpty()) grupo(if (nuevos.isEmpty()) "Todos" else "Anteriores", anteriores, alAbrir)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.grupo(titulo: String, avisos: List<Aviso>, alAbrir: (Long?) -> Unit) {
    item(key = "titulo-$titulo") {
        Text(titulo, style = MaterialTheme.typography.titleSmall, color = Colores.textoSecundario, modifier = Modifier.padding(top = 4.dp))
    }
    item(key = "grupo-$titulo") {
        Card(
            colors = CardDefaults.cardColors(containerColor = Colores.superficie),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            avisos.forEachIndexed { i, aviso ->
                if (i > 0) HorizontalDivider(color = Colores.borde, modifier = Modifier.padding(start = 68.dp))
                FilaAviso(aviso) { alAbrir(aviso.solicitudNumero) }
            }
        }
    }
}

/** Ícono y color según el tipo de aviso (se deduce del título que arma el backend). */
private class Tipo(val icono: ImageVector, val color: @Composable () -> Color, val fondo: @Composable () -> Color)

private fun tipoDe(aviso: Aviso): Tipo {
    val t = aviso.titulo.lowercase()
    return when {
        t.startsWith("mensaje") -> Tipo(Icons.AutoMirrored.Outlined.Chat, { Colores.azul }, { Colores.azulClaro })
        t.contains("te eligieron") -> Tipo(Icons.Outlined.EmojiEvents, { Colores.exito }, { Colores.exitoFondo })
        t.contains("valoraron") -> Tipo(Icons.Outlined.StarOutline, { Colores.aviso }, { Colores.avisoFondo })
        t.contains("cancelada") || t.contains("otro proveedor") -> Tipo(Icons.Outlined.Block, { Colores.gris }, { Colores.neutroFondo })
        t.contains("oferta") -> Tipo(Icons.Outlined.LocalOffer, { Colores.exito }, { Colores.exitoFondo })
        t.contains("solicitud") -> Tipo(Icons.Outlined.Inbox, { Colores.azul }, { Colores.azulClaro })
        else -> Tipo(Icons.Outlined.Notifications, { Colores.azul }, { Colores.azulClaro })
    }
}

@Composable
private fun FilaAviso(aviso: Aviso, alTocar: () -> Unit) {
    val tipo = tipoDe(aviso)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = alTocar)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(tipo.fondo(), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(tipo.icono, contentDescription = null, tint = tipo.color(), modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                aviso.titulo,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (aviso.leido) FontWeight.Normal else FontWeight.SemiBold,
                color = Colores.texto,
            )
            if (aviso.mensaje.isNotBlank()) {
                Text(aviso.mensaje, style = MaterialTheme.typography.bodyMedium, color = Colores.textoSecundario, modifier = Modifier.padding(top = 2.dp))
            }
            Text(fechaHoraCorta(aviso.fecha), style = MaterialTheme.typography.labelMedium, color = Colores.gris, modifier = Modifier.padding(top = 4.dp))
        }
        if (!aviso.leido) {
            Box(
                Modifier
                    .padding(start = 8.dp, top = 6.dp)
                    .size(10.dp)
                    .background(Colores.azul, CircleShape),
            )
        }
    }
}
