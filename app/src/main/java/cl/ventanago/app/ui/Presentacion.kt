package cl.ventanago.app.ui

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import cl.ventanago.app.ventana.ConfigVentana
import cl.ventanago.app.ventana.DibujoVentana
import cl.ventanago.app.ventana.ModeloVentana
import kotlinx.coroutines.launch

/** Si ya se mostró la presentación en este teléfono (se muestra una sola vez, a quien entra sin cuenta). */
object Presentacion {
    private const val CLAVE = "presentacion_vista"

    private fun preferencias(context: Context) = context.getSharedPreferences("ventanago", Context.MODE_PRIVATE)

    fun vista(context: Context) = preferencias(context).getBoolean(CLAVE, false)

    fun marcarVista(context: Context) = preferencias(context).edit { putBoolean(CLAVE, true) }
}

private class Pagina(val titulo: String, val texto: String, val icono: ImageVector?)

private val PAGINAS = listOf(
    Pagina("Diseña tu ventana", "Elige el tipo, las medidas en centímetros, el color y el vidrio. Ves el precio referencial al instante.", null),
    Pagina("Mírala en tu pared", "Con la cámara del teléfono ves la ventana a tamaño real donde va a ir, antes de pedirla.", Icons.Outlined.ViewInAr),
    Pagina("Recibe ofertas y elige", "Los proveedores de tu comuna te envían sus ofertas. Compara precio, plazo y valoraciones, conversa con ellos y elige.", Icons.Outlined.Handshake),
)

/** Presentación de la app en tres pasos deslizables; [alTerminar] al tocar "Empezar" o "Saltar". */
@Composable
fun PantallaPresentacion(alTerminar: () -> Unit) {
    val paginas = rememberPagerState { PAGINAS.size }
    val alcance = rememberCoroutineScope()
    val ultima = paginas.currentPage == PAGINAS.lastIndex
    Column(
        Modifier
            .fillMaxSize()
            .background(Colores.fondo)
            .systemBarsPadding()
            .padding(24.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("VentanaGo", style = MaterialTheme.typography.titleLarge, color = Colores.textoFuerte, modifier = Modifier.weight(1f))
            if (!ultima) TextButton(onClick = alTerminar) { Text("Saltar") }
        }
        HorizontalPager(paginas, modifier = Modifier.weight(1f)) { i ->
            val p = PAGINAS[i]
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier
                        .size(220.dp)
                        .background(Colores.azulClaro, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (p.icono != null) {
                        Icon(p.icono, contentDescription = null, tint = Colores.azul, modifier = Modifier.size(96.dp))
                    } else {
                        // El mismo dibujo que se usa al diseñar: una corredera de dos hojas.
                        DibujoVentana(
                            ConfigVentana(anchoMm = 1200, altoMm = 1000, hojas = 2, modelo = ModeloVentana.CORREDERA, colorMarco = 0xF4F4F4, colorVidrio = 0xBFE6FF),
                            Modifier.size(140.dp),
                        )
                    }
                }
                Spacer(Modifier.height(40.dp))
                Text(p.titulo, style = MaterialTheme.typography.headlineMedium, color = Colores.textoFuerte, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(p.texto, style = MaterialTheme.typography.bodyLarge, color = Colores.textoSecundario, textAlign = TextAlign.Center)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Puntos(paginas.currentPage, PAGINAS.size, Modifier.weight(1f))
            Button(
                onClick = { if (ultima) alTerminar() else alcance.launch { paginas.animateScrollToPage(paginas.currentPage + 1) } },
                modifier = Modifier.height(52.dp),
            ) { Text(if (ultima) "Empezar a diseñar" else "Siguiente") }
        }
    }
}

/** Indicador de página: el punto actual se alarga. */
@Composable
private fun Puntos(actual: Int, total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.semantics { contentDescription = "Paso ${actual + 1} de $total" },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(total) { i ->
            val ancho by animateDpAsState(if (i == actual) 24.dp else 8.dp, label = "ancho")
            val color by animateColorAsState(if (i == actual) Colores.azul else Colores.borde, label = "color")
            Box(
                Modifier
                    .width(ancho)
                    .height(8.dp)
                    .background(color, RoundedCornerShape(4.dp)),
            )
        }
    }
}
