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
    /** false hasta la primera respuesta del backend (para mostrar "Cargando…" en vez de "Sin avisos"). */
    var cargados by mutableStateOf(false)
        private set
    var refrescando by mutableStateOf(false)
        private set

    val sinLeer get() = avisos.count { !it.leido }

    private suspend fun traer() {
        try {
            avisos = ClienteApi.avisos().sortedByDescending { it.fecha }
            cargados = true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Sin conexión: se reintenta en el próximo ciclo.
        }
    }

    /**
     * Mientras la app está a la vista se revisan cada minuto (la web, cada 30 s), o apenas llega una notificación
     * push. Con la app cerrada los avisos llegan como notificación.
     */
    suspend fun sondear() {
        while (true) {
            traer()
            withTimeoutOrNull(60_000) { Push.llegadas.first() }
        }
    }

    /** Al deslizar hacia abajo en la pantalla de avisos. */
    fun refrescar() {
        if (refrescando) return
        viewModelScope.launch {
            refrescando = true
            traer()
            refrescando = false
        }
    }

    fun marcarLeidos() {
        if (sinLeer == 0) return
        avisos = avisos.map { it.copy(leido = true) }
        viewModelScope.launch { runCatching { ClienteApi.marcarAvisosLeidos() } }
    }
}
