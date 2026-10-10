package cl.ventanago.app.ar

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import cl.ventanago.app.ventana.ConfigVentana
import cl.ventanago.app.ventana.dibujarVentana
import cl.ventanago.app.ventana.piezasVentana
import java.util.Locale
import kotlin.math.atan
import kotlin.math.tan

/**
 * Ángulo de visión horizontal (en vertical, el lado corto del sensor) de la cámara trasera, en radianes.
 * Con él se calcula cuántos píxeles mide un metro a cierta distancia. Si no se puede leer, se usan 60°.
 */
private fun campoVisionHorizontal(context: Context): Float = try {
    val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val id = cm.cameraIdList.first { cm.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK }
    val c = cm.getCameraCharacteristics(id)
    val sensor = c.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)!!
    val focal = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)!!.first()
    // La imagen de la cámara en vertical muestra el lado corto del sensor a lo ancho.
    (2 * atan(minOf(sensor.width, sensor.height) / (2 * focal)))
} catch (_: Exception) {
    Math.toRadians(60.0).toFloat()
}

/**
 * Alternativa sin ARCore: la ventana dibujada sobre la cámara. El tamaño se calcula con el ángulo de visión de
 * la cámara y la distancia a la pared que indica el cliente; se puede mover con el dedo y ajustar con dos dedos.
 */
@Composable
fun VistaCamara(config: ConfigVentana, duenoCiclo: LifecycleOwner, alFallar: (String) -> Unit) {
    val context = LocalContext.current
    val piezas = remember(config) { piezasVentana(config) }
    val campo = remember { campoVisionHorizontal(context) }
    var distancia by remember { mutableFloatStateOf(2f) }
    var desplazamiento by remember { mutableStateOf(Offset.Zero) }
    var ajuste by remember { mutableFloatStateOf(1f) }

    val vistaPrevia = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    DisposableEffect(Unit) {
        val futuro = ProcessCameraProvider.getInstance(context)
        var proveedor: ProcessCameraProvider? = null
        futuro.addListener({
            try {
                proveedor = futuro.get().also { p ->
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(vistaPrevia.surfaceProvider) }
                    p.unbindAll()
                    p.bindToLifecycle(duenoCiclo, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                }
            } catch (e: Exception) {
                alFallar("No se pudo abrir la cámara. Puede que otra aplicación la esté usando.")
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose { proveedor?.unbindAll() }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView({ vistaPrevia }, Modifier.fillMaxSize())
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, mover, zoom, _ ->
                        desplazamiento += mover
                        ajuste = (ajuste * zoom).coerceIn(0.3f, 3f)
                    }
                },
        ) {
            // Píxeles por metro a esa distancia: ancho de pantalla / ancho de pared que alcanza a ver la cámara.
            val paredVisible = 2 * distancia * tan(campo / 2)
            val escala = size.width / paredVisible * ajuste
            dibujarVentana(config, piezas, center + desplazamiento, escala)
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                .padding(14.dp),
        ) {
            Text(
                "Párate frente a la pared a ${"%.1f".format(Locale.forLanguageTag("es-CL"), distancia)} m",
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Tamaño aproximado. Arrastra la ventana con un dedo; con dos dedos ajusta el tamaño si lo ves distinto.",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall,
            )
            Slider(
                value = distancia,
                onValueChange = { distancia = it },
                valueRange = 0.5f..5f,
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Distancia a la pared", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                if (desplazamiento != Offset.Zero || ajuste != 1f) {
                    TextButton(onClick = {
                        desplazamiento = Offset.Zero
                        ajuste = 1f
                    }) { Text("Centrar", color = Color.White) }
                }
            }
        }
    }
}
