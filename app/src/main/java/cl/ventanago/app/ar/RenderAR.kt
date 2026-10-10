package cl.ventanago.app.ar

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import cl.ventanago.app.ar.render.Cajas
import cl.ventanago.app.ar.render.FondoCamara
import cl.ventanago.app.ar.render.Planos
import cl.ventanago.app.ventana.COLOR_HERRAJE
import cl.ventanago.app.ventana.Caja
import cl.ventanago.app.ventana.ConfigVentana
import cl.ventanago.app.ventana.Figura
import cl.ventanago.app.ventana.Material
import cl.ventanago.app.ventana.opacidadVidrio
import cl.ventanago.app.ventana.piezasVentana
import com.google.ar.core.Anchor
import com.google.ar.core.Camera
import com.google.ar.core.Config
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.SessionPausedException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentLinkedQueue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** Lo que la pantalla muestra sobre la cámara. */
data class EstadoAR(
    val mensaje: String = "Iniciando la cámara…",
    /** Hay al menos una pared detectada. */
    val paredDetectada: Boolean = false,
    val colocada: Boolean = false,
    /** Pasó un rato sin encontrar pared: se ofrece colocarla frente al cliente. */
    val sugerirFrente: Boolean = false,
    /** Se pasó a modo liviano porque el teléfono iba lento. */
    val liviana: Boolean = false,
    val avisoLiviana: Boolean = false,
    /** Error que impide seguir con la RA (se ofrece la vista con cámara). */
    val error: String? = null,
)

/** Pedidos desde la pantalla, que se atienden en el hilo de OpenGL. */
sealed interface Pedido {
    data class Tocar(val x: Float, val y: Float) : Pedido
    data object ColocarFrente : Pedido
    data object Quitar : Pedido
}

/**
 * Dibuja la cámara y la ventana a escala 1:1 anclada a la pared. Corre en el hilo de GLSurfaceView;
 * se comunica con la pantalla por [estado] y [pedir].
 */
class RenderAR(
    private val session: Session,
    config: ConfigVentana,
    livianaInicial: Boolean,
    private val rotacionPantalla: () -> Int,
) : GLSurfaceView.Renderer {

    private val _estado = MutableStateFlow(EstadoAR(liviana = livianaInicial))
    val estado: StateFlow<EstadoAR> = _estado

    private val pedidos = ConcurrentLinkedQueue<Pedido>()
    fun pedir(p: Pedido) {
        pedidos += p
    }

    private val fondo = FondoCamara()
    private val cajas = Cajas()
    private val planos = Planos()

    private var liviana = livianaInicial
    private var ancla: Anchor? = null
    /** Ejes de la ventana al colocarla: derecha, arriba y frente (normal de la pared). */
    private val giro = FloatArray(16).also { Matrix.setIdentityM(it, 0) }

    private val vista = FloatArray(16)
    private val proyeccion = FloatArray(16)
    private val modelo = FloatArray(16)
    private val temporal = FloatArray(16)
    private val mvp = FloatArray(16)
    private val vistaModelo = FloatArray(16)

    private val bloques: List<Bloque> = bloquesDe(config)
    private var ancho = 0
    private var alto = 0
    private var inicio = 0L
    private var muestrasLentas = 0
    private var ultimoCuadro = 0L

    /** Caja ya resuelta (las figuras del arco se aproximan con cajas). */
    private class Bloque(val w: Float, val h: Float, val d: Float, val x: Float, val y: Float, val z: Float, val rgba: FloatArray, val vidrio: Boolean)

    override fun onSurfaceCreated(gl: GL10?, eglConfig: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        try {
            fondo.crear()
            cajas.crear()
            planos.crear()
            session.setCameraTextureName(fondo.textura)
        } catch (e: Exception) {
            fallar("No se pudieron preparar los gráficos de la realidad aumentada en este teléfono.")
        }
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        ancho = w
        alto = h
        GLES20.glViewport(0, 0, w, h)
        session.setDisplayGeometry(rotacionPantalla(), w, h)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (_estado.value.error != null) return
        val frame: Frame = try {
            session.update()
        } catch (_: SessionPausedException) {
            return
        } catch (_: CameraNotAvailableException) {
            fallar("Otra aplicación está usando la cámara. Ciérrala y vuelve a intentarlo, o usa la vista con cámara.")
            return
        } catch (e: Exception) {
            fallar("La realidad aumentada se detuvo inesperadamente en este teléfono.")
            return
        }
        if (inicio == 0L) inicio = System.currentTimeMillis()
        medirRendimiento()

        fondo.dibujar(frame)
        val camara = frame.camera
        atenderPedidos(frame, camara)

        if (camara.trackingState != TrackingState.TRACKING) {
            actualizar { it.copy(mensaje = mensajeSinSeguimiento(camara.trackingFailureReason)) }
            return
        }

        camara.getViewMatrix(vista, 0)
        camara.getProjectionMatrix(proyeccion, 0, 0.05f, 50f)

        val paredes = session.getAllTrackables(Plane::class.java)
            .any { it.trackingState == TrackingState.TRACKING && it.type == Plane.Type.VERTICAL && it.subsumedBy == null }
        val anclaActual = ancla?.takeIf { it.trackingState != TrackingState.STOPPED }

        if (anclaActual == null && !liviana) {
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            planos.dibujar(session.getAllTrackables(Plane::class.java), vista, proyeccion)
            GLES20.glDisable(GLES20.GL_BLEND)
        }

        if (anclaActual != null && anclaActual.trackingState == TrackingState.TRACKING) dibujarVentana(anclaActual.pose)

        val segundos = (System.currentTimeMillis() - inicio) / 1000
        actualizar {
            it.copy(
                paredDetectada = paredes,
                colocada = anclaActual != null,
                sugerirFrente = anclaActual == null && segundos >= 12,
                mensaje = when {
                    anclaActual != null -> "Toca otra parte de la pared para moverla."
                    paredes -> "Toca la pared (zona blanca) donde quieres ver la ventana."
                    segundos >= 12 -> "¿Pared lisa? Apunta a un borde, un cuadro o un enchufe, o colócala frente a ti."
                    else -> "Apunta a la pared y mueve el teléfono lentamente de lado a lado."
                },
            )
        }
    }

    private fun dibujarVentana(pose: Pose) {
        // Modelo = posición del ancla × giro calculado al colocarla (la ventana siempre derecha).
        val base = FloatArray(16)
        Matrix.setIdentityM(base, 0)
        Matrix.translateM(base, 0, pose.tx(), pose.ty(), pose.tz())
        Matrix.multiplyMM(temporal, 0, base, 0, giro, 0)
        val raiz = temporal.copyOf()

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        cajas.empezar()
        // Primero lo opaco y después los vidrios (de atrás hacia adelante), con transparencia.
        for (pasada in 0..1) {
            val vidrio = pasada == 1
            if (vidrio) {
                GLES20.glEnable(GLES20.GL_BLEND)
                GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
                GLES20.glDepthMask(false)
            }
            for (b in bloques) {
                if (b.vidrio != vidrio) continue
                Matrix.setIdentityM(modelo, 0)
                Matrix.translateM(modelo, 0, b.x, b.y, b.z)
                Matrix.scaleM(modelo, 0, b.w, b.h, b.d)
                Matrix.multiplyMM(temporal, 0, raiz, 0, modelo, 0)
                Matrix.multiplyMM(vistaModelo, 0, vista, 0, temporal, 0)
                Matrix.multiplyMM(mvp, 0, proyeccion, 0, vistaModelo, 0)
                cajas.dibujar(mvp, giro, b.rgba)
            }
        }
        GLES20.glDepthMask(true)
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    private fun atenderPedidos(frame: Frame, camara: Camera) {
        while (true) {
            when (val p = pedidos.poll() ?: break) {
                is Pedido.Quitar -> {
                    ancla?.detach()
                    ancla = null
                }
                is Pedido.ColocarFrente -> if (camara.trackingState == TrackingState.TRACKING) colocarFrente(camara)
                is Pedido.Tocar -> if (camara.trackingState == TrackingState.TRACKING) colocarEnPared(frame, p.x, p.y)
            }
        }
    }

    /**
     * Busca una pared bajo el dedo: plano vertical detectado, punto de profundidad (paredes lisas en teléfonos
     * con Depth API) o punto con normal estimada. La normal debe ser casi horizontal para que sea pared.
     */
    private fun colocarEnPared(frame: Frame, x: Float, y: Float) {
        val acierto: HitResult? = frame.hitTest(x, y).firstOrNull { hit ->
            val normal = hit.hitPose.yAxis
            val esPared = abs(normal[1]) < 0.5f
            when (val t = hit.trackable) {
                is Plane -> t.type == Plane.Type.VERTICAL && t.isPoseInPolygon(hit.hitPose)
                is DepthPoint -> esPared
                is Point -> t.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL && esPared
                else -> false
            }
        }
        if (acierto == null) {
            actualizar { it.copy(mensaje = "Ahí no se detectó una pared. Toca la zona blanca o colócala frente a ti.") }
            return
        }
        val normal = acierto.hitPose.yAxis
        val nueva = acierto.createAnchor()
        // La ventana se separa de la pared la mitad de su profundidad, para que no quede "enterrada".
        ajustarGiro(normal[0], normal[2])
        ancla?.detach()
        ancla = nueva
    }

    /** Sin pared detectada: la ventana queda a 1,5 m frente a la cámara, derecha y mirando al cliente. */
    private fun colocarFrente(camara: Camera) {
        val pose = camara.displayOrientedPose
        val adelante = pose.zAxis.let { floatArrayOf(-it[0], -it[1], -it[2]) }
        val horizontal = sqrt(adelante[0] * adelante[0] + adelante[2] * adelante[2]).takeIf { it > 0.01f } ?: return
        val dx = adelante[0] / horizontal
        val dz = adelante[2] / horizontal
        val distancia = 1.5f
        val posicion = Pose.makeTranslation(pose.tx() + dx * distancia, pose.ty(), pose.tz() + dz * distancia)
        ajustarGiro(-dx, -dz)
        ancla?.detach()
        ancla = session.createAnchor(posicion)
    }

    /** Giro de la ventana: frente = normal de la pared (sobre el plano horizontal), arriba = vertical. */
    private fun ajustarGiro(nx: Float, nz: Float) {
        val angulo = atan2(nx, nz)
        Matrix.setIdentityM(giro, 0)
        giro[0] = cos(angulo)
        giro[2] = -sin(angulo)
        giro[8] = sin(angulo)
        giro[10] = cos(angulo)
    }

    /** Si el teléfono va bajo ~20 cuadros por segundo durante un rato, se quitan los efectos (pasa a liviano). */
    private fun medirRendimiento() {
        val ahora = System.nanoTime()
        if (ultimoCuadro != 0L) {
            val ms = (ahora - ultimoCuadro) / 1_000_000
            if (ms > 50) muestrasLentas++ else muestrasLentas = max(0, muestrasLentas - 1)
        }
        ultimoCuadro = ahora
        if (!liviana && muestrasLentas > 90) {
            liviana = true
            try {
                val config = session.config
                if (config.depthMode != Config.DepthMode.DISABLED) {
                    config.depthMode = Config.DepthMode.DISABLED
                    session.configure(config)
                }
            } catch (_: Exception) {
                // Seguir sin cambiar la configuración: igual se deja de dibujar las paredes.
            }
            actualizar { it.copy(liviana = true, avisoLiviana = true) }
        }
    }

    private fun mensajeSinSeguimiento(motivo: TrackingFailureReason) = when (motivo) {
        TrackingFailureReason.INSUFFICIENT_LIGHT -> "Hay poca luz. Enciende una luz o abre las cortinas."
        TrackingFailureReason.EXCESSIVE_MOTION -> "Mueve el teléfono más despacio."
        TrackingFailureReason.INSUFFICIENT_FEATURES -> "No hay suficiente detalle: apunta a una zona con bordes, muebles o cuadros."
        TrackingFailureReason.CAMERA_UNAVAILABLE -> "La cámara no está disponible. Cierra otras apps que la usen."
        TrackingFailureReason.BAD_STATE -> "Reiniciando el seguimiento… mantén el teléfono quieto un momento."
        else -> "Apunta a la pared y mueve el teléfono lentamente de lado a lado."
    }

    private fun fallar(mensaje: String) = actualizar { it.copy(error = mensaje) }

    private inline fun actualizar(cambio: (EstadoAR) -> EstadoAR) {
        val nuevo = cambio(_estado.value)
        if (nuevo != _estado.value) _estado.value = nuevo
    }

    fun ocultarAvisoLiviana() = actualizar { it.copy(avisoLiviana = false) }

    private companion object {
        private const val PROFUNDIDAD = 0.07f

        fun rgba(rgb: Int, alfa: Float) = floatArrayOf(((rgb shr 16) and 0xFF) / 255f, ((rgb shr 8) and 0xFF) / 255f, (rgb and 0xFF) / 255f, alfa)

        /** Piezas de la geometría como cajas; el arco (figuras) se aproxima con tiras y tramos. */
        fun bloquesDe(config: ConfigVentana): List<Bloque> {
            val vidrio = rgba(config.colorVidrio, opacidadVidrio(config.modelo))
            fun color(m: Material) = when (m) {
                Material.MARCO -> rgba(config.colorMarco, 1f)
                Material.VIDRIO -> vidrio
                Material.HERRAJE -> rgba(COLOR_HERRAJE, 1f)
            }
            // La ventana queda delante de la pared: su cara trasera toca el punto de la pared.
            val sobrePared = PROFUNDIDAD / 2
            val lista = mutableListOf<Bloque>()
            for (pieza in piezasVentana(config)) {
                when (pieza) {
                    is Caja -> lista += Bloque(pieza.w, pieza.h, pieza.d, pieza.x, pieza.y, pieza.z + sobrePared, color(pieza.material), pieza.material == Material.VIDRIO)
                    is Figura -> lista += figuraComoCajas(pieza, color(pieza.material), sobrePared)
                }
            }
            return lista
        }

        private fun figuraComoCajas(f: Figura, rgba: FloatArray, sobrePared: Float): List<Bloque> {
            val esVidrio = f.material == Material.VIDRIO
            val puntos = f.contorno
            if (esVidrio) {
                // Tiras verticales que siguen la forma del arco.
                val minX = puntos.minOf { it.first }
                val maxX = puntos.maxOf { it.first }
                val base = puntos.minOf { it.second }
                val tiras = 24
                val ancho = (maxX - minX) / tiras
                return (0 until tiras).mapNotNull { i ->
                    val cx = minX + ancho * (i + 0.5f)
                    val tope = alturaEn(puntos, cx) ?: return@mapNotNull null
                    val h = tope - base
                    if (h <= 0) null else Bloque(ancho * 1.02f, h, 0.008f, cx, base + h / 2, f.z + sobrePared, rgba, true)
                }
            }
            // Marco: un tramo de perfil entre cada par de puntos del contorno exterior.
            val grosor = 0.05f
            return puntos.indices.map { i ->
                val (x1, y1) = puntos[i]
                val (x2, y2) = puntos[(i + 1) % puntos.size]
                val largo = sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))
                // Tramos horizontales/verticales/inclinados: se aproximan con su caja envolvente más el grosor.
                val w = max(abs(x2 - x1), grosor)
                val h = max(abs(y2 - y1), grosor)
                Bloque(if (largo < grosor) grosor else w, if (largo < grosor) grosor else h, 0.035f, (x1 + x2) / 2, (y1 + y2) / 2, f.z + sobrePared, rgba, false)
            }
        }

        /** Altura del contorno superior en x (el punto más alto del polígono en esa vertical). */
        private fun alturaEn(puntos: List<Pair<Float, Float>>, x: Float): Float? {
            var tope: Float? = null
            for (i in puntos.indices) {
                val (x1, y1) = puntos[i]
                val (x2, y2) = puntos[(i + 1) % puntos.size]
                if ((x in minOf(x1, x2)..maxOf(x1, x2)) && x1 != x2) {
                    val y = y1 + (y2 - y1) * (x - x1) / (x2 - x1)
                    if (tope == null || y > tope) tope = y
                }
            }
            return tope
        }
    }
}
