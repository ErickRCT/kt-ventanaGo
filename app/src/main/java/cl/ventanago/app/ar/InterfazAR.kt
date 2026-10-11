package cl.ventanago.app.ar

import android.annotation.SuppressLint
import android.opengl.GLSurfaceView
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Surface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.lifecycle.LifecycleOwner
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ventana.ConfigVentana
import cl.ventanago.app.ventana.DibujoVentana
import cl.ventanago.app.ventana.medidasCm

// Interfaz de la realidad aumentada: lo que se ve en cada modo. La sesión de ARCore y el ciclo de vida están en
// RealidadAumentadaActivity.

/** Cómo se muestra la ventana sobre la pared, según lo que pueda hacer el teléfono. */
internal sealed interface Modo {
    data object Verificando : Modo
    data object PidiendoPermiso : Modo
    data class Instalar(val actualizar: Boolean) : Modo
    data class Ar(val liviana: Boolean) : Modo
    data class Camara(val motivo: String?) : Modo
    data class SoloDibujo(val motivo: String, val abrirAjustes: Boolean = false) : Modo
}

/** Lo que la interfaz le pide a la actividad. */
internal interface AccionesAR {
    fun salir()
    fun instalarArcore()
    /** Deja la realidad aumentada y usa la vista con cámara ([motivo]: por qué, para mostrarlo). */
    fun pasarACamara(motivo: String?)
    fun soloDibujo(motivo: String)
    fun pedirPermiso()
    fun abrirAjustes()
    fun superficieCreada(superficie: GLSurfaceView)
}

@Composable
internal fun PantallaAR(modo: Modo, config: ConfigVentana, nombre: String, render: RenderAR?, duenoCiclo: LifecycleOwner, acciones: AccionesAR) {
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
                Button(onClick = acciones::instalarArcore) { Text(if (m.actualizar) "Actualizar" else "Instalar") }
                TextButton(onClick = { acciones.pasarACamara(null) }) { Text("Usar la vista con cámara", color = Color.White) }
            }
            is Modo.Ar -> VistaAr(render, acciones)
            is Modo.Camara -> {
                VistaCamara(config, duenoCiclo) { motivo ->
                    acciones.soloDibujo(motivo)
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
                    IconButton(onClick = acciones::salir) {
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
                Text(medidasCm(config.anchoMm, config.altoMm), color = Colores.gris, modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(12.dp))
                if (m.abrirAjustes) {
                    Button(onClick = acciones::abrirAjustes, modifier = Modifier.fillMaxWidth()) { Text("Abrir Ajustes") }
                } else if (m.motivo.contains("permiso", ignoreCase = true)) {
                    Button(onClick = acciones::pedirPermiso, modifier = Modifier.fillMaxWidth()) { Text("Dar permiso de cámara") }
                }
                TextButton(onClick = acciones::salir) { Text("Volver") }
            }
        }
        if (modo !is Modo.SoloDibujo) BarraSuperior(modo, config, nombre, acciones)
    }
}

@Composable
private fun BarraSuperior(modo: Modo, config: ConfigVentana, nombre: String, acciones: AccionesAR) {
    Row(
        Modifier
            .systemBarsPadding()
            .padding(8.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = acciones::salir,
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
            Text(medidasCm(config.anchoMm, config.altoMm), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
        }
        if (modo is Modo.Ar) {
            TextButton(
                onClick = { acciones.pasarACamara("Cambiaste a la vista con cámara.") },
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
private fun VistaAr(render: RenderAR?, acciones: AccionesAR) {
    val r = render ?: return Centro { CircularProgressIndicator(color = Color.White) }
    val estado by r.estado.collectAsState()

    LaunchedEffect(estado.error) {
        estado.error?.let { acciones.pasarACamara("$it Te mostramos la ventana sobre la cámara con un tamaño aproximado.") }
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
                    acciones.superficieCreada(this)
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
