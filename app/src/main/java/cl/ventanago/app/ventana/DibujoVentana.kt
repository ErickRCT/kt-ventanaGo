package cl.ventanago.app.ventana

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.min

fun colorDe(rgb: Int, alfa: Float = 1f) = Color((rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF, (alfa * 255).toInt())

/** Color de cada material, con un borde un poco más oscuro para que se distingan los perfiles. */
fun colorMaterial(material: Material, config: ConfigVentana) = when (material) {
    Material.MARCO -> colorDe(config.colorMarco)
    Material.VIDRIO -> colorDe(config.colorVidrio, opacidadVidrio(config.modelo) + 0.25f)
    Material.HERRAJE -> colorDe(COLOR_HERRAJE)
}

/**
 * Dibuja la ventana de frente (equivale al dibujo SVG de la app React Native). [escala] son píxeles por
 * metro; si es null, la ventana se ajusta al espacio disponible.
 */
fun DrawScope.dibujarVentana(config: ConfigVentana, piezas: List<Pieza>, centro: Offset = center, escala: Float? = null) {
    val margen = 0.92f
    val s = escala ?: min(size.width / config.anchoM, size.height / config.altoM) * margen
    fun punto(x: Float, y: Float) = Offset(centro.x + x * s, centro.y - y * s)
    val borde = Color.Black.copy(alpha = 0.25f)

    for (pieza in piezas) {
        val color = colorMaterial(pieza.material, config)
        when (pieza) {
            is Caja -> {
                val esquina = punto(pieza.x - pieza.w / 2, pieza.y + pieza.h / 2)
                val tamano = Size(pieza.w * s, pieza.h * s)
                drawRect(color, esquina, tamano)
                if (pieza.material != Material.VIDRIO) drawRect(borde, esquina, tamano, style = Stroke(1f))
                else {
                    // Reflejo diagonal sobre el vidrio.
                    val a = punto(pieza.x - pieza.w / 2 + pieza.w * 0.15f, pieza.y + pieza.h / 2 - pieza.h * 0.1f)
                    val b = punto(pieza.x - pieza.w / 2 + pieza.w * 0.45f, pieza.y + pieza.h / 2 - pieza.h * 0.4f)
                    drawLine(Color.White.copy(alpha = 0.55f), a, b, strokeWidth = (s * 0.012f).coerceAtLeast(1.5f))
                }
            }
            is Figura -> {
                val path = Path().apply {
                    fillType = PathFillType.EvenOdd
                    (listOf(pieza.contorno) + pieza.huecos).forEach { contorno ->
                        contorno.forEachIndexed { i, (x, y) ->
                            val p = punto(x, y)
                            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                        }
                        close()
                    }
                }
                drawPath(path, color)
                if (pieza.material != Material.VIDRIO) drawPath(path, borde, style = Stroke(1f))
            }
        }
    }
}

/** Vista previa de la ventana con sus colores y proporciones. */
@Composable
fun DibujoVentana(config: ConfigVentana, modifier: Modifier = Modifier) {
    val piezas = remember(config) { piezasVentana(config) }
    Canvas(modifier) { dibujarVentana(config, piezas) }
}
