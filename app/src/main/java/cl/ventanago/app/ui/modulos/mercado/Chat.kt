package cl.ventanago.app.ui.modulos.mercado

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.ventanago.app.datos.ChatApi
import cl.ventanago.app.datos.MensajeChat
import cl.ventanago.app.datos.mensaje
import cl.ventanago.app.notificaciones.Push
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.comun.EstadoCargando
import cl.ventanago.app.ui.comun.MensajeError
import cl.ventanago.app.ui.comun.PantallaDialogo
import cl.ventanago.app.ui.comun.fechaLegible
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

// Chat entre el cliente y el proveedor de una oferta.

/**
 * Conversación entre el cliente y el proveedor de una oferta. Se revisan mensajes nuevos cada 8 segundos
 * mientras está abierta.
 */
@Composable
fun DialogoChat(ofertaId: Long, titulo: String, puedeEscribir: Boolean, alCerrar: () -> Unit) {
    val alcance = rememberCoroutineScope()
    var mensajes by remember { mutableStateOf<List<MensajeChat>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var texto by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    val lista = rememberLazyListState()

    LaunchedEffect(ofertaId) {
        while (true) {
            try {
                mensajes = ChatApi.mensajes(ofertaId)
                error = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.mensaje()
            }
            // Cada 8 segundos, o apenas llegue una notificación (p. ej. un mensaje nuevo en este chat).
            withTimeoutOrNull(8_000) { Push.llegadas.first() }
        }
    }
    LaunchedEffect(mensajes?.size) { mensajes?.let { if (it.isNotEmpty()) lista.animateScrollToItem(it.size - 1) } }

    PantallaDialogo(titulo, alCerrar) {
        Column(Modifier.fillMaxSize()) {
            error?.let { Box(Modifier.padding(12.dp)) { MensajeError(it) } }
            val m = mensajes
            Box(Modifier.weight(1f)) {
                when {
                    m == null -> EstadoCargando("Cargando conversación…")
                    m.isEmpty() -> Text(
                        "Aún no hay mensajes. Pregunta lo que necesites antes de decidir: plazos, garantía, si incluye retiro de la ventana antigua…",
                        color = Colores.gris,
                        modifier = Modifier.padding(24.dp),
                    )
                    else -> LazyColumn(state = lista, contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(m, key = { it.id }) { BurbujaMensaje(it) }
                    }
                }
            }
            if (puedeEscribir) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Colores.superficie)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { texto = it.take(1000) },
                        placeholder = { Text("Escribe un mensaje") },
                        maxLines = 4,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {
                            val limpio = texto.trim()
                            if (limpio.isEmpty() || enviando) return@IconButton
                            alcance.launch {
                                enviando = true
                                try {
                                    val nuevo = ChatApi.enviar(ofertaId, limpio)
                                    mensajes = mensajes.orEmpty() + nuevo
                                    texto = ""
                                    error = null
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    error = e.mensaje()
                                }
                                enviando = false
                            }
                        },
                        enabled = !enviando && texto.isNotBlank(),
                    ) {
                        if (enviando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = Colores.azul)
                    }
                }
            } else {
                Text("Esta conversación está cerrada.", color = Colores.gris, modifier = Modifier.padding(12.dp))
            }
        }
    }
}

@Composable
private fun BurbujaMensaje(m: MensajeChat) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.mio) Arrangement.End else Arrangement.Start) {
        Surface(
            color = if (m.mio) Colores.azul else Colores.superficie,
            contentColor = if (m.mio) Colores.sobreAzul else Colores.texto,
            shape = RoundedCornerShape(14.dp),
            border = if (m.mio) null else BorderStroke(1.dp, Colores.borde),
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (!m.mio) Text(m.autor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(m.texto)
                Text(
                    m.fecha?.let { "${fechaLegible(it.take(10))} ${it.drop(11).take(5)}" }.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (m.mio) Colores.sobreAzul.copy(alpha = 0.7f) else Colores.gris,
                )
            }
        }
    }
}
