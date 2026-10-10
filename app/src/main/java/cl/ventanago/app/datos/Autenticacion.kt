package cl.ventanago.app.datos

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

/** Sesión activa, guardada en el teléfono para no pedir la contraseña cada vez que se abre la app. */
class Autenticacion(context: Context) {
    private val preferencias = context.getSharedPreferences("ventanago", Context.MODE_PRIVATE)

    var sesion by mutableStateOf(leer())
        private set

    /** Motivo del último cierre automático (sesión vencida), para mostrarlo en el login. */
    var aviso by mutableStateOf<String?>(null)
        private set

    init {
        Api.token = sesion?.token
        Api.alVencerSesion = {
            cerrar()
            aviso = "Tu sesión venció. Vuelve a iniciar sesión."
        }
    }

    fun iniciar(nueva: Sesion) {
        preferencias.edit { putString(CLAVE, Api.json.encodeToString(Sesion.serializer(), nueva)) }
        Api.token = nueva.token
        aviso = null
        sesion = nueva
    }

    fun cerrar() {
        preferencias.edit { remove(CLAVE) }
        Api.token = null
        sesion = null
    }

    private fun leer(): Sesion? = try {
        preferencias.getString(CLAVE, null)
            ?.let { Api.json.decodeFromString(Sesion.serializer(), it) }
            ?.takeIf { it.expiraEn > System.currentTimeMillis() }
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val CLAVE = "sesion"
    }
}
