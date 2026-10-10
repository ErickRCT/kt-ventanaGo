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
import cl.ventanago.app.datos.nombreServicio
import cl.ventanago.app.datos.pesos
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.comun.EstadoCargando
import cl.ventanago.app.ui.comun.EstadoError
import cl.ventanago.app.ui.comun.EstadoMensaje
import cl.ventanago.app.ui.comun.LocalAvisos
import cl.ventanago.app.ui.modulos.Etiqueta
import cl.ventanago.app.ui.modulos.cotizaciones.fechaLegible
import cl.ventanago.app.ui.modulos.mercado.EtiquetaEstadoSolicitud
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** En qué está cada solicitud desde el punto de vista del proveedor. */
private enum class Bandeja(val titulo: String) {
    POR_OFERTAR("Por ofertar"),
    OFERTADAS("Ofertadas"),
    GANADAS("Ganadas"),
    CERRADAS("Cerradas"),
    TODAS("Todas"),
}

private fun bandeja(s: Solicitud): Bandeja {
    val mia = s.miOferta
    return when {
        mia == null -> if (s.estado == "PENDIENTE") Bandeja.POR_OFERTAR else Bandeja.CERRADAS
        mia.estado == "ELEGIDA" -> Bandeja.GANADAS
        mia.estado == "VIGENTE" && mia.tipo != "RECHAZADA" && s.estado == "PENDIENTE" -> Bandeja.OFERTADAS
        else -> Bandeja.CERRADAS
    }
}

/** Para el administrador, que no oferta: las bandejas son los estados de la solicitud. */
private val ESTADOS_ADMIN = listOf("PENDIENTE", "ADJUDICADA", "TERMINADA", "CANCELADA", "TODAS")

@Composable
fun EtiquetaOferta(s: Solicitud) {
    val mia = s.miOferta ?: return EtiquetaEstadoSolicitud(s.estado)
    when {
        mia.estado == "ELEGIDA" -> Etiqueta(if (s.estado == "TERMINADA") "Terminada" else "¡Ganada!", Color(0xFFE8F5E9), Colores.exito)
        mia.tipo == "RECHAZADA" -> Etiqueta("No tomada", Color(0xFFEDEFF2), Colores.gris)
        mia.estado == "DESCARTADA" -> Etiqueta(if (s.estado == "CANCELADA") "Cancelada" else "Otro proveedor", Color(0xFFEDEFF2), Colores.gris)
        else -> Etiqueta("Oferta enviada", Colores.azulClaro, Colores.azul)
    }
}

/**
 * Solicitudes que le llegan al proveedor (de su zona y servicios, o a las que lo invitaron) agrupadas por lo que
 * falta hacer; al tocar una se abre para enviar la oferta o seguir la conversación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaSolicitudesEmpresa(esAdmin: Boolean = false) {
    val avisar = LocalAvisos.current
    var solicitudes by remember { mutableStateOf<List<Solicitud>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableIntStateOf(0) }
    var refrescando by remember { mutableStateOf(false) }
    var filtro by rememberSaveable { mutableStateOf(if (esAdmin) "PENDIENTE" else Bandeja.POR_OFERTAR.name) }
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

    fun coincide(s: Solicitud, f: String) =
        if (esAdmin) f == "TODAS" || s.estado == f else f == Bandeja.TODAS.name || bandeja(s).name == f
    val filtros: List<Pair<String, String>> =
        if (esAdmin) ESTADOS_ADMIN.map { it to if (it == "TODAS") "Todas" else cl.ventanago.app.datos.nombreEstadoSolicitud(it) }
        else Bandeja.entries.map { it.name to it.titulo }

    val lista = solicitudes
    when {
        lista == null && error != null -> EstadoError(error!!) { error = null; recarga++ }
        lista == null -> EstadoCargando("Cargando solicitudes…")
        else -> PullToRefreshBox(isRefreshing = refrescando, onRefresh = { refrescando = true; recarga++ }, modifier = Modifier.fillMaxSize()) {
            val visibles = lista.filter { coincide(it, filtro) }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                item {
                    Text("Solicitudes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        if (esAdmin) "Todas las solicitudes del mercado y sus ofertas."
                        else "Pedidos de clientes de tu zona. Envía tu oferta: el cliente compara y elige.",
                        color = Colores.gris,
                    )
                    Row(
                        Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        filtros.forEach { (f, titulo) ->
                            FilterChip(
                                selected = filtro == f,
                                onClick = { filtro = f },
                                label = { Text("$titulo (${lista.count { coincide(it, f) }})") },
                            )
                        }
                    }
                }
                if (visibles.isEmpty()) {
                    item {
                        EstadoMensaje(
                            Icons.Outlined.Inbox,
                            if (lista.isEmpty()) "Aún no llegan solicitudes" else "No hay solicitudes aquí",
                            if (lista.isEmpty()) "Cuando un cliente de tu zona pida cotización, aparecerá aquí. Revisa que tu perfil tenga tus comunas y servicios." else null,
                        )
                    }
                }
                items(visibles, key = { it.numero }) { s ->
                    val porOfertar = !esAdmin && bandeja(s) == Bandeja.POR_OFERTAR
                    Card(
                        onClick = { abierta = s.numero },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, if (porOfertar) Color(0xFFF2C46D) else Colores.borde),
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
                                if (esAdmin) EtiquetaEstadoSolicitud(s.estado) else EtiquetaOferta(s)
                            }
                            Text(
                                listOfNotNull(fechaLegible(s.fecha?.take(10)), s.comuna?.nombre, s.servicios.joinToString(", ") { nombreServicio(it) })
                                    .filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = Colores.gris,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (s.items.isEmpty()) (s.observaciones ?: "Servicio sin ventanas")
                                    else "${s.items.sumOf { it.cantidad }} ventanas · ${s.items.size} tipos" +
                                        (if (s.cantidadOfertas > 0) " · ${s.cantidadOfertas} oferta(s)" else ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Colores.textoSecundario,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                val total = s.miOferta?.total
                                Text(
                                    when {
                                        porOfertar -> "Ofertar ›"
                                        total != null -> pesos(total)
                                        else -> "Ver ›"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = if (porOfertar) Color(0xFF8A5300) else Colores.menuFondo,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    lista?.firstOrNull { it.numero == abierta }?.let { s ->
        DetalleSolicitudEmpresa(s, esAdmin) { actualizada ->
            abierta = null
            if (actualizada != null) {
                solicitudes = solicitudes?.map { if (it.numero == actualizada.numero) actualizada else it }
                avisar("Oferta enviada para la solicitud N° ${actualizada.numero}")
            }
        }
    }
}
