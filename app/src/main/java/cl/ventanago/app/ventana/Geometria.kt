package cl.ventanago.app.ventana

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/*
 * Geometría de la ventana en metros, con el centro en el origen y el frente hacia +Z. Es la misma de la
 * web (ventana3d.ts) y de la app React Native (geometria.ts); la usan el dibujo 2D y la realidad aumentada.
 */

/** Formas de ventana que se saben dibujar, según las imágenes de las pautas. */
enum class ModeloVentana { CORREDERA, MONORRIEL, FIJA, ARCO, BATIENTE, GRANERO, MAMPARA }

enum class Material { MARCO, VIDRIO, HERRAJE }

/** Lo que define una ventana: medidas en mm (como en las cotizaciones) y colores 0xRRGGBB. */
data class ConfigVentana(
    val anchoMm: Int,
    val altoMm: Int,
    /** Hojas de la corredera: 1 es fija, desde 2 son corredizas. */
    val hojas: Int,
    val modelo: ModeloVentana,
    val colorMarco: Int,
    val colorVidrio: Int,
) {
    val anchoM get() = anchoMm / 1000f
    val altoM get() = altoMm / 1000f
}

sealed interface Pieza {
    val material: Material
}

/** Caja (perfil, paño de vidrio o herraje) con su centro en (x, y, z). */
data class Caja(override val material: Material, val w: Float, val h: Float, val d: Float, val x: Float, val y: Float, val z: Float) : Pieza

/** Figura plana (el arco), con huecos opcionales, a la profundidad z. */
data class Figura(override val material: Material, val contorno: List<Pair<Float, Float>>, val huecos: List<List<Pair<Float, Float>>>, val z: Float) : Pieza

const val COLOR_HERRAJE = 0xC9CCD1

private const val PROFUNDIDAD = 0.07f
private const val GROSOR_MARCO = 0.05f
private const val GROSOR_HOJA = 0.04f
private const val PROFUNDIDAD_HOJA = 0.03f
private const val GROSOR_TRAVESANO = 0.03f
private const val GROSOR_VIDRIO = 0.008f
/** Lo que se montan las hojas de una corredera una sobre otra. */
private const val SOLAPE = 0.03f
/** Separación entre rieles de la corredera. */
private const val PASO_RIEL = 0.016f

private class Constructor {
    val piezas = mutableListOf<Pieza>()

    fun caja(m: Material, w: Float, h: Float, d: Float, x: Float, y: Float, z: Float = 0f) {
        if (w > 0 && h > 0 && d > 0) piezas += Caja(m, w, h, d, x, y, z)
    }

    /** Rectángulo de perfiles (marco u hoja) centrado en (x, y). */
    fun rectangulo(w: Float, h: Float, grosor: Float, d: Float, x: Float, y: Float, z: Float = 0f) {
        caja(Material.MARCO, w, grosor, d, x, y + (h - grosor) / 2, z)
        caja(Material.MARCO, w, grosor, d, x, y - (h - grosor) / 2, z)
        caja(Material.MARCO, grosor, h - 2 * grosor, d, x - (w - grosor) / 2, y, z)
        caja(Material.MARCO, grosor, h - 2 * grosor, d, x + (w - grosor) / 2, y, z)
    }

    fun hoja(w: Float, h: Float, x: Float, y: Float, z: Float) {
        caja(Material.VIDRIO, w - 2 * GROSOR_HOJA, h - 2 * GROSOR_HOJA, GROSOR_VIDRIO, x, y, z)
        rectangulo(w, h, GROSOR_HOJA, PROFUNDIDAD_HOJA, x, y, z)
    }

    fun tirador(x: Float, z: Float) = caja(Material.HERRAJE, 0.018f, 0.12f, 0.02f, x, 0f, z + PROFUNDIDAD_HOJA / 2 + 0.01f)
}

private fun Constructor.corredera(ancho: Float, alto: Float, n: Int) {
    val anchoInterior = ancho - 2 * GROSOR_MARCO
    val altoInterior = alto - 2 * GROSOR_MARCO
    if (n <= 1) {
        caja(Material.VIDRIO, anchoInterior, altoInterior, GROSOR_VIDRIO, 0f, 0f)
        rectangulo(ancho, alto, GROSOR_MARCO, PROFUNDIDAD, 0f, 0f)
        return
    }
    rectangulo(ancho, alto, GROSOR_MARCO, PROFUNDIDAD, 0f, 0f)
    val rieles = if (n >= 6) 3 else 2
    val anchoHoja = anchoInterior / n + SOLAPE
    val paso = (anchoInterior - anchoHoja) / (n - 1)
    // Con un número par de hojas (desde 4) abren desde el centro: las centrales van en el riel de adelante.
    val simetrica = n >= 4 && n % 2 == 0
    for (i in 0 until n) {
        val riel = if (simetrica) min(min(i, n - 1 - i), rieles - 1) else i % rieles
        val z = (riel - (rieles - 1) / 2f) * PASO_RIEL
        val x = -anchoInterior / 2 + anchoHoja / 2 + i * paso
        hoja(anchoHoja, altoInterior, x, 0f, z)
        if (i == floor((n - 1) / 2.0).toInt()) tirador(x + anchoHoja / 2 - GROSOR_HOJA / 2, z)
        if (i == ceil((n - 1) / 2.0).toInt() && n % 2 == 0) tirador(x - anchoHoja / 2 + GROSOR_HOJA / 2, z)
    }
}

private fun Constructor.monorriel(ancho: Float, alto: Float) {
    val anchoInterior = ancho - 2 * GROSOR_MARCO
    val altoInterior = alto - 2 * GROSOR_MARCO
    val mitad = anchoInterior / 2
    caja(Material.VIDRIO, mitad - GROSOR_TRAVESANO / 2, altoInterior, GROSOR_VIDRIO, -mitad / 2 - GROSOR_TRAVESANO / 4, 0f, -PASO_RIEL / 2)
    rectangulo(ancho, alto, GROSOR_MARCO, PROFUNDIDAD, 0f, 0f)
    caja(Material.MARCO, GROSOR_TRAVESANO, altoInterior, PROFUNDIDAD, 0f, 0f)
    val xHoja = mitad / 2 + GROSOR_TRAVESANO / 4
    val anchoHoja = mitad - GROSOR_TRAVESANO / 2
    hoja(anchoHoja, altoInterior, xHoja, 0f, PASO_RIEL / 2)
    tirador(xHoja + anchoHoja / 2 - GROSOR_HOJA / 2, PASO_RIEL / 2)
}

/** Contorno con arco arriba: tramo recto y medio óvalo que ocupa todo el ancho. */
fun contornoArco(w: Float, h: Float, altoArco: Float, segmentos: Int = 32): List<Pair<Float, Float>> {
    val base = -h / 2
    val inicioArco = h / 2 - altoArco
    val puntos = mutableListOf(-w / 2 to base, w / 2 to base, w / 2 to inicioArco)
    for (i in 1 until segmentos) {
        val angulo = PI * i / segmentos
        puntos += (w / 2 * cos(angulo)).toFloat() to (inicioArco + altoArco * sin(angulo)).toFloat()
    }
    puntos += -w / 2 to inicioArco
    return puntos
}

/** Alto del arco: semicircular salvo que la ventana sea tan baja que no quepa. */
fun altoArco(ancho: Float, alto: Float) = min(ancho / 2, alto * 0.6f)

private fun Constructor.arco(ancho: Float, alto: Float) {
    val altoArco = altoArco(ancho, alto)
    val interior = contornoArco(ancho - 2 * GROSOR_MARCO, alto - 2 * GROSOR_MARCO, max(0.01f, altoArco - GROSOR_MARCO))
    piezas += Figura(Material.VIDRIO, interior, emptyList(), 0f)
    for (z in listOf(PROFUNDIDAD / 2, -PROFUNDIDAD / 2)) {
        piezas += Figura(Material.MARCO, contornoArco(ancho, alto, altoArco), listOf(interior), z)
    }
}

private fun Constructor.batiente(ancho: Float, alto: Float) {
    rectangulo(ancho, alto, GROSOR_MARCO, PROFUNDIDAD, 0f, 0f)
    val anchoInterior = ancho - 2 * GROSOR_MARCO
    val altoInterior = alto - 2 * GROSOR_MARCO
    hoja(anchoInterior, altoInterior, 0f, 0f, 0.01f)
    val zFrente = 0.01f + PROFUNDIDAD_HOJA / 2
    // En puertas la manilla va a ~1 m del piso; en ventanas bajas, al medio.
    val yManilla = if (alto > 1.6f) -alto / 2 + 1.0f else 0f
    val xManilla = -anchoInterior / 2 + GROSOR_HOJA / 2
    caja(Material.HERRAJE, 0.03f, 0.14f, 0.012f, xManilla, yManilla - 0.03f, zFrente + 0.006f)
    caja(Material.HERRAJE, 0.13f, 0.02f, 0.02f, xManilla + 0.065f, yManilla, zFrente + 0.035f)
    caja(Material.HERRAJE, 0.016f, 0.02f, 0.035f, xManilla, yManilla, zFrente + 0.018f)
    for (y in listOf(altoInterior / 2 - 0.12f, -altoInterior / 2 + 0.12f)) {
        caja(Material.HERRAJE, 0.018f, 0.1f, 0.03f, anchoInterior / 2 + 0.004f, y, zFrente)
    }
}

private fun Constructor.granero(ancho: Float, alto: Float) {
    val altoRiel = 0.06f
    val altoVidrio = alto - altoRiel - 0.05f
    val yVidrio = -alto / 2 + altoVidrio / 2
    val zVidrio = 0.03f
    val yRiel = alto / 2 - altoRiel / 2
    val hueco = 0.01f
    val anchoHoja = (ancho - hueco) / 2
    for (lado in listOf(-1, 1)) caja(Material.VIDRIO, anchoHoja, altoVidrio, 0.01f, lado * (anchoHoja + hueco) / 2, yVidrio, zVidrio)
    caja(Material.HERRAJE, ancho + 0.1f, 0.025f, 0.025f, 0f, yRiel, zVidrio + 0.03f)
    for (x in listOf(-(ancho + 0.1f) / 2 + 0.05f, 0f, (ancho + 0.1f) / 2 - 0.05f)) caja(Material.HERRAJE, 0.02f, 0.02f, 0.05f, x, yRiel, zVidrio + 0.005f)
    for (lado in listOf(-1, 1)) {
        val x = lado * (anchoHoja + hueco) / 2
        for (dx in listOf(-anchoHoja / 2 + 0.1f, anchoHoja / 2 - 0.1f)) {
            caja(Material.HERRAJE, 0.07f, 0.07f, 0.015f, x + dx, yRiel + 0.02f, zVidrio + 0.03f)
            caja(Material.HERRAJE, 0.03f, yRiel - (yVidrio + altoVidrio / 2) + 0.06f, 0.006f, x + dx, (yRiel + yVidrio + altoVidrio / 2) / 2, zVidrio + 0.012f)
        }
        val largo = min(1.2f, altoVidrio * 0.6f)
        val xTirador = x - lado * (anchoHoja / 2 - 0.08f)
        caja(Material.HERRAJE, 0.024f, largo, 0.024f, xTirador, yVidrio, zVidrio + 0.05f)
        for (dy in listOf(-largo / 2 + 0.05f, largo / 2 - 0.05f)) caja(Material.HERRAJE, 0.012f, 0.012f, 0.045f, xTirador, yVidrio + dy, zVidrio + 0.027f)
    }
}

private fun Constructor.mampara(ancho: Float, alto: Float) {
    val hueco = 0.008f
    val proporciones = listOf(0.35f, 0.34f, 0.31f)
    val util = ancho - hueco * (proporciones.size - 1)
    var x = -ancho / 2
    val vidrios = proporciones.map { p ->
        val w = util * p
        val centro = x + w / 2
        x += w + hueco
        caja(Material.VIDRIO, w, alto, 0.01f, centro, 0f, 0f)
        centro to w
    }
    val (fijoIzq, puerta, fijoDer) = vidrios
    fun clip(cx: Float, cy: Float) = caja(Material.HERRAJE, 0.035f, 0.035f, 0.025f, cx, cy, 0f)
    clip(fijoIzq.first - fijoIzq.second / 2 + 0.018f, alto / 2 - 0.2f)
    clip(fijoIzq.first - fijoIzq.second / 2 + 0.018f, -alto / 2 + 0.2f)
    for (fijo in listOf(fijoIzq, fijoDer)) {
        clip(fijo.first - fijo.second / 4, -alto / 2 + 0.018f)
        clip(fijo.first + fijo.second / 4, -alto / 2 + 0.018f)
    }
    clip(fijoDer.first + fijoDer.second / 2 - 0.018f, alto / 2 - 0.2f)
    clip(fijoDer.first + fijoDer.second / 2 - 0.018f, -alto / 2 + 0.2f)
    val xBisagra = puerta.first + puerta.second / 2
    for (y in listOf(alto / 2 - 0.25f, -alto / 2 + 0.25f)) caja(Material.HERRAJE, 0.05f, 0.08f, 0.03f, xBisagra, y, 0f)
    val yManilla = if (alto > 1.6f) -alto / 2 + 1.05f else 0f
    val xManilla = puerta.first - puerta.second / 2 + 0.08f
    val largo = min(0.3f, puerta.second * 0.6f)
    caja(Material.HERRAJE, 0.018f, 0.12f, 0.018f, xManilla, yManilla + 0.06f, 0.04f)
    caja(Material.HERRAJE, largo, 0.018f, 0.018f, xManilla + largo / 2, yManilla, 0.04f)
    caja(Material.HERRAJE, 0.012f, 0.012f, 0.035f, xManilla, yManilla, 0.02f)
    caja(Material.HERRAJE, 0.012f, 0.012f, 0.035f, xManilla + largo, yManilla, 0.02f)
}

/** Piezas de la ventana, ordenadas de atrás hacia adelante (para dibujarla de frente y para la transparencia). */
fun piezasVentana(config: ConfigVentana): List<Pieza> {
    val c = Constructor()
    val ancho = config.anchoM
    val alto = config.altoM
    when (config.modelo) {
        ModeloVentana.MONORRIEL -> c.monorriel(ancho, alto)
        ModeloVentana.FIJA -> c.corredera(ancho, alto, 1)
        ModeloVentana.ARCO -> c.arco(ancho, alto)
        ModeloVentana.BATIENTE -> c.batiente(ancho, alto)
        ModeloVentana.GRANERO -> c.granero(ancho, alto)
        ModeloVentana.MAMPARA -> c.mampara(ancho, alto)
        ModeloVentana.CORREDERA -> c.corredera(ancho, alto, config.hojas)
    }
    fun frente(p: Pieza) = when (p) {
        is Caja -> p.z + p.d / 2
        is Figura -> p.z
    }
    return c.piezas.withIndex().sortedWith(compareBy({ frente(it.value) }, { it.index })).map { it.value }
}

/** Vidrios sin marco (granero y mampara) se ven más opacos, como en las imágenes de las pautas. */
fun opacidadVidrio(modelo: ModeloVentana) = if (modelo == ModeloVentana.GRANERO || modelo == ModeloVentana.MAMPARA) 0.45f else 0.3f
