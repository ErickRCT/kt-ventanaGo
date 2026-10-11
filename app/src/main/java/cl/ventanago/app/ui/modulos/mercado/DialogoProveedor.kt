package cl.ventanago.app.ui.modulos.mercado

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.ventanago.app.datos.ProveedorDetalle
import cl.ventanago.app.datos.ProveedoresApi
import cl.ventanago.app.datos.mensaje
import cl.ventanago.app.datos.nombreServicio
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.comun.EstadoCargando
import cl.ventanago.app.ui.comun.EstadoError
import cl.ventanago.app.ui.comun.PantallaDialogo
import cl.ventanago.app.ui.comun.fechaLegible
import kotlinx.coroutines.CancellationException

// Ficha pública del proveedor.

/** Ficha pública del proveedor con sus últimas valoraciones. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoProveedor(cuentaId: Long, alCerrar: () -> Unit) {
    var detalle by remember { mutableStateOf<ProveedorDetalle?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableStateOf(0) }
    LaunchedEffect(cuentaId, recarga) {
        try {
            detalle = ProveedoresApi.detalle(cuentaId)
            error = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.mensaje()
        }
    }
    PantallaDialogo(detalle?.proveedor?.nombre ?: "Proveedor", alCerrar) {
        val d = detalle
        when {
            d == null && error != null -> EstadoError(error!!) { error = null; recarga++ }
            d == null -> EstadoCargando("Cargando proveedor…")
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val p = d.proveedor
                NombreProveedor(p)
                if (p.verificado) Text("Proveedor verificado por VentanaGo", style = MaterialTheme.typography.bodySmall, color = Colores.azul)
                Estrellas(p.promedio, p.valoraciones)
                Text(experiencia(p), style = MaterialTheme.typography.bodyMedium, color = Colores.textoSecundario)
                p.descripcion?.takeIf { it.isNotBlank() }?.let { Text(it) }
                if (p.servicios.isNotEmpty()) {
                    Text("Servicios", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(p.servicios.joinToString(", ") { nombreServicio(it) })
                }
                if (p.comunas.isNotEmpty()) {
                    Text("Trabaja en", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(p.comunas.joinToString(", ") { it.nombre })
                }
                Text("Valoraciones", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (d.valoraciones.isEmpty()) Text("Aún no tiene valoraciones.", color = Colores.gris)
                d.valoraciones.forEach { v ->
                    Surface(color = Colores.fondo, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Colores.borde), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                for (i in 1..5) {
                                    Icon(if (i <= v.estrellas) Icons.Filled.Star else Icons.Filled.StarBorder, contentDescription = null, tint = amarillo, modifier = Modifier.size(14.dp))
                                }
                                Text("  ${v.autor.orEmpty()} · ${fechaLegible(v.fecha?.take(10))}", style = MaterialTheme.typography.bodySmall, color = Colores.gris)
                            }
                            v.comentario?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                }
            }
        }
    }
}
