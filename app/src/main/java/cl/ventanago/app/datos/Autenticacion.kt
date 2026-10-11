package cl.ventanago.app.datos

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import cl.ventanago.app.notificaciones.Push

/** Sesión activa, guardada en el teléfono para no pedir la contraseña cada vez que se abre la app. */
class Autenticacion(context: Context) {
    private val app = context.applicationContext
    private val preferencias = preferenciasDe(app)

    var sesion by mutableStateOf(guardada(app))
        private set

    /** Motivo del último cierre automático (sesión vencida), para mostrarlo en el login. */
    var aviso by mutableStateOf<String?>(null)
        private set

    init {
        Api.token = sesion?.token
        Api.alVencerSesion = {
            cerrar(vencida = true)
            aviso = "Tu sesión venció. Vuelve a iniciar sesión."
        }
    }

    fun iniciar(nueva: Sesion) {
        preferencias.edit { putString(CLAVE, Api.json.encodeToString(Sesion.serializer(), nueva)) }
        Api.token = nueva.token
        aviso = null
        sesion = nueva
    }

    /**
     * Al cerrar sesión a mano el teléfono deja de recibir las notificaciones de la cuenta. Si la sesión [vencida]
     * se cierra sola, las sigue recibiendo: al tocar una se pide entrar de nuevo.
     */
    fun cerrar(vencida: Boolean = false) {
        val anterior = sesion?.token
        preferencias.edit { remove(CLAVE) }
        Api.token = null
        sesion = null
        if (!vencida && anterior != null) Push.quitar(app, anterior)
    }

    companion object {
        private const val CLAVE = "sesion"

        private fun preferenciasDe(context: Context) = context.getSharedPreferences("ventanago", Context.MODE_PRIVATE)

        /** Sesión guardada y vigente, también fuera de la app (p. ej. al cambiar el token de notificaciones). */
        fun guardada(context: Context): Sesion? = try {
            preferenciasDe(context).getString(CLAVE, null)
                ?.let { Api.json.decodeFromString(Sesion.serializer(), it) }
                ?.takeIf { it.expiraEn > System.currentTimeMillis() }
        } catch (_: Exception) {
            null
        }
    }
}
