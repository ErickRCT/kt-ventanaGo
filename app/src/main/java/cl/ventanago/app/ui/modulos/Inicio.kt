package cl.ventanago.app.ui.modulos

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.ventanago.app.datos.Recursos
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.comun.EstadoMensaje
import cl.ventanago.app.ui.navegacion.Pantalla
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Cantidad de registros de cada mantenedor, para el resumen de Inicio. */
private val conteos: Map<Pantalla, suspend () -> Int> = mapOf(
    Pantalla.CLIENTES to { Recursos.clientes.listar().size },
    Pantalla.PAUTAS to { Recursos.pautas.listar().size },
    Pantalla.SERIES to { Recursos.series.listar().size },
    Pantalla.PERFILES to { Recursos.perfiles.listar().size },
    Pantalla.TIPOS_PERFIL to { Recursos.tiposPerfil.listar().size },
    Pantalla.QUINCALLERIA to { Recursos.quincallerias.listar().size },
    Pantalla.VIDRIOS to { Recursos.vidrios.listar().size },
    Pantalla.COLORES to { Recursos.colores.listar().size },
    Pantalla.TIPOS_PAUTA to { Recursos.tiposPauta.listar().size },
)

@Composable
fun PantallaInicio(nombre: String, abrir: (Pantalla) -> Unit) {
    // null = cargando, -1 = no se pudo cargar
    val totales = remember { mutableStateMapOf<Pantalla, Int?>() }
    LaunchedEffect(Unit) {
        coroutineScope {
            conteos.forEach { (pantalla, contar) ->
                async {
                    totales[pantalla] = try {
                        contar()
                    } catch (_: Exception) {
                        -1
                    }
                }
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(listOf(Colores.azul, Colores.menuFondo)),
                        RoundedCornerShape(16.dp),
                    )
                    .padding(20.dp),
            ) {
                Text("Hola, $nombre", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Panel de administración de VentanaGo. Desde el menú puedes mantener el catálogo y los clientes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                "Catálogo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        items(conteos.keys.toList()) { pantalla ->
            Card(
                onClick = { abrir(pantalla) },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(Colores.azulClaro, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(pantalla.icono, contentDescription = null, tint = Colores.azul) }
                    Spacer(Modifier.height(10.dp))
                    when (val total = totales[pantalla]) {
                        null -> CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 2.dp)
                        -1 -> Text("—", style = MaterialTheme.typography.headlineSmall, color = Colores.gris)
                        else -> Text(total.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(pantalla.titulo, style = MaterialTheme.typography.bodyMedium, color = Colores.gris)
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .background(Color(0xFFFFF4E5), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Construction, contentDescription = null, tint = Color(0xFF8A5300))
                Spacer(Modifier.width(10.dp))
                Text(
                    "Crear cotización y Cotizaciones se migran en la siguiente etapa; por ahora siguen en la versión web.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5C3A00),
                )
            }
        }
    }
}

@Composable
fun PantallaProximamente(pantalla: Pantalla) = EstadoMensaje(
    icono = Icons.Outlined.Construction,
    titulo = "${pantalla.titulo} llegará pronto",
    detalle = "Este módulo se migra a la app en la siguiente etapa. Mientras tanto, úsalo desde la versión web de VentanaGo.",
)
