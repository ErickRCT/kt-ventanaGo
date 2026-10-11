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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cl.ventanago.app.datos.Solicitud
import cl.ventanago.app.datos.nombreEstadoSolicitud
import cl.ventanago.app.datos.nombreServicio
import cl.ventanago.app.datos.pesos
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.comun.EsqueletoLista
import cl.ventanago.app.ui.comun.EstadoError
import cl.ventanago.app.ui.comun.EstadoMensaje
import cl.ventanago.app.ui.comun.MostrarAvisos
import cl.ventanago.app.ui.comun.fechaLegible
import cl.ventanago.app.ui.modulos.Etiqueta
import cl.ventanago.app.ui.modulos.mercado.EtiquetaEstadoSolicitud

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
        mia.estado == "ELEGIDA" -> Etiqueta(if (s.estado == "TERMINADA") "Terminada" else "¡Ganada!", Colores.exitoFondo, Colores.exito)
        mia.tipo == "RECHAZADA" -> Etiqueta("No tomada", Colores.neutroFondo, Colores.gris)
        mia.estado == "DESCARTADA" -> Etiqueta(if (s.estado == "CANCELADA") "Cancelada" else "Otro proveedor", Colores.neutroFondo, Colores.gris)
        else -> Etiqueta("Oferta enviada", Colores.azulClaro, Colores.azul)
    }
}

/**
 * Solicitudes que le llegan al proveedor (de su zona y servicios, o a las que lo invitaron) agrupadas por lo que
 * falta hacer; al tocar una se abre su detalle ([alAbrir]) para enviar la oferta o seguir la conversación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaSolicitudesEmpresa(esAdmin: Boolean, vm: SolicitudesEmpresaViewModel, alAbrir: (Long) -> Unit) {
    MostrarAvisos(vm)
    LaunchedEffect(vm) { vm.sondear() }
    val filtro = vm.filtro ?: if (esAdmin) "PENDIENTE" else Bandeja.POR_OFERTAR.name

    fun coincide(s: Solicitud, f: String) =
        if (esAdmin) f == "TODAS" || s.estado == f else f == Bandeja.TODAS.name || bandeja(s).name == f
    val filtros: List<Pair<String, String>> =
        if (esAdmin) ESTADOS_ADMIN.map { it to if (it == "TODAS") "Todas" else nombreEstadoSolicitud(it) }
        else Bandeja.entries.map { it.name to it.titulo }

    val lista = vm.solicitudes
    when {
        lista == null && vm.error != null -> EstadoError(vm.error!!) { vm.reintentar() }
        lista == null -> EsqueletoLista("Cargando solicitudes…", conImagen = false)
        else -> PullToRefreshBox(isRefreshing = vm.refrescando, onRefresh = { vm.cargar(manual = true) }, modifier = Modifier.fillMaxSize()) {
            val visibles = lista.filter { coincide(it, filtro) }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                item {
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
                                onClick = { vm.filtro = f },
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
                        onClick = { alAbrir(s.numero) },
                        colors = CardDefaults.cardColors(containerColor = Colores.superficie),
                        border = BorderStroke(1.dp, if (porOfertar) Colores.avisoBorde else Colores.borde),
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
                                    color = if (porOfertar) Colores.aviso else Colores.textoFuerte,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
