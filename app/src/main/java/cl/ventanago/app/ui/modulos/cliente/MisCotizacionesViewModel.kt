package cl.ventanago.app.ui.modulos.cliente

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import cl.ventanago.app.datos.ClienteApi
import cl.ventanago.app.datos.Oferta
import cl.ventanago.app.datos.Solicitud
import cl.ventanago.app.datos.mensaje
import cl.ventanago.app.notificaciones.Push
import cl.ventanago.app.ui.comun.ModeloBase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Solicitudes del cliente con sus ofertas, y las acciones sobre ellas (elegir, cancelar, valorar). */
class MisCotizacionesViewModel : ModeloBase() {
    var solicitudes by mutableStateOf<List<Solicitud>?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var refrescando by mutableStateOf(false)
        private set
    var trabajando by mutableStateOf(false)
        private set

    // Diálogos abiertos (compartidos por todas las tarjetas).
    var chat by mutableStateOf<Pair<Oferta, Boolean>?>(null)
    var proveedor by mutableStateOf<Long?>(null)
    var porElegir by mutableStateOf<Pair<Solicitud, Oferta>?>(null)
    var porCancelar by mutableStateOf<Solicitud?>(null)
    var porValorar by mutableStateOf<Solicitud?>(null)

    private var carga: Job? = null

    init {
        cargar()
        // Una oferta o mensaje nuevo (notificación con la app abierta) se ve sin tener que deslizar.
        viewModelScope.launch { Push.llegadas.collect { cargar() } }
    }

    fun cargar(manual: Boolean = false) {
        if (manual) refrescando = true
        carga?.cancel()
        carga = viewModelScope.launch {
            try {
                solicitudes = ClienteApi.solicitudes().sortedByDescending { it.numero }
                error = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (solicitudes == null) error = e.mensaje() else if (manual) avisar(e.mensaje())
            } finally {
                refrescando = false
            }
        }
    }

    fun reintentar() {
        error = null
        cargar()
    }

    private fun ejecutar(exito: String, bloque: suspend () -> Solicitud) {
        if (trabajando) return
        viewModelScope.launch {
            trabajando = true
            try {
                val actualizada = bloque()
                solicitudes = solicitudes?.map { if (it.numero == actualizada.numero) actualizada else it }
                avisar(exito)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                avisar(e.mensaje())
            }
            trabajando = false
            porElegir = null
            porCancelar = null
            porValorar = null
        }
    }

    fun elegir(s: Solicitud, o: Oferta) = ejecutar("Elegiste a ${o.proveedor?.nombre.orEmpty()}") { ClienteApi.elegirOferta(s.numero, o.id) }

    fun cancelar(s: Solicitud) = ejecutar("Solicitud N° ${s.numero} cancelada") { ClienteApi.cancelar(s.numero) }

    fun valorar(s: Solicitud, estrellas: Int, comentario: String) =
        ejecutar("¡Gracias por tu valoración!") { ClienteApi.valorar(s.numero, estrellas, comentario) }

    fun cerrarChat() {
        chat = null
        cargar()
    }
}
