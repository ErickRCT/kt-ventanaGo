package cl.ventanago.app.ar

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import cl.ventanago.app.ui.TemaVentanaGo
import cl.ventanago.app.ventana.ConfigVentana
import cl.ventanago.app.ventana.ModeloVentana
import com.google.ar.core.ArCoreApk
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import kotlinx.coroutines.launch
import java.util.EnumSet

/**
 * Ver la ventana sobre la pared. Intenta la realidad aumentada (ARCore) y, si el teléfono no puede o falla,
 * cae a la vista con cámara y, sin cámara, al dibujo: siempre explicando el motivo.
 */
class RealidadAumentadaActivity : ComponentActivity() {

    private lateinit var config: ConfigVentana
    private lateinit var nombre: String

    private var modo by mutableStateOf<Modo>(Modo.Verificando)
    private var session: Session? = null
    private var render by mutableStateOf<RenderAR?>(null)
    private var superficie: GLSurfaceView? = null
    private var esperandoInstalacion = false

    private val acciones = object : AccionesAR {
        override fun salir() = finish()
        override fun instalarArcore() = this@RealidadAumentadaActivity.instalarArcore()
        override fun pasarACamara(motivo: String?) = this@RealidadAumentadaActivity.pasarACamara(motivo)
        override fun soloDibujo(motivo: String) {
            modo = Modo.SoloDibujo(motivo)
        }
        override fun pedirPermiso() = comenzar()
        override fun abrirAjustes() = startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
        override fun superficieCreada(superficie: GLSurfaceView) {
            this@RealidadAumentadaActivity.superficie = superficie
        }
    }

    private val permisoCamara = registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) elegirModo()
        else {
            // Si ya no se puede volver a preguntar, solo queda activarlo en Ajustes.
            val bloqueado = !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
            modo = Modo.SoloDibujo(
                if (bloqueado) "Bloqueaste el permiso de cámara. Para ver la ventana en tu pared, actívalo en Ajustes > Permisos > Cámara."
                else "Sin permiso de cámara no podemos mostrar la ventana sobre tu pared. Aquí tienes el dibujo a escala.",
                abrirAjustes = bloqueado,
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        config = leerConfig(intent)
        nombre = intent.getStringExtra(EXTRA_NOMBRE).orEmpty()
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent { TemaVentanaGo { PantallaAR(modo, config, nombre, render, this, acciones) } }
        comenzar()
    }

    private fun comenzar() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) elegirModo()
        else {
            modo = Modo.PidiendoPermiso
            permisoCamara.launch(Manifest.permission.CAMERA)
        }
    }

    private fun elegirModo() {
        modo = Modo.Verificando
        lifecycleScope.launch {
            modo = when (val s = DetectorAR.revisar(this@RealidadAumentadaActivity)) {
                is SoporteAR.Completa -> Modo.Ar(s.liviana)
                is SoporteAR.InstalarArcore -> Modo.Instalar(s.actualizar)
                is SoporteAR.SoloCamara -> Modo.Camara(s.motivo)
                is SoporteAR.SinCamara -> Modo.SoloDibujo(s.motivo)
                SoporteAR.Verificando -> Modo.Verificando
            }
            if (modo is Modo.Ar) iniciarSesion()
        }
    }

    /** Abre la Play Store para instalar o actualizar ARCore; al volver, [onResume] revisa el resultado. */
    private fun instalarArcore() {
        try {
            when (ArCoreApk.getInstance().requestInstall(this, true)) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> esperandoInstalacion = true
                ArCoreApk.InstallStatus.INSTALLED -> {
                    DetectorAR.olvidar()
                    elegirModo()
                }
            }
        } catch (e: Exception) {
            pasarACamara(motivoDe(e))
        }
    }

    override fun onResume() {
        super.onResume()
        if (esperandoInstalacion) {
            esperandoInstalacion = false
            try {
                if (ArCoreApk.getInstance().requestInstall(this, false) == ArCoreApk.InstallStatus.INSTALLED) {
                    DetectorAR.olvidar()
                    elegirModo()
                }
            } catch (e: Exception) {
                pasarACamara(motivoDe(e))
            }
            return
        }
        // Volvió de Ajustes con el permiso de cámara activado: se intenta de nuevo.
        if ((modo as? Modo.SoloDibujo)?.abrirAjustes == true &&
            checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        ) {
            elegirModo()
            return
        }
        reanudarSesion()
    }

    override fun onPause() {
        super.onPause()
        superficie?.onPause()
        session?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        session?.close()
        session = null
    }

    /** Crea y configura la sesión de ARCore con lo que soporte el teléfono. */
    private fun iniciarSesion() {
        val liviana = (modo as? Modo.Ar)?.liviana ?: return
        try {
            val nueva = Session(this)
            val configAr = Config(nueva).apply {
                // Solo paredes: buscar también el piso gasta batería y no sirve para una ventana.
                planeFindingMode = Config.PlaneFindingMode.VERTICAL
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                focusMode = Config.FocusMode.AUTO
                lightEstimationMode = Config.LightEstimationMode.DISABLED
                // La profundidad permite colocar la ventana en paredes lisas, donde no se detectan planos.
                depthMode = if (!liviana && nueva.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
            }
            if (liviana) {
                // En teléfonos con poca memoria, cámara a 30 cuadros por segundo.
                val filtro = CameraConfigFilter(nueva).setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30))
                nueva.getSupportedCameraConfigs(filtro).firstOrNull()?.let { nueva.cameraConfig = it }
            }
            nueva.configure(configAr)
            session = nueva
            render = RenderAR(nueva, config, liviana) { rotacionPantalla() }
            reanudarSesion()
        } catch (e: Exception) {
            session?.close()
            session = null
            pasarACamara(motivoDe(e))
        }
    }

    private fun reanudarSesion() {
        val s = session ?: return
        try {
            s.resume()
            superficie?.onResume()
        } catch (e: CameraNotAvailableException) {
            pasarACamara("La cámara está ocupada por otra aplicación. Ciérrala y vuelve a abrir la realidad aumentada.")
        } catch (e: Exception) {
            pasarACamara(motivoDe(e))
        }
    }

    private fun pasarACamara(motivo: String?) {
        superficie?.onPause()
        session?.pause()
        session?.close()
        session = null
        render = null
        superficie = null
        modo = Modo.Camara(motivo)
    }

    private fun motivoDe(e: Exception) = when (e) {
        is UnavailableDeviceNotCompatibleException -> "Este teléfono no es compatible con ARCore."
        is UnavailableUserDeclinedInstallationException -> "No se instaló ARCore (Servicios de Google Play para RA)."
        is UnavailableArcoreNotInstalledException -> "ARCore no está instalado en este teléfono."
        is UnavailableApkTooOldException -> "ARCore está desactualizado. Actualízalo desde la Play Store."
        is UnavailableSdkTooOldException -> "Esta versión de la app necesita actualizarse para usar ARCore."
        else -> "La realidad aumentada no pudo iniciar en este teléfono."
    } + " Te mostramos la ventana sobre la cámara con un tamaño aproximado."

    @Suppress("DEPRECATION")
    private fun rotacionPantalla(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display?.rotation ?: Surface.ROTATION_0
    else windowManager.defaultDisplay.rotation

    companion object {
        private const val EXTRA_ANCHO = "ancho"
        private const val EXTRA_ALTO = "alto"
        private const val EXTRA_HOJAS = "hojas"
        private const val EXTRA_MODELO = "modelo"
        private const val EXTRA_MARCO = "marco"
        private const val EXTRA_VIDRIO = "vidrio"
        private const val EXTRA_NOMBRE = "nombre"

        fun abrir(context: Context, config: ConfigVentana, nombre: String) {
            context.startActivity(
                Intent(context, RealidadAumentadaActivity::class.java)
                    .putExtra(EXTRA_ANCHO, config.anchoMm)
                    .putExtra(EXTRA_ALTO, config.altoMm)
                    .putExtra(EXTRA_HOJAS, config.hojas)
                    .putExtra(EXTRA_MODELO, config.modelo.name)
                    .putExtra(EXTRA_MARCO, config.colorMarco)
                    .putExtra(EXTRA_VIDRIO, config.colorVidrio)
                    .putExtra(EXTRA_NOMBRE, nombre),
            )
        }

        private fun leerConfig(i: Intent) = ConfigVentana(
            anchoMm = i.getIntExtra(EXTRA_ANCHO, 1200),
            altoMm = i.getIntExtra(EXTRA_ALTO, 1000),
            hojas = i.getIntExtra(EXTRA_HOJAS, 2),
            modelo = runCatching { ModeloVentana.valueOf(i.getStringExtra(EXTRA_MODELO).orEmpty()) }.getOrDefault(ModeloVentana.CORREDERA),
            colorMarco = i.getIntExtra(EXTRA_MARCO, 0xB8BCC7),
            colorVidrio = i.getIntExtra(EXTRA_VIDRIO, 0xBFE6FF),
        )
    }
}
