package cl.ventanago.app.ar.render

import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/*
 * Renderizado mínimo con OpenGL ES 2.0 (sin motor 3D): imagen de la cámara de fondo, paredes detectadas
 * y cajas de colores. Es liviano a propósito, para que funcione fluido en teléfonos de gama de entrada.
 */

internal fun buffer(datos: FloatArray): FloatBuffer =
    ByteBuffer.allocateDirect(datos.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
        put(datos)
        position(0)
    }

internal fun programa(vertice: String, fragmento: String): Int {
    fun compilar(tipo: Int, codigo: String): Int {
        val shader = GLES20.glCreateShader(tipo)
        GLES20.glShaderSource(shader, codigo)
        GLES20.glCompileShader(shader)
        val ok = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, ok, 0)
        if (ok[0] == 0) throw IllegalStateException("Shader: " + GLES20.glGetShaderInfoLog(shader))
        return shader
    }
    val p = GLES20.glCreateProgram()
    GLES20.glAttachShader(p, compilar(GLES20.GL_VERTEX_SHADER, vertice))
    GLES20.glAttachShader(p, compilar(GLES20.GL_FRAGMENT_SHADER, fragmento))
    GLES20.glLinkProgram(p)
    val ok = IntArray(1)
    GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0)
    if (ok[0] == 0) throw IllegalStateException("Programa: " + GLES20.glGetProgramInfoLog(p))
    return p
}

/** Imagen de la cámara a pantalla completa (textura externa que llena ARCore). */
internal class FondoCamara {
    var textura = 0
        private set
    private var programa = 0
    private val cuadro = buffer(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f))
    private val uv = buffer(FloatArray(8))

    fun crear() {
        val t = IntArray(1)
        GLES20.glGenTextures(1, t, 0)
        textura = t[0]
        val destino = GLES11Ext.GL_TEXTURE_EXTERNAL_OES
        GLES20.glBindTexture(destino, textura)
        GLES20.glTexParameteri(destino, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(destino, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(destino, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(destino, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        programa = programa(
            """
            attribute vec4 a_Pos; attribute vec2 a_Uv; varying vec2 v_Uv;
            void main() { gl_Position = a_Pos; v_Uv = a_Uv; }
            """.trimIndent(),
            """
            #extension GL_OES_EGL_image_external : require
            precision mediump float; varying vec2 v_Uv; uniform samplerExternalOES u_Tex;
            void main() { gl_FragColor = texture2D(u_Tex, v_Uv); }
            """.trimIndent(),
        )
    }

    fun dibujar(frame: Frame) {
        // Al rotar o cambiar el tamaño, ARCore indica cómo recortar la imagen de la cámara.
        if (frame.hasDisplayGeometryChanged()) {
            frame.transformCoordinates2d(
                Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES, cuadro,
                Coordinates2d.TEXTURE_NORMALIZED, uv,
            )
        }
        if (frame.timestamp == 0L) return
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)
        GLES20.glUseProgram(programa)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textura)
        val pos = GLES20.glGetAttribLocation(programa, "a_Pos")
        val tex = GLES20.glGetAttribLocation(programa, "a_Uv")
        cuadro.position(0)
        uv.position(0)
        GLES20.glVertexAttribPointer(pos, 2, GLES20.GL_FLOAT, false, 0, cuadro)
        GLES20.glVertexAttribPointer(tex, 2, GLES20.GL_FLOAT, false, 0, uv)
        GLES20.glEnableVertexAttribArray(pos)
        GLES20.glEnableVertexAttribArray(tex)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(pos)
        GLES20.glDisableVertexAttribArray(tex)
        GLES20.glDepthMask(true)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }
}

/** Cajas iluminadas con una luz fija (sin texturas): perfiles, vidrios y herrajes de la ventana. */
internal class Cajas {
    private var programa = 0
    private lateinit var vertices: FloatBuffer
    private lateinit var normales: FloatBuffer

    fun crear() {
        // Cubo unitario centrado en el origen: 6 caras × 2 triángulos.
        val caras = listOf(
            floatArrayOf(0f, 0f, 1f) to listOf(floatArrayOf(-1f, -1f, 1f), floatArrayOf(1f, -1f, 1f), floatArrayOf(1f, 1f, 1f), floatArrayOf(-1f, 1f, 1f)),
            floatArrayOf(0f, 0f, -1f) to listOf(floatArrayOf(1f, -1f, -1f), floatArrayOf(-1f, -1f, -1f), floatArrayOf(-1f, 1f, -1f), floatArrayOf(1f, 1f, -1f)),
            floatArrayOf(1f, 0f, 0f) to listOf(floatArrayOf(1f, -1f, 1f), floatArrayOf(1f, -1f, -1f), floatArrayOf(1f, 1f, -1f), floatArrayOf(1f, 1f, 1f)),
            floatArrayOf(-1f, 0f, 0f) to listOf(floatArrayOf(-1f, -1f, -1f), floatArrayOf(-1f, -1f, 1f), floatArrayOf(-1f, 1f, 1f), floatArrayOf(-1f, 1f, -1f)),
            floatArrayOf(0f, 1f, 0f) to listOf(floatArrayOf(-1f, 1f, 1f), floatArrayOf(1f, 1f, 1f), floatArrayOf(1f, 1f, -1f), floatArrayOf(-1f, 1f, -1f)),
            floatArrayOf(0f, -1f, 0f) to listOf(floatArrayOf(-1f, -1f, -1f), floatArrayOf(1f, -1f, -1f), floatArrayOf(1f, -1f, 1f), floatArrayOf(-1f, -1f, 1f)),
        )
        val v = mutableListOf<Float>()
        val n = mutableListOf<Float>()
        for ((normal, esquinas) in caras) {
            for (i in listOf(0, 1, 2, 0, 2, 3)) {
                esquinas[i].forEach { v += it * 0.5f }
                normal.forEach { n += it }
            }
        }
        vertices = buffer(v.toFloatArray())
        normales = buffer(n.toFloatArray())
        programa = programa(
            """
            uniform mat4 u_Mvp; uniform mat4 u_Rot;
            attribute vec4 a_Pos; attribute vec3 a_Normal; varying float v_Luz;
            void main() {
                vec3 n = normalize((u_Rot * vec4(a_Normal, 0.0)).xyz);
                v_Luz = 0.6 + 0.4 * max(dot(n, normalize(vec3(0.3, 0.8, 0.5))), 0.0);
                gl_Position = u_Mvp * a_Pos;
            }
            """.trimIndent(),
            """
            precision mediump float; uniform vec4 u_Color; varying float v_Luz;
            void main() { gl_FragColor = vec4(u_Color.rgb * v_Luz, u_Color.a); }
            """.trimIndent(),
        )
    }

    fun empezar() {
        GLES20.glUseProgram(programa)
        val pos = GLES20.glGetAttribLocation(programa, "a_Pos")
        val nor = GLES20.glGetAttribLocation(programa, "a_Normal")
        vertices.position(0)
        normales.position(0)
        GLES20.glVertexAttribPointer(pos, 3, GLES20.GL_FLOAT, false, 0, vertices)
        GLES20.glVertexAttribPointer(nor, 3, GLES20.GL_FLOAT, false, 0, normales)
        GLES20.glEnableVertexAttribArray(pos)
        GLES20.glEnableVertexAttribArray(nor)
    }

    /** [mvp]: proyección × vista × modelo de la caja; [rotacion]: solo el giro, para la luz. */
    fun dibujar(mvp: FloatArray, rotacion: FloatArray, rgba: FloatArray) {
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(programa, "u_Mvp"), 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(programa, "u_Rot"), 1, false, rotacion, 0)
        GLES20.glUniform4fv(GLES20.glGetUniformLocation(programa, "u_Color"), 1, rgba, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36)
    }
}

/** Paredes detectadas, como una capa blanca translúcida: así el cliente sabe dónde puede tocar. */
internal class Planos {
    private var programa = 0
    private val modelo = FloatArray(16)
    private val mvp = FloatArray(16)
    private val vp = FloatArray(16)

    fun crear() {
        programa = programa(
            "uniform mat4 u_Mvp; attribute vec4 a_Pos; void main() { gl_Position = u_Mvp * vec4(a_Pos.x, 0.0, a_Pos.y, 1.0); }",
            "precision mediump float; uniform vec4 u_Color; void main() { gl_FragColor = u_Color; }",
        )
    }

    fun dibujar(planos: Collection<Plane>, vista: FloatArray, proyeccion: FloatArray) {
        android.opengl.Matrix.multiplyMM(vp, 0, proyeccion, 0, vista, 0)
        GLES20.glUseProgram(programa)
        GLES20.glDepthMask(false)
        val pos = GLES20.glGetAttribLocation(programa, "a_Pos")
        GLES20.glEnableVertexAttribArray(pos)
        for (plano in planos) {
            if (plano.trackingState != com.google.ar.core.TrackingState.TRACKING || plano.subsumedBy != null) continue
            if (plano.type != Plane.Type.VERTICAL) continue
            val poligono = plano.polygon ?: continue
            if (poligono.limit() < 6) continue
            plano.centerPose.toMatrix(modelo, 0)
            android.opengl.Matrix.multiplyMM(mvp, 0, vp, 0, modelo, 0)
            poligono.position(0)
            GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(programa, "u_Mvp"), 1, false, mvp, 0)
            GLES20.glUniform4f(GLES20.glGetUniformLocation(programa, "u_Color"), 1f, 1f, 1f, 0.22f)
            GLES20.glVertexAttribPointer(pos, 2, GLES20.GL_FLOAT, false, 0, poligono)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, poligono.limit() / 2)
        }
        GLES20.glDisableVertexAttribArray(pos)
        GLES20.glDepthMask(true)
    }
}
