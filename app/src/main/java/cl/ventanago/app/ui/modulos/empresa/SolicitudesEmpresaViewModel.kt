package cl.ventanago.app.ui.modulos.empresa

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import cl.ventanago.app.datos.EmpresaApi
import cl.ventanago.app.datos.Solicitud
import cl.ventanago.app.datos.mensaje
import cl.ventanago.app.notificaciones.Push
import cl.ventanago.app.ui.comun.ModeloBase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Solicitudes que le llegan al proveedor (o todas, al administrador). Lo comparten la lista y el detalle de una
 * solicitud: al enviar una oferta la lista queda al día sin volver a cargarla.
 */
class SolicitudesEmpresaViewModel : ModeloBase() {
    var solicitudes by mutableStateOf<List<Solicitud>?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var refrescando by mutableStateOf(false)
        private set
    /** Bandeja elegida (proveedor) o estado (administrador); null hasta que la pantalla elige la inicial. */
    var filtro by mutableStateOf<String?>(null)

    fun solicitud(numero: Long) = solicitudes?.firstOrNull { it.numero == numero }

    private suspend fun actualizar(manual: Boolean) {
        try {
            solicitudes = EmpresaApi.solicitudes().sortedByDescending { it.numero }
            error = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (solicitudes == null) error = e.mensaje() else if (manual) avisar(e.mensaje())
        }
    }

    /**
     * Las solicitudes nuevas llegan de los clientes: mientras la lista está a la vista se revisan cada minuto, o
     * apenas llega una notificación.
     */
    suspend fun sondear() {
        while (true) {
            actualizar(manual = false)
            withTimeoutOrNull(60_000) { Push.llegadas.first() }
        }
    }

    fun cargar(manual: Boolean = false) {
        if (manual) refrescando = true
        viewModelScope.launch {
            actualizar(manual)
            refrescando = false
        }
    }

    /** Carga y espera a que termine (para buscar una solicitud que aún no está en la lista). */
    suspend fun recargar() = actualizar(manual = false)

    fun reintentar() {
        error = null
        cargar()
    }

    fun reemplazar(actualizada: Solicitud) {
        solicitudes = solicitudes?.map { if (it.numero == actualizada.numero) actualizada else it }
        avisar("Oferta enviada para la solicitud N° ${actualizada.numero}")
    }
}
