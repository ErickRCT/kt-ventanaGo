package cl.ventanago.app.ui.modulos.mercado

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cl.ventanago.app.datos.ChatApi
import cl.ventanago.app.datos.FotoNueva
import cl.ventanago.app.ui.Colores
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

// Fotos del lugar que el cliente adjunta a su solicitud.

private const val LADO_MAXIMO = 1280

/**
 * Lee la foto elegida, la achica a 1280 px por lado y la comprime en JPEG, para que pese bastante menos de
 * 1,5 MB (el límite del backend). Devuelve null si no se pudo leer.
 */
suspend fun comprimirFoto(context: Context, uri: Uri): Pair<FotoNueva, ImageBitmap>? = withContext(Dispatchers.IO) {
    try {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
        if (limites.outWidth <= 0) return@withContext null
        var muestra = 1
        while (max(limites.outWidth, limites.outHeight) / (muestra * 2) >= LADO_MAXIMO) muestra *= 2
        val leida = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = muestra })
        } ?: return@withContext null
        val escala = min(1f, LADO_MAXIMO.toFloat() / max(leida.width, leida.height))
        val final = if (escala < 1f) Bitmap.createScaledBitmap(leida, (leida.width * escala).toInt(), (leida.height * escala).toInt(), true) else leida
        val salida = ByteArrayOutputStream()
        final.compress(Bitmap.CompressFormat.JPEG, 75, salida)
        FotoNueva("image/jpeg", Base64.encodeToString(salida.toByteArray(), Base64.NO_WRAP)) to final.asImageBitmap()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
}

/** Miniaturas de las fotos de una solicitud; al tocar una se ve completa. */
@Composable
fun FotosSolicitud(numero: Long, ids: List<Long>) {
    if (ids.isEmpty()) return
    val imagenes = remember(numero) { mutableStateMapOf<Long, ImageBitmap>() }
    var abierta by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(numero, ids) {
        ids.filter { it !in imagenes }.forEach { id ->
            try {
                val bytes = ChatApi.foto(numero, id)
                withContext(Dispatchers.Default) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }?.let { imagenes[id] = it.asImageBitmap() }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Si una foto no carga se deja el recuadro vacío.
            }
        }
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ids.forEach { id ->
            val img = imagenes[id]
            Box(
                Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Colores.borde)
                    .clickable(enabled = img != null) { abierta = img },
                contentAlignment = Alignment.Center,
            ) {
                if (img == null) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Image(img, contentDescription = "Foto del lugar", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
    }
    abierta?.let { img ->
        Dialog(onDismissRequest = { abierta = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { abierta = null },
                contentAlignment = Alignment.Center,
            ) {
                Image(img, contentDescription = "Foto del lugar", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth())
                IconButton(onClick = { abierta = null }, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color(0x66000000), CircleShape)) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
                }
            }
        }
    }
}
