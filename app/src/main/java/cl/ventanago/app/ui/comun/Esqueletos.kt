package cl.ventanago.app.ui.comun

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cl.ventanago.app.ui.Colores
import kotlinx.coroutines.delay

// Esqueletos de carga: la forma de lo que viene, con un brillo que la recorre, en vez de una ruedita. Si la carga
// tarda (el servidor de Render se duerme sin uso), se avisa que la primera respuesta puede demorar.

/** Brillo que recorre el bloque de izquierda a derecha. */
private fun Modifier.brillo(): Modifier = composed {
    val base = Colores.superficieTenue
    val luz = Colores.borde
    val transicion = rememberInfiniteTransition(label = "brillo")
    val x by transicion.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart), label = "x")
    background(
        Brush.linearGradient(
            listOf(base, luz, base),
            start = Offset(-600f + x * 1800f, 0f),
            end = Offset(-200f + x * 1800f, 200f),
        ),
    )
}

/** Un bloque gris con brillo, del tamaño que diga [modifier]. */
@Composable
fun Hueso(modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .brillo(),
    )
}

/**
 * Aviso que aparece si la carga tarda más de [despuesDeMs]: el servidor gratuito se duerme sin uso y la primera
 * respuesta del día puede tardar hasta un minuto.
 */
@Composable
fun AvisoServidorLento(despuesDeMs: Long = 6000) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(despuesDeMs)
        visible = true
    }
    AnimatedVisibility(visible, enter = fadeIn() + expandVertically()) {
        Surface(color = Colores.azulClaro, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Bedtime, contentDescription = null, tint = Colores.azul, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "Despertando el servidor… la primera carga del día puede tardar hasta un minuto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Colores.texto,
                )
            }
        }
    }
}

/** Lista cargando: tarjetas con imagen y dos líneas de texto. [mensaje] es lo que oye quien usa lector de pantalla. */
@Composable
fun EsqueletoLista(mensaje: String, filas: Int = 5, conImagen: Boolean = true) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .semantics { contentDescription = mensaje },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AvisoServidorLento()
        repeat(filas) {
            Surface(color = Colores.superficie, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (conImagen) {
                        Hueso(Modifier.size(52.dp))
                        Spacer(Modifier.width(14.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Hueso(
                            Modifier
                                .fillMaxWidth(0.6f)
                                .height(16.dp),
                        )
                        Hueso(
                            Modifier
                                .fillMaxWidth(0.9f)
                                .height(12.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Catálogo cargando: una cuadrícula de dos columnas con imagen y nombre. */
@Composable
fun EsqueletoCuadricula(mensaje: String, celdas: Int = 6) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .semantics { contentDescription = mensaje },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AvisoServidorLento()
        Hueso(
            Modifier
                .fillMaxWidth()
                .height(52.dp),
        )
        repeat((celdas + 1) / 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) {
                    Surface(color = Colores.superficie, shape = MaterialTheme.shapes.large, modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Hueso(
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                            )
                            Hueso(
                                Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(14.dp),
                            )
                            Hueso(
                                Modifier
                                    .fillMaxWidth(0.5f)
                                    .height(10.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
