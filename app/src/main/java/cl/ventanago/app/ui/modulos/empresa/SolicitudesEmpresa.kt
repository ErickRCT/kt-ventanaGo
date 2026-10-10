package cl.ventanago.app.ui.modulos.empresa

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cl.ventanago.app.datos.EmpresaApi
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

private val FILTROS = listOf("PENDIENTE", "TODAS", "ACEPTADA", "MODIFICADA", "RECHAZADA")

@Composable
fun EtiquetaEstadoSolicitud(estado: String) = when (estado) {
    "ACEPTADA" -> Etiqueta("Aceptada", Color(0xFFE8F5E9), Colores.exito)
    "MODIFICADA" -> Etiqueta("Modificada", Colores.azulClaro, Colores.azul)
    "RECHAZADA" -> Etiqueta("Rechazada", Color(0xFFFDECEA), Colores.error)
    else -> Etiqueta(nombreEstadoSolicitud(estado), Color(0xFFFFF4E5), Color(0xFF8A5300))
}

/** Total de lo que la empresa cotizó, o null si falta algún precio. */
fun totalSolicitud(s: Solicitud): Long? =
    if (s.items.any { it.precioUnitario == null }) null else s.items.sumOf { (it.precioUnitario ?: 0) * it.cantidad }

/**
 * Solicitudes de cotización de los clientes (equivale a SolicitudesEmpresa de la web): filtradas por estado,
 * y al tocar una se abre para aceptarla con precio, modificarla o rechazarla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaSolicitudesEmpresa() {
    val avisar = LocalAvisos.current
    var solicitudes by remember { mutableStateOf<List<Solicitud>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableIntStateOf(0) }
    var refrescando by remember { mutableStateOf(false) }
    var filtro by rememberSaveable { mutableStateOf("PENDIENTE") }
    var abierta by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(recarga) {
        // Las solicitudes nuevas llegan de los clientes: se revisan cada minuto mientras la pantalla está abierta.
        while (true) {
            try {
                solicitudes = EmpresaApi.solicitudes().sortedByDescending { it.numero }
                error = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (solicitudes == null) error = e.mensaje() else if (refrescando) avisar(e.mensaje())
            }
            refrescando = false
            delay(60_000)
        }
    }

    val lista = solicitudes
    when {
        lista == null && error != null -> EstadoError(error!!) { error = null; recarga++ }
        lista == null -> EstadoCargando("Cargando solicitudes…")
        else -> PullToRefreshBox(isRefreshing = refrescando, onRefresh = { refrescando = true; recarga++ }, modifier = Modifier.fillMaxSize()) {
            val visibles = lista.filter { filtro == "TODAS" || it.estado == filtro }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                item {
                    Text("Solicitudes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Pedidos de cotización de los clientes. Responde con precio, propón cambios o recházalos.", color = Colores.gris)
                    Row(
                        Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FILTROS.forEach { f ->
                            val cantidad = lista.count { f == "TODAS" || it.estado == f }
                            FilterChip(
                                selected = filtro == f,
                                onClick = { filtro = f },
                                label = { Text("${if (f == "TODAS") "Todas" else nombreEstadoSolicitud(f)} ($cantidad)") },
                            )
                        }
                    }
                }
                if (visibles.isEmpty()) {
                    item {
                        EstadoMensaje(
                            Icons.Outlined.Inbox,
                            if (lista.isEmpty()) "Aún no llegan solicitudes" else "No hay solicitudes en este estado",
                            if (lista.isEmpty()) "Cuando un cliente envíe su carrito, aparecerá aquí." else null,
                        )
                    }
                }
                items(visibles, key = { it.numero }) { s ->
                    Card(
                        onClick = { abierta = s.numero },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, if (s.estado == "PENDIENTE") Color(0xFFF2C46D) else Colores.borde),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("N° ${s.numero}", fontWeight = FontWeight.Bold, color = Colores.azul)
                                Text(
                                    "  ${s.contacto?.nombre.orEmpty()}",
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                EtiquetaEstadoSolicitud(s.estado)
                            }
                            Text(
                                listOf(fechaLegible(s.fecha?.take(10)), s.servicios.joinToString(", ") { nombreServicio(it) })
                                    .filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = Colores.gris,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${s.items.sumOf { it.cantidad }} ventanas · ${s.items.size} tipos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Colores.textoSecundario,
                                    modifier = Modifier.weight(1f),
                                )
                                val total = totalSolicitud(s)
                                Text(
                                    if (total == null || s.estado == "RECHAZADA") (if (s.estado == "PENDIENTE") "Responder ›" else "—") else pesos(total),
                                    fontWeight = FontWeight.Bold,
                                    color = if (s.estado == "PENDIENTE") Color(0xFF8A5300) else Colores.menuFondo,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    lista?.firstOrNull { it.numero == abierta }?.let { s ->
        DetalleSolicitudEmpresa(s) { actualizada ->
            abierta = null
            if (actualizada != null) {
                solicitudes = solicitudes?.map { if (it.numero == actualizada.numero) actualizada else it }
                avisar("Solicitud N° ${actualizada.numero} ${nombreEstadoSolicitud(actualizada.estado).lowercase()}")
            }
        }
    }
}
