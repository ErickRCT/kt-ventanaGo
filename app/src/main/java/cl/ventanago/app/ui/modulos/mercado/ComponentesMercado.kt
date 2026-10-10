package cl.ventanago.app.ui.modulos.mercado

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cl.ventanago.app.datos.ChatApi
import cl.ventanago.app.datos.Comuna
import cl.ventanago.app.datos.FotoNueva
import cl.ventanago.app.datos.MensajeChat
import cl.ventanago.app.datos.ProveedorDetalle
import cl.ventanago.app.datos.ProveedorResumen
import cl.ventanago.app.datos.ProveedoresApi
import cl.ventanago.app.datos.Recursos
import cl.ventanago.app.datos.Region
import cl.ventanago.app.datos.mensaje
import cl.ventanago.app.datos.nombreEstadoSolicitud
import cl.ventanago.app.datos.nombreServicio
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.comun.EstadoCargando
import cl.ventanago.app.ui.comun.EstadoError
import cl.ventanago.app.ui.comun.MensajeError
import cl.ventanago.app.ui.comun.Selector
import cl.ventanago.app.ui.modulos.Etiqueta
import cl.ventanago.app.ui.modulos.cotizaciones.fechaLegible
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

// Piezas que comparten las pantallas del mercado: estrellas, proveedores, chat, fotos y comuna.

@Composable
fun EtiquetaEstadoSolicitud(estado: String) = when (estado) {
    "ADJUDICADA" -> Etiqueta("Adjudicada", Colores.azulClaro, Colores.azul)
    "TERMINADA" -> Etiqueta("Terminada", Color(0xFFE8F5E9), Colores.exito)
    "CANCELADA" -> Etiqueta("Cancelada", Color(0xFFEDEFF2), Colores.gris)
    else -> Etiqueta(nombreEstadoSolicitud(estado), Color(0xFFFFF4E5), Color(0xFF8A5300))
}

// ---------- Estrellas ----------

private val amarillo = Color(0xFFF2A900)

/** Promedio en estrellas con la cantidad de valoraciones; "Sin valoraciones" si no hay. */
@Composable
fun Estrellas(promedio: Double?, cantidad: Long, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (promedio == null || cantidad == 0L) {
            Text("Sin valoraciones aún", style = MaterialTheme.typography.bodySmall, color = Colores.gris)
            return@Row
        }
        for (i in 1..5) {
            val icono = when {
                promedio >= i -> Icons.Filled.Star
                promedio >= i - 0.5 -> Icons.Filled.StarHalf
                else -> Icons.Filled.StarBorder
            }
            Icon(icono, contentDescription = null, tint = amarillo, modifier = Modifier.size(16.dp))
        }
        Text(" ${"%.1f".format(promedio)} ($cantidad)", style = MaterialTheme.typography.bodySmall, color = Colores.textoSecundario)
    }
}

/** Para valorar: cinco estrellas que se tocan. */
@Composable
fun SelectorEstrellas(valor: Int, alCambiar: (Int) -> Unit) {
    Row {
        for (i in 1..5) {
            IconButton(onClick = { alCambiar(i) }) {
                Icon(
                    if (i <= valor) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "$i estrella(s)",
                    tint = amarillo,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

// ---------- Proveedores ----------

@Composable
fun NombreProveedor(p: ProveedorResumen, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(p.nombre, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f, fill = false))
        if (p.verificado) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Filled.Verified, contentDescription = "Verificado", tint = Colores.azul, modifier = Modifier.size(18.dp))
        }
    }
}

/** Experiencia y trabajos hechos en VentanaGo, en una línea. */
fun experiencia(p: ProveedorResumen) = listOfNotNull(
    p.aniosExperiencia.takeIf { it > 0 }?.let { "$it años de experiencia" },
    "${p.trabajos} ${if (p.trabajos == 1L) "trabajo" else "trabajos"} en VentanaGo",
).joinToString(" · ")

@Composable
fun TarjetaProveedor(p: ProveedorResumen, alTocar: () -> Unit, extra: @Composable () -> Unit = {}) {
    Card(
        onClick = alTocar,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Colores.borde),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NombreProveedor(p, Modifier.weight(1f))
                extra()
            }
            Estrellas(p.promedio, p.valoraciones)
            Text(experiencia(p), style = MaterialTheme.typography.bodySmall, color = Colores.gris)
            if (p.servicios.isNotEmpty()) {
                Text(p.servicios.joinToString(" · ") { nombreServicio(it) }, style = MaterialTheme.typography.bodySmall, color = Colores.textoSecundario)
            }
        }
    }
}

/** Ficha pública del proveedor con sus últimas valoraciones. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoProveedor(cuentaId: Long, alCerrar: () -> Unit) {
    var detalle by remember { mutableStateOf<ProveedorDetalle?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableStateOf(0) }
    LaunchedEffect(cuentaId, recarga) {
        try {
            detalle = ProveedoresApi.detalle(cuentaId)
            error = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.mensaje()
        }
    }
    PantallaDialogo(detalle?.proveedor?.nombre ?: "Proveedor", alCerrar) {
        val d = detalle
        when {
            d == null && error != null -> EstadoError(error!!) { error = null; recarga++ }
            d == null -> EstadoCargando("Cargando proveedor…")
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val p = d.proveedor
                NombreProveedor(p)
                if (p.verificado) Text("Proveedor verificado por VentanaGo", style = MaterialTheme.typography.bodySmall, color = Colores.azul)
                Estrellas(p.promedio, p.valoraciones)
                Text(experiencia(p), style = MaterialTheme.typography.bodyMedium, color = Colores.textoSecundario)
                p.descripcion?.takeIf { it.isNotBlank() }?.let { Text(it) }
                if (p.servicios.isNotEmpty()) {
                    Text("Servicios", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(p.servicios.joinToString(", ") { nombreServicio(it) })
                }
                if (p.comunas.isNotEmpty()) {
                    Text("Trabaja en", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(p.comunas.joinToString(", ") { it.nombre })
                }
                Text("Valoraciones", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (d.valoraciones.isEmpty()) Text("Aún no tiene valoraciones.", color = Colores.gris)
                d.valoraciones.forEach { v ->
                    Surface(color = Colores.fondo, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Colores.borde), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                for (i in 1..5) {
                                    Icon(if (i <= v.estrellas) Icons.Filled.Star else Icons.Filled.StarBorder, contentDescription = null, tint = amarillo, modifier = Modifier.size(14.dp))
                                }
                                Text("  ${v.autor.orEmpty()} · ${fechaLegible(v.fecha?.take(10))}", style = MaterialTheme.typography.bodySmall, color = Colores.gris)
                            }
                            v.comentario?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                }
            }
        }
    }
}

/** Diálogo a pantalla completa con barra superior y botón de cerrar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDialogo(titulo: String, alCerrar: () -> Unit, contenido: @Composable () -> Unit) {
    Dialog(onDismissRequest = alCerrar, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler { alCerrar() }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(titulo, maxLines = 1) },
                    navigationIcon = { IconButton(onClick = alCerrar) { Icon(Icons.Filled.Close, contentDescription = "Cerrar") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Colores.azul, titleContentColor = Color.White, navigationIconContentColor = Color.White),
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            Box(
                Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding()
                    .fillMaxSize(),
            ) { contenido() }
        }
    }
}

// ---------- Chat ----------

/**
 * Conversación entre el cliente y el proveedor de una oferta. Se revisan mensajes nuevos cada 8 segundos
 * mientras está abierta.
 */
@Composable
fun DialogoChat(ofertaId: Long, titulo: String, puedeEscribir: Boolean, alCerrar: () -> Unit) {
    val alcance = rememberCoroutineScope()
    var mensajes by remember { mutableStateOf<List<MensajeChat>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var texto by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    val lista = rememberLazyListState()

    LaunchedEffect(ofertaId) {
        while (true) {
            try {
                mensajes = ChatApi.mensajes(ofertaId)
                error = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.mensaje()
            }
            delay(8_000)
        }
    }
    LaunchedEffect(mensajes?.size) { mensajes?.let { if (it.isNotEmpty()) lista.animateScrollToItem(it.size - 1) } }

    PantallaDialogo(titulo, alCerrar) {
        Column(Modifier.fillMaxSize()) {
            error?.let { Box(Modifier.padding(12.dp)) { MensajeError(it) } }
            val m = mensajes
            Box(Modifier.weight(1f)) {
                when {
                    m == null -> EstadoCargando("Cargando conversación…")
                    m.isEmpty() -> Text(
                        "Aún no hay mensajes. Pregunta lo que necesites antes de decidir: plazos, garantía, si incluye retiro de la ventana antigua…",
                        color = Colores.gris,
                        modifier = Modifier.padding(24.dp),
                    )
                    else -> LazyColumn(state = lista, contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(m, key = { it.id }) { BurbujaMensaje(it) }
                    }
                }
            }
            if (puedeEscribir) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { texto = it.take(1000) },
                        placeholder = { Text("Escribe un mensaje") },
                        maxLines = 4,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {
                            val limpio = texto.trim()
                            if (limpio.isEmpty() || enviando) return@IconButton
                            alcance.launch {
                                enviando = true
                                try {
                                    val nuevo = ChatApi.enviar(ofertaId, limpio)
                                    mensajes = mensajes.orEmpty() + nuevo
                                    texto = ""
                                    error = null
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    error = e.mensaje()
                                }
                                enviando = false
                            }
                        },
                        enabled = !enviando && texto.isNotBlank(),
                    ) {
                        if (enviando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = Colores.azul)
                    }
                }
            } else {
                Text("Esta conversación está cerrada.", color = Colores.gris, modifier = Modifier.padding(12.dp))
            }
        }
    }
}

@Composable
private fun BurbujaMensaje(m: MensajeChat) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.mio) Arrangement.End else Arrangement.Start) {
        Surface(
            color = if (m.mio) Colores.azul else Color.White,
            contentColor = if (m.mio) Color.White else Colores.texto,
            shape = RoundedCornerShape(14.dp),
            border = if (m.mio) null else BorderStroke(1.dp, Colores.borde),
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (!m.mio) Text(m.autor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(m.texto)
                Text(
                    m.fecha?.let { "${fechaLegible(it.take(10))} ${it.drop(11).take(5)}" }.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (m.mio) Color(0xB3FFFFFF) else Colores.gris,
                )
            }
        }
    }
}

// ---------- Fotos ----------

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

// ---------- Comuna ----------

/** Región y luego comuna, como en el formulario de clientes. [alElegir] recibe la comuna (o null al cambiar de región). */
@Composable
fun SelectorComuna(comuna: Comuna?, alElegir: (Comuna?) -> Unit, error: String? = null) {
    var region by remember { mutableStateOf<Region?>(comuna?.region) }
    var regiones by remember { mutableStateOf<List<Region>?>(null) }
    var comunas by remember { mutableStateOf<List<Comuna>?>(null) }
    var errorCarga by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        regiones = try {
            Recursos.regiones()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorCarga = e.mensaje()
            emptyList()
        }
    }
    LaunchedEffect(region?.regionId) {
        val id = region?.regionId ?: return@LaunchedEffect
        comunas = null
        comunas = try {
            Recursos.comunas(id).sortedBy { it.nombre }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorCarga = e.mensaje()
            emptyList()
        }
    }
    errorCarga?.let { MensajeError(it) }
    Selector(
        "Región", regiones.orEmpty(), regiones?.find { it.regionId == region?.regionId } ?: region,
        texto = { it.nombre },
        alElegir = {
            if (it.regionId != region?.regionId) alElegir(null)
            region = it
        },
        cargando = regiones == null,
        error = error.takeIf { region == null },
    )
    Selector(
        "Comuna", comunas.orEmpty(), comunas?.find { it.comunaId == comuna?.comunaId } ?: comuna,
        texto = { it.nombre },
        alElegir = { alElegir(it) },
        habilitado = region != null,
        cargando = region != null && comunas == null,
        error = error.takeIf { region != null && comuna == null },
    )
}
