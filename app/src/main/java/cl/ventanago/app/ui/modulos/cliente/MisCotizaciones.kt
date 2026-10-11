package cl.ventanago.app.ui.modulos.cliente

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cl.ventanago.app.datos.Solicitud
import cl.ventanago.app.datos.nombreServicio
import cl.ventanago.app.datos.ofertaElegida
import cl.ventanago.app.datos.ofertasValidas
import cl.ventanago.app.datos.pesos
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.comun.EsqueletoLista
import cl.ventanago.app.ui.comun.EstadoError
import cl.ventanago.app.ui.comun.EstadoMensaje
import cl.ventanago.app.ui.comun.MostrarAvisos
import cl.ventanago.app.ui.comun.fechaLegible
import cl.ventanago.app.ui.modulos.mercado.EtiquetaEstadoSolicitud
import cl.ventanago.app.ui.navegacion.Pantalla

/**
 * Mis cotizaciones: un resumen de cada solicitud del cliente (lo pedido, en qué paso va y cuántas ofertas tiene).
 * Al tocar una se abre su detalle ([alAbrir] recibe el número), donde se comparan y eligen las ofertas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMisCotizaciones(abrir: (Pantalla) -> Unit, vm: MisCotizacionesViewModel, alAbrir: (Long) -> Unit) {
    MostrarAvisos(vm)
    val lista = vm.solicitudes
    when {
        lista == null && vm.error != null -> EstadoError(vm.error!!) { vm.reintentar() }
        lista == null -> EsqueletoLista("Cargando tus cotizaciones…", filas = 4)
        lista.isEmpty() -> EstadoMensaje(
            Icons.Outlined.History, "Aún no has pedido cotizaciones",
            "Cuando envíes tu carrito, los proveedores de tu zona te harán ofertas y las verás aquí.", "Diseñar ventana",
        ) { abrir(Pantalla.DISENAR_VENTANA) }
        else -> PullToRefreshBox(isRefreshing = vm.refrescando, onRefresh = { vm.cargar(manual = true) }, modifier = Modifier.fillMaxSize()) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                item {
                    Text("Toca una solicitud para comparar sus ofertas y elegir. Desliza hacia abajo para actualizar.", color = Colores.gris)
                }
                items(lista, key = { it.numero }) { s -> TarjetaSolicitud(s, Modifier.animateItem()) { alAbrir(s.numero) } }
            }
        }
    }
}

@Composable
private fun TarjetaSolicitud(s: Solicitud, modifier: Modifier = Modifier, alTocar: () -> Unit) {
    Card(
        onClick = alTocar,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Colores.superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Solicitud N° ${s.numero}", style = MaterialTheme.typography.titleMedium, color = Colores.textoFuerte)
                    Text(
                        listOfNotNull(fechaLegible(s.fecha?.take(10)), s.comuna?.nombre).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = Colores.gris,
                    )
                }
                EtiquetaEstadoSolicitud(s.estado)
            }
            LoPedido(s)
            SeguimientoCompacto(s)
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (texto, color) = resumenEstado(s)
                Text(texto, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = color, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Colores.gris)
            }
        }
    }
}

/** Miniaturas de las ventanas pedidas (hasta tres) o, sin ventanas, los servicios. */
@Composable
private fun LoPedido(s: Solicitud) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (s.items.isEmpty()) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(Colores.superficieTenue, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.Build, contentDescription = null, tint = Colores.gris) }
            Spacer(Modifier.width(10.dp))
            Text(
                s.servicios.joinToString(", ") { nombreServicio(it) }.ifBlank { "Servicio sin ventanas" },
                style = MaterialTheme.typography.bodyMedium, color = Colores.textoSecundario, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            return@Row
        }
        s.items.take(3).forEach {
            MiniVentana(it, Modifier.size(44.dp))
            Spacer(Modifier.width(6.dp))
        }
        if (s.items.size > 3) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(Colores.superficieTenue, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) { Text("+${s.items.size - 3}", style = MaterialTheme.typography.labelLarge, color = Colores.textoSecundario) }
            Spacer(Modifier.width(6.dp))
        }
        val unidades = s.items.sumOf { it.cantidad }
        Text(
            "$unidades ${if (unidades == 1) "ventana" else "ventanas"}",
            style = MaterialTheme.typography.bodyMedium, color = Colores.textoSecundario, modifier = Modifier.padding(start = 4.dp),
        )
    }
}

/** La línea que resume en qué va la solicitud, con su color. */
@Composable
private fun resumenEstado(s: Solicitud): Pair<String, Color> {
    val validas = s.ofertasValidas()
    return when (s.estado) {
        "ADJUDICADA" -> "Elegiste a ${s.ofertaElegida()?.proveedor?.nombre ?: "un proveedor"}" +
            (s.ofertaElegida()?.total?.let { " · ${pesos(it)}" } ?: "") to Colores.azul
        "TERMINADA" -> (s.valoracion?.let { "Trabajo terminado · ${"★".repeat(it.estrellas)}" } ?: "Trabajo terminado") to Colores.exito
        "CANCELADA" -> "Cancelaste esta solicitud" to Colores.gris
        else -> if (validas.isEmpty()) {
            "Esperando ofertas de los proveedores" to Colores.gris
        } else {
            val desde = validas.mapNotNull { it.total }.minOrNull()
            "${validas.size} ${if (validas.size == 1) "oferta" else "ofertas"}" + (desde?.let { " · desde ${pesos(it)}" } ?: "") to Colores.exito
        }
    }
}

// ---------- Seguimiento ----------

/** Pasos por los que pasa una solicitud del cliente. */
internal val PASOS = listOf("Enviada", "Ofertas", "Elegido", "Terminado")

/** Cuántos [PASOS] están cumplidos (1 a 4). Una cancelada queda en el paso en que se canceló. */
internal fun pasosCumplidos(s: Solicitud) = when (s.estado) {
    "TERMINADA" -> 4
    "ADJUDICADA" -> 3
    else -> if (s.ofertasValidas().isNotEmpty()) 2 else 1
}

/** Barra de cuatro tramos: los cumplidos en azul (gris si se canceló). */
@Composable
private fun SeguimientoCompacto(s: Solicitud) {
    val cumplidos = pasosCumplidos(s)
    val color = if (s.estado == "CANCELADA") Colores.gris else Colores.azul
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        PASOS.indices.forEach { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(if (i < cumplidos) color else Colores.borde, RoundedCornerShape(3.dp)),
            )
        }
    }
}

/** Los cuatro pasos con su nombre: cumplidos con un visto, el siguiente marcado, los demás en gris. */
@Composable
internal fun Seguimiento(s: Solicitud) {
    val cumplidos = pasosCumplidos(s)
    val cancelada = s.estado == "CANCELADA"
    val color = if (cancelada) Colores.gris else Colores.azul
    Row(Modifier.fillMaxWidth()) {
        PASOS.forEachIndexed { i, paso ->
            val hecho = i < cumplidos
            val actual = i == cumplidos && !cancelada
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Tramo que une con el paso anterior (y con el siguiente), a cada lado del círculo.
                    Box(
                        Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(if (i == 0) Color.Transparent else if (hecho) color else Colores.borde),
                    )
                    Box(
                        Modifier
                            .size(28.dp)
                            .background(
                                when {
                                    hecho -> color
                                    actual -> Colores.azulClaro
                                    else -> Colores.superficieTenue
                                },
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (hecho) Icon(Icons.Filled.Check, contentDescription = null, tint = Colores.sobreAzul, modifier = Modifier.size(16.dp))
                        else Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = if (actual) Colores.azul else Colores.gris)
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(if (i == PASOS.lastIndex) Color.Transparent else if (i + 1 < cumplidos) color else Colores.borde),
                    )
                }
                Text(
                    paso,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (hecho || actual) Colores.textoFuerte else Colores.gris,
                    fontWeight = if (actual) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}
