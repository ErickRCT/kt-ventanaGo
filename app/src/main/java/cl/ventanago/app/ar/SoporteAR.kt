package cl.ventanago.app.ar

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import com.google.ar.core.ArCoreApk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Lo que puede hacer el teléfono para mostrar la ventana sobre la pared, de mejor a peor. Cada nivel
 * tiene una alternativa: nunca se deja al cliente sin poder ver su ventana.
 */
sealed interface SoporteAR {
    data object Verificando : SoporteAR

    /** ARCore funciona. [liviana]: teléfono con poca memoria, se usan menos efectos y 30 fps. */
    data class Completa(val liviana: Boolean) : SoporteAR

    /** El teléfono es compatible, pero falta instalar o actualizar "Servicios de Google Play para RA". */
    data class InstalarArcore(val actualizar: Boolean) : SoporteAR

    /** Sin ARCore: la ventana se dibuja sobre la cámara con tamaño aproximado. */
    data class SoloCamara(val motivo: String, val reintentable: Boolean = false) : SoporteAR

    /** Sin cámara: solo el dibujo. */
    data class SinCamara(val motivo: String) : SoporteAR
}

object DetectorAR {
    /** ARCore exige OpenGL ES 3.0. */
    private const val GLES_MINIMO = 0x30000
    /** Bajo esto (RAM total) la RA funciona, pero mejor sin efectos extra. */
    private const val RAM_LIVIANA_BYTES = 3_500L * 1024 * 1024

    @Volatile
    private var guardado: SoporteAR? = null

    fun olvidar() {
        guardado = null
    }

    suspend fun revisar(context: Context): SoporteAR {
        guardado?.let { return it }
        val resultado = withContext(Dispatchers.Default) { calcular(context.applicationContext) }
        // Las respuestas que pueden cambiar (instalar, sin conexión) no se guardan.
        if (resultado is SoporteAR.Completa || (resultado is SoporteAR.SoloCamara && !resultado.reintentable) || resultado is SoporteAR.SinCamara) {
            guardado = resultado
        }
        return resultado
    }

    fun esTelefonoLiviano(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoria = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        return am.isLowRamDevice || memoria.totalMem < RAM_LIVIANA_BYTES
    }

    private suspend fun calcular(context: Context): SoporteAR {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            return SoporteAR.SinCamara("Este teléfono no tiene cámara, así que solo puedes ver el dibujo de la ventana.")
        }
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        if (am.deviceConfigurationInfo.reqGlEsVersion < GLES_MINIMO) {
            return SoporteAR.SoloCamara("Los gráficos de este teléfono no alcanzan para la realidad aumentada. Te mostramos la ventana sobre la cámara con un tamaño aproximado.")
        }
        // La primera consulta puede quedar "verificando" mientras ARCore pregunta a Google Play.
        var disponibilidad = ArCoreApk.getInstance().checkAvailability(context)
        var intentos = 0
        while (disponibilidad.isTransient && intentos < 20) {
            delay(250)
            disponibilidad = ArCoreApk.getInstance().checkAvailability(context)
            intentos++
        }
        return when (disponibilidad) {
            ArCoreApk.Availability.SUPPORTED_INSTALLED -> SoporteAR.Completa(liviana = esTelefonoLiviano(context))
            ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> SoporteAR.InstalarArcore(actualizar = false)
            ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD -> SoporteAR.InstalarArcore(actualizar = true)
            ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> SoporteAR.SoloCamara(
                "Este teléfono no es compatible con la realidad aumentada de Google (ARCore). Te mostramos la ventana sobre la cámara con un tamaño aproximado.",
            )
            else -> SoporteAR.SoloCamara(
                "No se pudo confirmar si el teléfono es compatible con ARCore (¿sin internet?). Mientras tanto, usa la vista con cámara.",
                reintentable = true,
            )
        }
    }
}
