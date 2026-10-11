package cl.ventanago.app.ui.modulos.empresa

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import cl.ventanago.app.datos.Cotizaciones
import cl.ventanago.app.datos.EmpresaApi
import cl.ventanago.app.datos.GANANCIA_REFERENCIAL
import cl.ventanago.app.datos.ItemVentana
import cl.ventanago.app.datos.Oferta
import cl.ventanago.app.datos.Recursos
import cl.ventanago.app.datos.RespuestaSolicitud
import cl.ventanago.app.datos.Solicitud
import cl.ventanago.app.datos.Ventana
import cl.ventanago.app.datos.factorVenta
import cl.ventanago.app.datos.mensaje
import cl.ventanago.app.ui.comun.ModeloBase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

// Mismos límites generales que el backend (SolicitudService) y la web.
private const val MIN_MM = 300
private const val MAX_ANCHO_MM = 9000
private const val MAX_ALTO_MM = 3000
private const val MAX_CANTIDAD = 99
private const val MAX_PRECIO = 100_000_000L
private const val MAX_PLAZO_DIAS = 365

/** Ventana en edición: los números como texto para poder borrar y reescribir el campo. */
class BorradorVentana(val original: ItemVentana) {
    var ancho by mutableStateOf(original.anchoMm.toString())
    var alto by mutableStateOf(original.altoMm.toString())
    var cantidad by mutableStateOf(original.cantidad.toString())
    var precio by mutableStateOf(original.precioUnitario?.toString().orEmpty())
}

/**
 * Oferta del proveedor para la solicitud [s]: acepta lo pedido con precio, propone cambios (medidas, cantidades
 * y precios) o no toma el trabajo.
 */
class OfertaViewModel(private val s: Solicitud) : ModeloBase() {
    val sinVentanas = s.items.isEmpty()

    /** ACEPTADA, MODIFICADA o RECHAZADA. */
    var accion by mutableStateOf("ACEPTADA")
        private set
    val borradores = s.items.map { BorradorVentana(it) }
    var totalServicio by mutableStateOf("")
    var plazo by mutableStateOf("")
    var mensaje by mutableStateOf("")
    var error by mutableStateOf<String?>(null)
        private set
    var enviando by mutableStateOf(false)
        private set
    var sugiriendo by mutableStateOf(false)
        private set
    var chat by mutableStateOf<Oferta?>(null)

    fun elegirAccion(nueva: String) {
        accion = nueva
        error = null
    }

    /** Total en vivo con lo que hay escrito. */
    val total: Long?
        get() = when {
            accion == "RECHAZADA" -> null
            sinVentanas -> totalServicio.toLongOrNull()
            else -> borradores.takeIf { lista -> lista.all { it.precio.toLongOrNull() != null } }
                ?.sumOf { (it.precio.toLongOrNull() ?: 0) * (it.cantidad.toIntOrNull() ?: 0) }
        }

    /** Precio sugerido = costo que calcula el backend (como en Crear cotización) con la ganancia de partida. */
    fun sugerirPrecios() {
        viewModelScope.launch {
            sugiriendo = true
            error = null
            try {
                val colores = Recursos.colores.listar()
                val vidrios = Recursos.vidrios.listar()
                val sinDatos = coroutineScope {
                    borradores.map { b ->
                        async {
                            val i = b.original
                            val color = colores.firstOrNull { it.colorId == i.colorId }
                            val vidrio = vidrios.firstOrNull { it.vidrioId == i.vidrioId }
                            val ancho = b.ancho.toIntOrNull()
                            val alto = b.alto.toIntOrNull()
                            if (i.pautaId == null || color == null || vidrio == null || ancho == null || alto == null) return@async true
                            val costo = Cotizaciones.cotizarVentana(
                                Ventana(cantidad = 1, ancho = ancho, alto = alto, color = color, vidrio = vidrio, pauta = EmpresaApi.pauta(i.pautaId)),
                            ).precioNeto
                            b.precio = (costo * factorVenta(GANANCIA_REFERENCIAL, 0)).roundToLong().toString()
                            false
                        }
                    }.awaitAll().count { it }
                }
                if (sinDatos > 0) error = "$sinDatos ventana(s) no tienen pauta, color o vidrio del catálogo: ingresa su precio a mano."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.mensaje()
            }
            sugiriendo = false
        }
    }

    private fun construir(): List<ItemVentana>? {
        val items = mutableListOf<ItemVentana>()
        for (b in borradores) {
            val ancho = b.ancho.toIntOrNull()?.takeIf { it in MIN_MM..MAX_ANCHO_MM }
                ?: run { error = "El ancho debe estar entre $MIN_MM y $MAX_ANCHO_MM mm."; return null }
            val alto = b.alto.toIntOrNull()?.takeIf { it in MIN_MM..MAX_ALTO_MM }
                ?: run { error = "El alto debe estar entre $MIN_MM y $MAX_ALTO_MM mm."; return null }
            val cantidad = b.cantidad.toIntOrNull()?.takeIf { it in 1..MAX_CANTIDAD }
                ?: run { error = "La cantidad debe estar entre 1 y $MAX_CANTIDAD."; return null }
            val precio = b.precio.toLongOrNull()?.takeIf { it in 1..MAX_PRECIO }
                ?: run { error = "Ingresa el precio unitario de todas las ventanas."; return null }
            items += b.original.copy(anchoMm = ancho, altoMm = alto, cantidad = cantidad, precioUnitario = precio)
        }
        return items
    }

    /** Valida y envía la oferta; [alEnviar] recibe la solicitud actualizada. */
    fun enviar(alEnviar: (Solicitud) -> Unit) {
        error = null
        var items = s.items
        var total: Long? = null
        val plazoDias = plazo.toIntOrNull()
        if (accion != "RECHAZADA") {
            if (sinVentanas) {
                total = totalServicio.toLongOrNull()?.takeIf { it in 1..MAX_PRECIO }
                    ?: run { error = "Ingresa el precio total del trabajo."; return }
            } else {
                items = construir() ?: return
                val cambio = items.zip(s.items).any { (n, o) -> n.anchoMm != o.anchoMm || n.altoMm != o.altoMm || n.cantidad != o.cantidad }
                if (accion == "MODIFICADA" && !cambio) {
                    error = "No cambiaste medidas ni cantidades. Si estás de acuerdo con lo pedido, usa Aceptar."
                    return
                }
                if (accion == "ACEPTADA" && cambio) {
                    error = "Cambiaste medidas o cantidades: usa Modificar para informarlo al cliente."
                    return
                }
            }
            if (plazo.isNotBlank() && (plazoDias == null || plazoDias !in 1..MAX_PLAZO_DIAS)) {
                error = "El plazo debe estar entre 1 y $MAX_PLAZO_DIAS días."
                return
            }
        }
        if (accion != "ACEPTADA" && mensaje.isBlank()) {
            error = if (accion == "RECHAZADA") "Indica el motivo por el que no tomas el trabajo." else "Explica al cliente qué modificaste."
            return
        }
        viewModelScope.launch {
            enviando = true
            try {
                alEnviar(
                    EmpresaApi.responder(
                        s.numero,
                        RespuestaSolicitud(accion, mensaje.trim(), items, notificarEnApp = true, notificarPorCorreo = false, total = total, plazoDias = plazoDias),
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.mensaje()
            }
            enviando = false
        }
    }
}
