package cl.ventanago.app.datos

import kotlin.math.roundToLong

/**
 * Precio de venta, igual que CalculoCotizacion del backend (y el PDF): a cada ventana se le aplica
 * la ganancia y luego el descuento; se suman los extras y sobre ese neto se calcula el IVA.
 */
data class ResumenCotizacion(val costo: Long, val ventanas: Long, val extras: Long, val neto: Long, val iva: Long, val total: Long)

fun factorVenta(ganancia: Int?, descuento: Int?) = (1 + (ganancia ?: 0) / 100.0) * (1 - (descuento ?: 0) / 100.0)

fun precioVentaUnitario(ventana: Ventana, factor: Double): Long = (ventana.precioNeto * factor).roundToLong()

fun Cotizacion.resumen(): ResumenCotizacion {
    val factor = factorVenta(ganancia, descuento)
    val lista = ventanas.orEmpty()
    val costo = lista.sumOf { it.precioNeto * (it.cantidad ?: 1) }
    val totalVentanas = lista.sumOf { precioVentaUnitario(it, factor) * (it.cantidad ?: 1) }
    val extras = listOf(valorFlete, valorInstalacion, valorManoDeObra, valorOtrosGastos).sumOf { (it ?: 0).toLong() }
    val neto = totalVentanas + extras
    val iva = (neto * 0.19).roundToLong()
    return ResumenCotizacion(costo, totalVentanas, extras, neto, iva, neto + iva)
}

/** Estados que entiende la web (los chips comparan "Aprobada" y "Rechazada"). */
val estadosCotizacion = listOf("CREADA" to "Creada", "Aprobada" to "Aprobada", "Rechazada" to "Rechazada")

fun nombreEstado(estado: String?) = estadosCotizacion.firstOrNull { it.first.equals(estado, ignoreCase = true) }?.second ?: (estado ?: "Creada")

object Cotizaciones {
    suspend fun listar(): List<Cotizacion> = Api.lista("/cotizacion", Cotizacion.serializer())
    suspend fun obtener(id: Long): Cotizacion = Api.get("/cotizacion/$id", Cotizacion.serializer())

    /** Crea la cotización y devuelve la guardada (con su id). */
    suspend fun agregar(c: Cotizacion): Cotizacion =
        Api.json.decodeFromString(Cotizacion.serializer(), Api.enviar("POST", "/cotizacion/agregar", c, Cotizacion.serializer()))

    suspend fun modificar(c: Cotizacion) {
        // Las ventanas se guardan aparte (/ventana); no se reenvían con la cotización.
        Api.enviar("PUT", "/cotizacion/modificar", c.copy(ventanas = null), Cotizacion.serializer())
    }

    /** Calcula el costo de una ventana sin guardarla. */
    suspend fun cotizarVentana(v: Ventana): Ventana =
        Api.json.decodeFromString(Ventana.serializer(), Api.enviar("POST", "/ventana/cotizar", v, Ventana.serializer()))

    suspend fun agregarVentana(v: Ventana) {
        Api.enviar("POST", "/ventana/agregar", v, Ventana.serializer())
    }

    suspend fun eliminarVentana(id: Long) = Api.eliminar("/ventana/eliminar/$id")

    suspend fun pautasDeSerie(serieId: Long): List<Pauta> = Api.lista("/pauta/serie/$serieId", Pauta.serializer())
}

/**
 * Ganancia con la que se muestra al cliente un precio referencial mientras diseña (la misma con la que parte
 * una cotización nueva). El precio final lo confirma la empresa al responder la solicitud.
 */
const val GANANCIA_REFERENCIAL = 50
