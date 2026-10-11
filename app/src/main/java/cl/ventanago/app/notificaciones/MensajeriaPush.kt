package cl.ventanago.app.notificaciones

import cl.ventanago.app.datos.Autenticacion
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.launch

/**
 * Recibe los avisos que envía el backend por Firebase (con la app abierta, en segundo plano o cerrada). Llegan
 * solo con datos (título, mensaje y número de solicitud) y la app arma la notificación.
 */
class MensajeriaPush : FirebaseMessagingService() {

    override fun onMessageReceived(mensaje: RemoteMessage) {
        val titulo = mensaje.data["titulo"]?.takeIf { it.isNotBlank() } ?: return
        Push.mostrar(applicationContext, titulo, mensaje.data["mensaje"].orEmpty(), mensaje.data["solicitud"]?.toLongOrNull())
    }

    /** Firebase cambió el token de este teléfono: se avisa al backend si hay una sesión guardada. */
    override fun onNewToken(token: String) {
        val sesion = Autenticacion.guardada(applicationContext) ?: return
        Push.alcance.launch { runCatching { Push.enviarToken("/dispositivos", token, sesion.token) } }
    }
}
