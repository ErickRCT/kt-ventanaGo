package cl.ventanago.app.ui.modulos.cotizaciones

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cl.ventanago.app.datos.Cotizacion
import cl.ventanago.app.datos.Cotizaciones
import cl.ventanago.app.datos.nombreEstado
import cl.ventanago.app.datos.pesos
import cl.ventanago.app.datos.resumen
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.comun.LocalAvisos
import cl.ventanago.app.ui.comun.Mantenedor
import cl.ventanago.app.ui.modulos.Etiqueta

@Composable
private fun EtiquetaEstado(estado: String?) = when (nombreEstado(estado)) {
    "Aprobada" -> Etiqueta("Aprobada", Color(0xFFE8F5E9), Colores.exito)
    "Rechazada" -> Etiqueta("Rechazada", Color(0xFFFDECEA), Colores.error)
    else -> Etiqueta(nombreEstado(estado), Colores.fondo, Colores.textoSecundario)
}

/**
 * Lista de cotizaciones (equivale a NuevoCotizaciones de la web); al tocar una se abre el editor.
 * Con [nuevaAlAbrir] (menú "Crear cotización") el editor de una cotización nueva se abre de inmediato.
 */
@Composable
fun PantallaCotizaciones(nuevaAlAbrir: Boolean = false, alCerrarNueva: () -> Unit = {}) {
    val avisar = LocalAvisos.current
    var nuevaAbierta by remember { mutableStateOf(nuevaAlAbrir) }

    Mantenedor<Cotizacion>(
        titulo = "Cotizaciones",
        descripcion = "Toca una cotización para ver sus ventanas, ajustar precios o generar el PDF.",
        cargar = { Cotizaciones.listar().sortedByDescending { it.cotizacionId ?: 0 } },
        clave = { it.cotizacionId ?: it.hashCode() },
        textoBusqueda = { c ->
            listOfNotNull(
                c.cotizacionId?.toString(), c.nombreCotizacion, c.cliente?.nombre, c.cliente?.rut, nombreEstado(c.estado),
            ).joinToString(" ")
        },
        nombre = { "Cotización N° ${it.cotizacionId}" },
        textoNuevo = "Nueva",
        formulario = { item, terminar -> EditorCotizacion(item?.cotizacionId, terminar) },
    ) { c ->
        val total = c.resumen().total
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("N° ${c.cotizacionId}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Colores.azul)
                Text(
                    "  ${c.nombreCotizacion?.takeIf { it.isNotBlank() } ?: "Sin nombre"}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                listOfNotNull(c.cliente?.nombre ?: "Sin cliente", fechaLegible(c.fecha).takeIf { it.isNotBlank() }).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = Colores.gris,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                EtiquetaEstado(c.estado)
                Etiqueta("${c.ventanas.orEmpty().sumOf { it.cantidad ?: 1 }} unid.", Colores.fondo, Colores.textoSecundario)
                Text(pesos(total), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Colores.menuFondo)
            }
        }
    }

    if (nuevaAbierta) {
        EditorCotizacion(null) { mensaje ->
            nuevaAbierta = false
            mensaje?.let(avisar)
            alCerrarNueva()
        }
    }
}
