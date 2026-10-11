package cl.ventanago.app.ui.modulos.mercado

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.ventanago.app.datos.Comuna
import cl.ventanago.app.datos.ProveedorResumen
import cl.ventanago.app.datos.Recursos
import cl.ventanago.app.datos.Region
import cl.ventanago.app.datos.mensaje
import cl.ventanago.app.datos.nombreEstadoSolicitud
import cl.ventanago.app.datos.nombreServicio
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.comun.MensajeError
import cl.ventanago.app.ui.comun.Selector
import cl.ventanago.app.ui.modulos.Etiqueta
import kotlinx.coroutines.CancellationException

// Piezas que comparten las pantallas del mercado: estado de la solicitud, estrellas, proveedores y comuna.
// La ficha del proveedor, el chat y las fotos están en sus propios archivos.

@Composable
fun EtiquetaEstadoSolicitud(estado: String) = when (estado) {
    "ADJUDICADA" -> Etiqueta("Adjudicada", Colores.azulClaro, Colores.azul)
    "TERMINADA" -> Etiqueta("Terminada", Colores.exitoFondo, Colores.exito)
    "CANCELADA" -> Etiqueta("Cancelada", Colores.neutroFondo, Colores.gris)
    else -> Etiqueta(nombreEstadoSolicitud(estado), Colores.avisoFondo, Colores.aviso)
}

// ---------- Estrellas ----------

internal val amarillo = Color(0xFFF2A900)

/** Promedio en estrellas con la cantidad de valoraciones; "Sin valoraciones" si no hay. */
@Composable
fun Estrellas(promedio: Double?, cantidad: Long, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (promedio == null || cantidad == 0L) {
            Text("Sin valoraciones aún", style = MaterialTheme.typography.bodySmall, color = Colores.gris)
            return@Row
        }
        for (i in 1..5) {
            val icono = when {
                promedio >= i -> Icons.Filled.Star
                promedio >= i - 0.5 -> Icons.Filled.StarHalf
                else -> Icons.Filled.StarBorder
            }
            Icon(icono, contentDescription = null, tint = amarillo, modifier = Modifier.size(16.dp))
        }
        Text(" ${"%.1f".format(promedio)} ($cantidad)", style = MaterialTheme.typography.bodySmall, color = Colores.textoSecundario)
    }
}

/** Para valorar: cinco estrellas que se tocan. */
@Composable
fun SelectorEstrellas(valor: Int, alCambiar: (Int) -> Unit) {
    Row {
        for (i in 1..5) {
            IconButton(onClick = { alCambiar(i) }) {
                Icon(
                    if (i <= valor) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "$i estrella(s)",
                    tint = amarillo,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

// ---------- Proveedores ----------

@Composable
fun NombreProveedor(p: ProveedorResumen, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(p.nombre, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f, fill = false))
        if (p.verificado) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Filled.Verified, contentDescription = "Verificado", tint = Colores.azul, modifier = Modifier.size(18.dp))
        }
    }
}

/** Experiencia y trabajos hechos en VentanaGo, en una línea. */
fun experiencia(p: ProveedorResumen) = listOfNotNull(
    p.aniosExperiencia.takeIf { it > 0 }?.let { "$it años de experiencia" },
    "${p.trabajos} ${if (p.trabajos == 1L) "trabajo" else "trabajos"} en VentanaGo",
).joinToString(" · ")

@Composable
fun TarjetaProveedor(p: ProveedorResumen, alTocar: () -> Unit, extra: @Composable () -> Unit = {}) {
    Card(
        onClick = alTocar,
        colors = CardDefaults.cardColors(containerColor = Colores.superficie),
        border = BorderStroke(1.dp, Colores.borde),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NombreProveedor(p, Modifier.weight(1f))
                extra()
            }
            Estrellas(p.promedio, p.valoraciones)
            Text(experiencia(p), style = MaterialTheme.typography.bodySmall, color = Colores.gris)
            if (p.servicios.isNotEmpty()) {
                Text(p.servicios.joinToString(" · ") { nombreServicio(it) }, style = MaterialTheme.typography.bodySmall, color = Colores.textoSecundario)
            }
        }
    }
}

// ---------- Comuna ----------

/** Región y luego comuna, como en el formulario de clientes. [alElegir] recibe la comuna (o null al cambiar de región). */
@Composable
fun SelectorComuna(comuna: Comuna?, alElegir: (Comuna?) -> Unit, error: String? = null) {
    var region by remember { mutableStateOf<Region?>(comuna?.region) }
    var regiones by remember { mutableStateOf<List<Region>?>(null) }
    var comunas by remember { mutableStateOf<List<Comuna>?>(null) }
    var errorCarga by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        regiones = try {
            Recursos.regiones()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorCarga = e.mensaje()
            emptyList()
        }
    }
    LaunchedEffect(region?.regionId) {
        val id = region?.regionId ?: return@LaunchedEffect
        comunas = null
        comunas = try {
            Recursos.comunas(id).sortedBy { it.nombre }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorCarga = e.mensaje()
            emptyList()
        }
    }
    errorCarga?.let { MensajeError(it) }
    Selector(
        "Región", regiones.orEmpty(), regiones?.find { it.regionId == region?.regionId } ?: region,
        texto = { it.nombre },
        alElegir = {
            if (it.regionId != region?.regionId) alElegir(null)
            region = it
        },
        cargando = regiones == null,
        error = error.takeIf { region == null },
    )
    Selector(
        "Comuna", comunas.orEmpty(), comunas?.find { it.comunaId == comuna?.comunaId } ?: comuna,
        texto = { it.nombre },
        alElegir = { alElegir(it) },
        habilitado = region != null,
        cargando = region != null && comunas == null,
        error = error.takeIf { region != null && comuna == null },
    )
}
