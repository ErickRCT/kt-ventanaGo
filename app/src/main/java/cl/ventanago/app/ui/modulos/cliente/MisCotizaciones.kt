package cl.ventanago.app.ui.modulos.cliente

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.ventanago.app.datos.ClienteApi
import cl.ventanago.app.datos.ItemVentana
import cl.ventanago.app.datos.Solicitud
import cl.ventanago.app.datos.mensaje
import cl.ventanago.app.datos.nombreEstadoSolicitud
import cl.ventanago.app.datos.nombreServicio
import cl.ventanago.app.datos.pesos
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.comun.EstadoCargando
import cl.ventanago.app.ui.comun.EstadoError
import cl.ventanago.app.ui.comun.EstadoMensaje
import cl.ventanago.app.ui.comun.LocalAvisos
import cl.ventanago.app.ui.modulos.Etiqueta
import cl.ventanago.app.ui.modulos.cotizaciones.fechaLegible
import cl.ventanago.app.ui.navegacion.Pantalla
import kotlinx.coroutines.CancellationException

@Composable
private fun EtiquetaSolicitud(estado: String) = when (estado) {
    "ACEPTADA" -> Etiqueta("Aceptada", Color(0xFFE8F5E9), Colores.exito)
    "MODIFICADA" -> Etiqueta("Modificada", Colores.azulClaro, Colores.azul)
    "RECHAZADA" -> Etiqueta("Rechazada", Color(0xFFFDECEA), Colores.error)
    else -> Etiqueta(nombreEstadoSolicitud(estado), Color(0xFFFFF4E5), Color(0xFF8A5300))
}

/** Mis cotizaciones (equivale a MisCotizaciones de la web): solicitudes enviadas y la respuesta de la empresa. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMisCotizaciones(abrir: (Pantalla) -> Unit) {
    val avisar = LocalAvisos.current
    var solicitudes by remember { mutableStateOf<List<Solicitud>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableIntStateOf(0) }
    var refrescando by remember { mutableStateOf(false) }

    LaunchedEffect(recarga) {
        try {
            solicitudes = ClienteApi.solicitudes().sortedByDescending { it.numero }
            error = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (solicitudes == null) error = e.mensaje() else avisar(e.mensaje())
        }
        refrescando = false
    }

    val lista = solicitudes
    when {
        lista == null && error != null -> EstadoError(error!!) { error = null; recarga++ }
        lista == null -> EstadoCargando("Cargando tus cotizaciones…")
        lista.isEmpty() -> EstadoMensaje(
            Icons.Outlined.History, "Aún no has pedido cotizaciones",
            "Cuando envíes tu carrito a la empresa, verás aquí su respuesta.", "Diseñar ventana",
        ) { abrir(Pantalla.DISENAR_VENTANA) }
        else -> PullToRefreshBox(isRefreshing = refrescando, onRefresh = { refrescando = true; recarga++ }, modifier = Modifier.fillMaxSize()) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                item {
                    Text("Mis cotizaciones", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Desliza hacia abajo para ver si hay respuestas nuevas.", color = Colores.gris)
                }
                items(lista, key = { it.numero }) { TarjetaSolicitud(it) }
            }
        }
    }
}

@Composable
private fun TarjetaSolicitud(s: Solicitud) {
    var abierta by remember { mutableStateOf(false) }
    Card(
        onClick = { abierta = !abierta },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Colores.borde),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Solicitud N° ${s.numero}", fontWeight = FontWeight.Bold)
                    Text(
                        listOf(fechaLegible(s.fecha?.take(10)), s.servicios.joinToString(", ") { nombreServicio(it) }).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = Colores.gris,
                    )
                }
                EtiquetaSolicitud(s.estado)
                Icon(if (abierta) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = Colores.gris)
            }

            // Respuesta de la empresa
            val r = s.respuesta
            Surface(
                color = when (s.estado) {
                    "ACEPTADA" -> Color(0xFFE8F5E9)
                    "RECHAZADA" -> Color(0xFFFDECEA)
                    "MODIFICADA" -> Colores.azulClaro
                    else -> Colores.fondo
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(10.dp)) {
                    if (r == null) {
                        Text("La empresa aún no responde tu solicitud.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text(
                            when (s.estado) {
                                "ACEPTADA" -> "La empresa aceptó tu solicitud"
                                "MODIFICADA" -> "La empresa propone cambios"
                                "RECHAZADA" -> "La empresa no puede realizar este trabajo"
                                else -> "Respuesta de la empresa"
                            } + (r.fecha?.let { " · ${fechaLegible(it.take(10))}" } ?: ""),
                            fontWeight = FontWeight.SemiBold,
                        )
                        r.mensaje?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        r.total?.let { Text("Total: ${pesos(it)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Colores.menuFondo) }
                    }
                }
            }

            AnimatedVisibility(abierta) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (s.itemsOriginales != null) {
                        Text("Lo que solicitaste", style = MaterialTheme.typography.labelLarge, color = Colores.textoSecundario)
                        s.itemsOriginales.forEach { FilaItem(it) }
                        Text("Propuesta de la empresa", style = MaterialTheme.typography.labelLarge, color = Colores.textoSecundario)
                    }
                    s.items.forEach { FilaItem(it) }
                    s.observaciones?.takeIf { it.isNotBlank() }?.let {
                        Text("Tus comentarios: $it", style = MaterialTheme.typography.bodySmall, color = Colores.gris)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaItem(item: ItemVentana) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MiniVentana(item, Modifier.size(44.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text("${item.cantidad} × ${item.descripcion}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(detalleItem(item), style = MaterialTheme.typography.bodySmall, color = Colores.gris)
        }
        item.precioUnitario?.let { Text(pesos(it), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold) }
    }
}
