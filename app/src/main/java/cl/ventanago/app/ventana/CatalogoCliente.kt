package cl.ventanago.app.ventana

import cl.ventanago.app.datos.Pauta
import java.net.URLDecoder

/*
 * Reglas del catálogo para el cliente, iguales a las de la web (src/modules/cliente/catalogo.ts):
 * forma y hojas de cada pauta (no vienen como dato), límites de medidas y colores para el dibujo.
 */

private val MODELOS = listOf(
    Regex("granero") to ModeloVentana.GRANERO,
    Regex("ba[ñn]o|ducha|shower|mampara") to ModeloVentana.MAMPARA,
    Regex("batiente|abatible|puerta") to ModeloVentana.BATIENTE,
    Regex("arcada|arco") to ModeloVentana.ARCO,
    Regex("mono\\s*r+iel") to ModeloVentana.MONORRIEL,
    Regex("corredera") to ModeloVentana.CORREDERA,
    Regex("fij[ao]") to ModeloVentana.FIJA,
)

private fun modeloDeTexto(texto: String) = MODELOS.firstOrNull { it.first.containsMatchIn(texto.lowercase()) }?.second

private fun decodificar(texto: String) = try {
    URLDecoder.decode(texto, "UTF-8")
} catch (_: Exception) {
    texto
}

/** El dibujo debe parecerse a la imagen de la pauta: primero el nombre del archivo, luego el tipo y la pauta. */
fun modeloDePauta(pauta: Pauta): ModeloVentana =
    modeloDeTexto(decodificar(pauta.tipoPauta?.rutaImagen.orEmpty()))
        ?: modeloDeTexto(pauta.tipoPauta?.nombre.orEmpty())
        ?: modeloDeTexto(pauta.nombre)
        ?: ModeloVentana.CORREDERA

private val HOJAS_EN_LETRAS = mapOf("una" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5, "seis" to 6, "siete" to 7, "ocho" to 8)

/** "Corredera 6 hojas" -> 6, "cuatro hojas" -> 4, "Ventana fija" -> 1, y 2 si no dice nada. */
fun hojasDePauta(pauta: Pauta): Int {
    val texto = "${pauta.nombre} ${pauta.tipoPauta?.nombre.orEmpty()} ${pauta.tipoPauta?.rutaImagen.orEmpty()}".lowercase()
    Regex("(\\d+|una|dos|tres|cuatro|cinco|seis|siete|ocho)\\s*hojas?").find(texto)?.let {
        val cantidad = HOJAS_EN_LETRAS[it.groupValues[1]] ?: it.groupValues[1].toIntOrNull() ?: 2
        return cantidad.coerceIn(1, 12)
    }
    return if (Regex("\\bfij[ao]s?\\b").containsMatchIn(texto)) 1 else 2
}

const val MIN_MM = 300
const val MAX_CANTIDAD = 99

data class Limites(val maxAnchoMm: Int, val maxAltoMm: Int)

/** Medidas máximas razonables de fabricación según la forma (cada hoja de corredera cubre hasta ~1,5 m). */
fun limitesDeModelo(modelo: ModeloVentana, hojas: Int): Limites = when (modelo) {
    ModeloVentana.CORREDERA -> if (hojas <= 1) Limites(3000, 3000) else Limites(minOf(1500 * hojas, 9000), if (hojas >= 3) 2600 else 2400)
    ModeloVentana.MONORRIEL -> Limites(3000, 2400)
    ModeloVentana.FIJA -> Limites(3000, 3000)
    ModeloVentana.ARCO -> Limites(2400, 3000)
    ModeloVentana.BATIENTE -> Limites(1200, 2700)
    ModeloVentana.GRANERO -> Limites(4000, 3000)
    ModeloVentana.MAMPARA -> Limites(3000, 2600)
}

private fun pesoDeOrientacion(pauta: Pauta, orientacion: String, cargado: Double?): Double {
    if (cargado != null && cargado > 0) return cargado
    return pauta.perfiles.orEmpty().filter { it.orientacion.equals(orientacion, true) }
        .sumOf { (it.perfil?.peso?.replace(',', '.')?.toDoubleOrNull() ?: 0.0) * it.cantidad }
}

/**
 * Datos de la pauta que harían que el precio del backend no sea real. Si hay alguno, al cliente no se le
 * muestra precio (se corrige en el mantenedor de pautas).
 */
fun problemasDePrecio(pauta: Pauta): List<String> = buildList {
    if (pesoDeOrientacion(pauta, "H", pauta.pesoTeoricoHorizontal) <= 0) add("sin perfiles horizontales")
    if (pesoDeOrientacion(pauta, "V", pauta.pesoTeoricoVertical) <= 0) add("sin perfiles verticales")
    pauta.quincallerias.orEmpty().forEach { q ->
        if (q.quincalleria?.unidad.equals("mt", true) && (q.variacionH > 20 || q.variacionV > 20)) add("variación de metros fuera de rango")
    }
    if (pauta.vidrios.isNullOrEmpty()) add("sin vidrios")
    if (pauta.vidrios.orEmpty().any { it.variacionH > 300 || it.variacionV > 300 }) add("descuento de vidrio fuera de rango")
}

// El backend solo entrega el nombre del color, así que el tono del dibujo se deduce de él.
private val COLORES_MARCO = mapOf("blanco" to 0xF4F4F4, "negro" to 0x1A1A1A, "titanio" to 0xA8A8A8, "madera" to 0x8B5A2B, "mate" to 0x4A4A4A)

fun colorMarco(nombre: String?): Int = COLORES_MARCO[nombre.orEmpty().trim().lowercase()] ?: 0xB8BCC7

private val TINTES_VIDRIO = listOf("bronce" to 0xB08A5A, "gris" to 0x8A929A, "azul" to 0x8FBFE8, "verde" to 0x9FD6B5)

fun colorVidrio(nombre: String?): Int = TINTES_VIDRIO.firstOrNull { nombre.orEmpty().lowercase().contains(it.first) }?.second ?: 0xBFE6FF

/** Forma de una ventana del carrito o de una solicitud (guardan la imagen y el nombre de la pauta). */
fun modeloDeTextos(imagen: String?, descripcion: String?): ModeloVentana =
    modeloDeTexto(decodificar(imagen.orEmpty())) ?: modeloDeTexto(descripcion.orEmpty()) ?: ModeloVentana.CORREDERA
