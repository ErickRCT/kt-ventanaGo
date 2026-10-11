package cl.ventanago.app.notificaciones

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import cl.ventanago.app.MainActivity
import cl.ventanago.app.R
import cl.ventanago.app.datos.Api
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlin.coroutines.resume

@Serializable
private data class TokenDispositivo(val token: String)

/**
 * Notificaciones push (Firebase Cloud Messaging): el backend envía cada aviso (oferta nueva, mensaje del chat,
 * oferta elegida…) al teléfono, aunque la app esté cerrada. Sin app/google-services.json Firebase no se inicia y
 * todo esto no hace nada: los avisos se siguen viendo en la campana del encabezado.
 */
object Push {
    private const val CANAL = "avisos"
    private const val CLAVE_PERMISO_PEDIDO = "permiso_notificaciones_pedido"

    /** Extra del intent con que la notificación abre la app: número de la solicitud del aviso. */
    const val EXTRA_SOLICITUD = "solicitud"

    /** Para lo que debe terminar aunque se cierre la pantalla (p. ej. quitar el teléfono al cerrar sesión). */
    val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _llegadas = MutableSharedFlow<Long?>(extraBufferCapacity = 8)

    /** Avisos que llegan con la app abierta (número de solicitud): las pantallas se actualizan sin esperar. */
    val llegadas: SharedFlow<Long?> = _llegadas.asSharedFlow()

    fun disponible(context: Context) = FirebaseApp.getApps(context).isNotEmpty()

    /**
     * Android 13+ pide permiso para mostrar notificaciones. Se pregunta una sola vez (después se puede activar en
     * los ajustes del teléfono); devuelve true si toca preguntar ahora.
     */
    fun debePedirPermiso(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || !disponible(context)) return false
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return false
        val preferencias = context.getSharedPreferences("ventanago", Context.MODE_PRIVATE)
        if (preferencias.getBoolean(CLAVE_PERMISO_PEDIDO, false)) return false
        preferencias.edit { putBoolean(CLAVE_PERMISO_PEDIDO, true) }
        return true
    }

    fun crearCanal(context: Context) {
        NotificationManagerCompat.from(context).createNotificationChannel(
            NotificationChannelCompat.Builder(CANAL, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName("Avisos de tus solicitudes")
                .setDescription("Ofertas nuevas, mensajes del chat y respuestas a tus solicitudes")
                .build(),
        )
    }

    /**
     * Asocia este teléfono a la cuenta de la sesión, para que el backend le envíe sus avisos. Sin conexión no pasa
     * nada: se vuelve a intentar la próxima vez que se abra la app.
     */
    suspend fun registrar(context: Context, tokenSesion: String) {
        if (!disponible(context)) return
        try {
            tokenFcm()?.let { enviarToken("/dispositivos", it, tokenSesion) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Se reintenta al abrir la app.
        }
    }

    /** Al cerrar sesión el teléfono deja de recibir los avisos de esa cuenta. */
    fun quitar(context: Context, tokenSesion: String) {
        if (!disponible(context)) return
        alcance.launch {
            runCatching { tokenFcm()?.let { enviarToken("/dispositivos/quitar", it, tokenSesion) } }
        }
    }

    suspend fun enviarToken(ruta: String, token: String, tokenSesion: String) {
        Api.enviar("POST", ruta, TokenDispositivo(token), TokenDispositivo.serializer(), tokenSesion)
    }

    private suspend fun tokenFcm(): String? = suspendCancellableCoroutine { continuacion ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { tarea ->
            continuacion.resume(if (tarea.isSuccessful) tarea.result else null)
        }
    }

    /** Muestra el aviso en la barra de notificaciones; al tocarlo abre la solicitud. */
    @SuppressLint("MissingPermission") // areNotificationsEnabled() revisa el permiso; si se quita justo ahora, se captura abajo.
    fun mostrar(context: Context, titulo: String, mensaje: String, solicitud: Long?) {
        _llegadas.tryEmit(solicitud)
        val gestor = NotificationManagerCompat.from(context)
        if (!gestor.areNotificationsEnabled()) return
        crearCanal(context)
        // Una notificación por solicitud: un aviso nuevo de la misma solicitud reemplaza al anterior.
        val id = (solicitud ?: System.currentTimeMillis()).hashCode()
        val abrir = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .apply { solicitud?.let { putExtra(EXTRA_SOLICITUD, it) } }
        val notificacion = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setColor(ContextCompat.getColor(context, R.color.azul_ventanago))
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(context, id, abrir, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            .build()
        try {
            gestor.notify(id, notificacion)
        } catch (_: SecurityException) {
            // Se quitó el permiso de notificaciones justo ahora: el aviso queda en la campana.
        }
    }
}
