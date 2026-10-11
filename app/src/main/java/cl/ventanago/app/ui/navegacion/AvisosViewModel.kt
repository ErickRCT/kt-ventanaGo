package cl.ventanago.app.ui.navegacion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import cl.ventanago.app.datos.Aviso
import cl.ventanago.app.datos.ClienteApi
import cl.ventanago.app.notificaciones.Push
import cl.ventanago.app.ui.comun.ModeloBase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Avisos de la campana del encabezado: el cliente recibe las respuestas y la empresa las solicitudes nuevas. */
class AvisosViewModel : ModeloBase() {
    var avisos by mutableStateOf<List<Aviso>>(emptyList())
        private set

    val sinLeer get() = avisos.count { !it.leido }

    /**
     * Mientras la app está a la vista se revisan cada minuto (la web, cada 30 s), o apenas llega una notificación
     * push. Con la app cerrada los avisos llegan como notificación.
     */
    suspend fun sondear() {
        while (true) {
            try {
                avisos = ClienteApi.avisos().sortedByDescending { it.fecha }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Sin conexión: se reintenta en el próximo ciclo.
            }
            withTimeoutOrNull(60_000) { Push.llegadas.first() }
        }
    }

    fun marcarLeidos() {
        if (sinLeer == 0) return
        avisos = avisos.map { it.copy(leido = true) }
        viewModelScope.launch { runCatching { ClienteApi.marcarAvisosLeidos() } }
    }
}
