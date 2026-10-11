package cl.ventanago.app.ui.modulos.cliente

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cl.ventanago.app.datos.ItemVentana
import cl.ventanago.app.datos.pesos
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ventana.ConfigVentana
import cl.ventanago.app.ventana.DibujoVentana
import cl.ventanago.app.ventana.colorMarco
import cl.ventanago.app.ventana.colorVidrio
import cl.ventanago.app.ventana.modeloDeTextos

/** Dibujo pequeño de una ventana del carrito o de una solicitud. */
@Composable
fun MiniVentana(item: ItemVentana, modifier: Modifier = Modifier) {
    val config = remember(item) {
        ConfigVentana(
            anchoMm = item.anchoMm.coerceAtLeast(300), altoMm = item.altoMm.coerceAtLeast(300), hojas = item.hojas,
            modelo = modeloDeTextos(item.imagenPauta, item.descripcion),
            colorMarco = colorMarco(item.colorNombre), colorVidrio = colorVidrio(item.vidrioNombre),
        )
    }
    Box(modifier.background(Color(0xFFEFF2F6), RoundedCornerShape(8.dp)).padding(4.dp)) {
        DibujoVentana(config, Modifier.fillMaxSize())
    }
}

fun detalleItem(item: ItemVentana) =
    "${item.anchoMm} × ${item.altoMm} mm · ${item.colorNombre} · ${item.vidrioNombre}" + (item.serieNombre?.let { " · Serie $it" } ?: "")

/** Una ventana pedida u ofertada: dibujo, cantidad, medidas y (si lo tiene) el precio unitario. */
@Composable
fun FilaItem(item: ItemVentana, tamano: Dp = 44.dp, conObservaciones: Boolean = false, sufijoPrecio: String = " c/u") {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MiniVentana(item, Modifier.size(tamano))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text("${item.cantidad} × ${item.descripcion}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(detalleItem(item), style = MaterialTheme.typography.bodySmall, color = Colores.gris)
            if (conObservaciones) {
                item.observaciones?.takeIf { it.isNotBlank() }?.let { Text("Obs.: $it", style = MaterialTheme.typography.bodySmall, color = Colores.gris) }
            }
        }
        item.precioUnitario?.let { Text(pesos(it) + sufijoPrecio, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold) }
    }
}
