package cl.ventanago.app.ar

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.NoPhotography
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.TemaVentanaGo
import cl.ventanago.app.ventana.ConfigVentana
import cl.ventanago.app.ventana.DibujoVentana
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

/** Cómo se muestra la ventana sobre la pared, según lo que pueda hacer el teléfono. */
private sealed interface Modo {
    data object Verificando : Modo
    data object PidiendoPermiso : Modo
    data class Instalar(val actualizar: Boolean) : Modo
    data class Ar(val liviana: Boolean) : Modo
    data class Camara(val motivo: String?) : Modo
    data class SoloDibujo(val motivo: String, val abrirAjustes: Boolean = false) : Modo
}

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
        setContent { TemaVentanaGo { Pantalla() } }
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

    private fun pasarACamara(motivo: String) {
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

    // ---------- Interfaz ----------

    @Composable
    private fun Pantalla() {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            when (val m = modo) {
                Modo.Verificando, Modo.PidiendoPermiso -> Centro {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(16.dp))
                    Text(if (m == Modo.PidiendoPermiso) "Esperando el permiso de cámara…" else "Revisando qué puede hacer tu teléfono…", color = Color.White)
                }
                is Modo.Instalar -> Centro {
                    Icon(Icons.Outlined.ViewInAr, contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (m.actualizar) "Actualiza la realidad aumentada" else "Activa la realidad aumentada",
                        color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tu teléfono es compatible, pero necesita \"Servicios de Google Play para RA\" " +
                            (if (m.actualizar) "actualizado" else "instalado") + " (gratis, desde la Play Store).",
                        color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = { instalarArcore() }) { Text(if (m.actualizar) "Actualizar" else "Instalar") }
                    TextButton(onClick = { modo = Modo.Camara(null) }) { Text("Usar la vista con cámara", color = Color.White) }
                }
                is Modo.Ar -> VistaAr()
                is Modo.Camara -> {
                    VistaCamara(config, this@RealidadAumentadaActivity) { motivo ->
                        modo = Modo.SoloDibujo(motivo)
                    }
                    m.motivo?.let { Aviso(it, Modifier.align(Alignment.TopCenter).systemBarsPadding().padding(top = 68.dp)) }
                }
                is Modo.SoloDibujo -> Column(
                    Modifier
                        .fillMaxSize()
                        .background(Colores.fondo)
                        .systemBarsPadding()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Colores.texto)
                        }
                        Text(nombre.ifBlank { "Tu ventana" }, fontWeight = FontWeight.SemiBold, maxLines = 1, color = Colores.texto)
                    }
                    Spacer(Modifier.height(8.dp))
                    Icon(Icons.Outlined.NoPhotography, contentDescription = null, tint = Colores.gris, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(m.motivo, textAlign = TextAlign.Center, color = Colores.textoSecundario)
                    Spacer(Modifier.height(16.dp))
                    // El dibujo se ajusta al espacio que queda (una puerta alta no tapa los botones).
                    DibujoVentana(
                        config,
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                    Text("${config.anchoMm} × ${config.altoMm} mm", color = Colores.gris, modifier = Modifier.padding(top = 8.dp))
                    Spacer(Modifier.height(12.dp))
                    if (m.abrirAjustes) {
                        Button(onClick = {
                            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
                        }, modifier = Modifier.fillMaxWidth()) { Text("Abrir Ajustes") }
                    } else if (m.motivo.contains("permiso", ignoreCase = true)) {
                        Button(onClick = { comenzar() }, modifier = Modifier.fillMaxWidth()) { Text("Dar permiso de cámara") }
                    }
                    TextButton(onClick = { finish() }) { Text("Volver") }
                }
            }
            if (modo !is Modo.SoloDibujo) BarraSuperior()
        }
    }

    @Composable
    private fun BarraSuperior() {
        Row(
            Modifier
                .systemBarsPadding()
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { finish() },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.45f), CircleShape),
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White) }
            Column(
                Modifier
                    .padding(start = 10.dp)
                    .weight(1f)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(nombre.ifBlank { "Tu ventana" }, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("${config.anchoMm} × ${config.altoMm} mm", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
            }
            if (modo is Modo.Ar) {
                TextButton(
                    onClick = { pasarACamara("Cambiaste a la vista con cámara.") },
                    modifier = Modifier.padding(start = 6.dp).background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                ) {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text(" Simple", color = Color.White)
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Composable
    private fun VistaAr() {
        val r = render ?: return Centro { CircularProgressIndicator(color = Color.White) }
        val estado by r.estado.collectAsState()

        LaunchedEffect(estado.error) {
            estado.error?.let { pasarACamara("$it Te mostramos la ventana sobre la cámara con un tamaño aproximado.") }
        }

        Box(Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    GLSurfaceView(ctx).apply {
                        preserveEGLContextOnPause = true
                        setEGLContextClientVersion(2)
                        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                        setRenderer(r)
                        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                        val gestos = GestureDetector(ctx, object : GestureDetector.SimpleOnGestureListener() {
                            override fun onSingleTapUp(e: MotionEvent): Boolean {
                                r.pedir(Pedido.Tocar(e.x, e.y))
                                return true
                            }

                            override fun onDown(e: MotionEvent) = true
                        })
                        setOnTouchListener { _, evento -> gestos.onTouchEvent(evento) }
                        superficie = this
                        onResume()
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            // Mira en el centro mientras se busca la pared.
            if (!estado.colocada) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(28.dp)
                        .background(Color.White.copy(alpha = 0.25f), CircleShape),
                )
            }

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .systemBarsPadding()
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (estado.avisoLiviana) {
                    Surface(color = Color(0xFFFFF4E5), shape = RoundedCornerShape(10.dp)) {
                        Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Tu teléfono iba lento: activamos el modo liviano para que funcione más fluido.",
                                style = MaterialTheme.typography.bodySmall, color = Color(0xFF5C3A00), modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { r.ocultarAvisoLiviana() }) { Text("OK") }
                        }
                    }
                }
                Text(
                    estado.mensaje,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (estado.colocada) {
                        OutlinedButton(
                            onClick = { r.pedir(Pedido.Quitar) },
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Black.copy(alpha = 0.45f), contentColor = Color.White),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = null)
                            Text(" Quitar")
                        }
                    }
                    if (!estado.colocada || estado.sugerirFrente) {
                        FilledTonalButton(onClick = { r.pedir(Pedido.ColocarFrente) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Text(" Colocar frente a mí")
                        }
                    }
                }
                if (estado.liviana) {
                    Text("Modo liviano", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.End))
                }
            }
        }
    }

    @Composable
    private fun Centro(contenido: @Composable () -> Unit) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) { contenido() }
    }

    @Composable
    private fun Aviso(texto: String, modifier: Modifier) {
        var visible by remember(texto) { mutableStateOf(true) }
        if (!visible) return
        Surface(color = Color.Black.copy(alpha = 0.65f), shape = RoundedCornerShape(12.dp), modifier = modifier.padding(horizontal = 12.dp)) {
            Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(texto, color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                TextButton(onClick = { visible = false }) { Text("OK", color = Color.White) }
            }
        }
    }

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
