package cl.ventanago.app.ui.modulos.empresa

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cl.ventanago.app.datos.Cotizaciones
import cl.ventanago.app.datos.EmpresaApi
import cl.ventanago.app.datos.GANANCIA_REFERENCIAL
import cl.ventanago.app.datos.ItemVentana
import cl.ventanago.app.datos.Oferta
import cl.ventanago.app.datos.Recursos
import cl.ventanago.app.datos.RespuestaSolicitud
import cl.ventanago.app.datos.Solicitud
import cl.ventanago.app.datos.Ventana
import cl.ventanago.app.datos.factorVenta
import cl.ventanago.app.datos.mensaje
import cl.ventanago.app.datos.nombreServicio
import cl.ventanago.app.datos.pesos
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.comun.CampoTexto
import cl.ventanago.app.ui.comun.MensajeError
import cl.ventanago.app.ui.comun.Opciones
import cl.ventanago.app.ui.modulos.cliente.MiniVentana
import cl.ventanago.app.ui.modulos.cliente.detalleItem
import cl.ventanago.app.ui.modulos.cotizaciones.fechaLegible
import cl.ventanago.app.ui.modulos.mercado.DialogoChat
import cl.ventanago.app.ui.modulos.mercado.EtiquetaEstadoSolicitud
import cl.ventanago.app.ui.modulos.mercado.FotosSolicitud
import cl.ventanago.app.ui.modulos.mercado.NombreProveedor
import cl.ventanago.app.ui.modulos.soloDigitos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

// Mismos límites generales que el backend (SolicitudService) y la web.
private const val MIN_MM = 300
private const val MAX_ANCHO_MM = 9000
private const val MAX_ALTO_MM = 3000
private const val MAX_CANTIDAD = 99
private const val MAX_PRECIO = 100_000_000L
private const val MAX_PLAZO_DIAS = 365

/** Ventana en edición: los números como texto para poder borrar y reescribir el campo. */
private class Borrador(val original: ItemVentana) {
    var ancho by mutableStateOf(original.anchoMm.toString())
    var alto by mutableStateOf(original.altoMm.toString())
    var cantidad by mutableStateOf(original.cantidad.toString())
    var precio by mutableStateOf(original.precioUnitario?.toString().orEmpty())
}

private fun abrir(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Sin app de teléfono o correo: no se hace nada.
    }
}

private fun nombreEstadoOferta(o: Oferta) = when {
    o.tipo == "RECHAZADA" -> "No tomaste este trabajo"
    o.estado == "ELEGIDA" -> "¡El cliente eligió tu oferta!"
    o.estado == "DESCARTADA" -> "El cliente eligió otra oferta o canceló"
    else -> "Oferta enviada: esperando que el cliente decida"
}

/**
 * Detalle de una solicitud para el proveedor. Si aún no oferta, envía su oferta: acepta lo pedido con precio,
 * propone cambios (medidas, cantidades y precios) o no toma el trabajo. Después ve el estado de su oferta y
 * conversa con el cliente. [alCerrar] recibe la solicitud actualizada o null.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleSolicitudEmpresa(s: Solicitud, esAdmin: Boolean, alCerrar: (Solicitud?) -> Unit) {
    val context = LocalContext.current
    val alcance = rememberCoroutineScope()
    val puedeOfertar = s.estado == "PENDIENTE" && s.miOferta == null && !esAdmin
    val sinVentanas = s.items.isEmpty()

    var accion by remember { mutableStateOf("ACEPTADA") }
    val borradores = remember { s.items.map { Borrador(it) } }
    var totalServicio by remember { mutableStateOf("") }
    var plazo by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var enviando by remember { mutableStateOf(false) }
    var sugiriendo by remember { mutableStateOf(false) }
    var chat by remember { mutableStateOf<Oferta?>(null) }

    /** Precio sugerido = costo que calcula el backend (como en Crear cotización) con la ganancia de partida. */
    fun sugerirPrecios() = alcance.launch {
        sugiriendo = true
        error = null
        try {
            val colores = Recursos.colores.listar()
            val vidrios = Recursos.vidrios.listar()
            val sinDatos = coroutineScope {
                borradores.map { b ->
                    async {
                        val i = b.original
                        val color = colores.firstOrNull { it.colorId == i.colorId }
                        val vidrio = vidrios.firstOrNull { it.vidrioId == i.vidrioId }
                        val ancho = b.ancho.toIntOrNull()
                        val alto = b.alto.toIntOrNull()
                        if (i.pautaId == null || color == null || vidrio == null || ancho == null || alto == null) return@async true
                        val costo = Cotizaciones.cotizarVentana(
                            Ventana(cantidad = 1, ancho = ancho, alto = alto, color = color, vidrio = vidrio, pauta = EmpresaApi.pauta(i.pautaId)),
                        ).precioNeto
                        b.precio = (costo * factorVenta(GANANCIA_REFERENCIAL, 0)).roundToLong().toString()
                        false
                    }
                }.awaitAll().count { it }
            }
            if (sinDatos > 0) error = "$sinDatos ventana(s) no tienen pauta, color o vidrio del catálogo: ingresa su precio a mano."
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.mensaje()
        }
        sugiriendo = false
    }

    fun construir(): List<ItemVentana>? {
        val items = mutableListOf<ItemVentana>()
        for (b in borradores) {
            val ancho = b.ancho.toIntOrNull()?.takeIf { it in MIN_MM..MAX_ANCHO_MM }
                ?: run { error = "El ancho debe estar entre $MIN_MM y $MAX_ANCHO_MM mm."; return null }
            val alto = b.alto.toIntOrNull()?.takeIf { it in MIN_MM..MAX_ALTO_MM }
                ?: run { error = "El alto debe estar entre $MIN_MM y $MAX_ALTO_MM mm."; return null }
            val cantidad = b.cantidad.toIntOrNull()?.takeIf { it in 1..MAX_CANTIDAD }
                ?: run { error = "La cantidad debe estar entre 1 y $MAX_CANTIDAD."; return null }
            val precio = b.precio.toLongOrNull()?.takeIf { it in 1..MAX_PRECIO }
                ?: run { error = "Ingresa el precio unitario de todas las ventanas."; return null }
            items += b.original.copy(anchoMm = ancho, altoMm = alto, cantidad = cantidad, precioUnitario = precio)
        }
        return items
    }

    fun enviar() {
        error = null
        var items = s.items
        var total: Long? = null
        val plazoDias = plazo.toIntOrNull()
        if (accion != "RECHAZADA") {
            if (sinVentanas) {
                total = totalServicio.toLongOrNull()?.takeIf { it in 1..MAX_PRECIO }
                    ?: run { error = "Ingresa el precio total del trabajo."; return }
            } else {
                items = construir() ?: return
                val cambio = items.zip(s.items).any { (n, o) -> n.anchoMm != o.anchoMm || n.altoMm != o.altoMm || n.cantidad != o.cantidad }
                if (accion == "MODIFICADA" && !cambio) {
                    error = "No cambiaste medidas ni cantidades. Si estás de acuerdo con lo pedido, usa Aceptar."
                    return
                }
                if (accion == "ACEPTADA" && cambio) {
                    error = "Cambiaste medidas o cantidades: usa Modificar para informarlo al cliente."
                    return
                }
            }
            if (plazo.isNotBlank() && (plazoDias == null || plazoDias !in 1..MAX_PLAZO_DIAS)) {
                error = "El plazo debe estar entre 1 y $MAX_PLAZO_DIAS días."
                return
            }
        }
        if (accion != "ACEPTADA" && mensaje.isBlank()) {
            error = if (accion == "RECHAZADA") "Indica el motivo por el que no tomas el trabajo." else "Explica al cliente qué modificaste."
            return
        }
        alcance.launch {
            enviando = true
            try {
                val actualizada = EmpresaApi.responder(
                    s.numero,
                    RespuestaSolicitud(accion, mensaje.trim(), items, notificarEnApp = true, notificarPorCorreo = false, total = total, plazoDias = plazoDias),
                )
                alCerrar(actualizada)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.mensaje()
            }
            enviando = false
        }
    }

    // Total en vivo con lo que hay escrito.
    val total = when {
        accion == "RECHAZADA" -> null
        sinVentanas -> totalServicio.toLongOrNull()
        else -> borradores.takeIf { lista -> lista.all { it.precio.toLongOrNull() != null } }
            ?.sumOf { (it.precio.toLongOrNull() ?: 0) * (it.cantidad.toIntOrNull() ?: 0) }
    }

    Dialog(
        onDismissRequest = { if (!enviando) alCerrar(null) },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnClickOutside = false),
    ) {
        BackHandler(enabled = !enviando) { alCerrar(null) }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Solicitud N° ${s.numero}") },
                    navigationIcon = { IconButton(onClick = { alCerrar(null) }, enabled = !enviando) { Icon(Icons.Filled.Close, contentDescription = "Cerrar") } },
                    actions = { Row(Modifier.padding(end = 12.dp)) { if (esAdmin) EtiquetaEstadoSolicitud(s.estado) else EtiquetaOferta(s) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Colores.azul, titleContentColor = Color.White, navigationIconContentColor = Color.White),
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            val desplazamiento = rememberScrollState()
            LaunchedEffect(error) { if (error != null) desplazamiento.animateScrollTo(0) }
            Column(
                Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding()
                    .fillMaxSize()
                    .verticalScroll(desplazamiento)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                error?.let { MensajeError(it) }

                Bloque("Cliente") {
                    val c = s.contacto
                    Text(c?.nombre.orEmpty(), fontWeight = FontWeight.SemiBold)
                    s.comuna?.let { Text(listOfNotNull(it.nombre, it.region).joinToString(", "), style = MaterialTheme.typography.bodyMedium) }
                    val conContacto = !c?.email.isNullOrBlank()
                    if (conContacto) {
                        Text(listOfNotNull(c?.email, c?.telefono?.takeIf { it.isNotBlank() }).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = Colores.gris)
                        c?.direccion?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Colores.gris) }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = Colores.gris, modifier = Modifier.size(16.dp))
                            Text(
                                " Verás su contacto y dirección si elige tu oferta. Antes, conversa por el chat de tu oferta.",
                                style = MaterialTheme.typography.bodySmall, color = Colores.gris,
                            )
                        }
                    }
                    Text("Recibida el ${fechaLegible(s.fecha?.take(10))}", style = MaterialTheme.typography.bodySmall, color = Colores.gris)
                    Text("Servicios: ${s.servicios.joinToString(", ") { nombreServicio(it) }}", style = MaterialTheme.typography.bodyMedium)
                    s.observaciones?.takeIf { it.isNotBlank() }?.let {
                        Surface(color = Colores.azulClaro, shape = RoundedCornerShape(8.dp)) {
                            Text((if (sinVentanas) "Trabajo: " else "Comentarios: ") + it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                        }
                    }
                    FotosSolicitud(s.numero, s.fotos)
                    if (conContacto) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            c?.telefono?.takeIf { it.isNotBlank() }?.let { tel ->
                                OutlinedButton(onClick = { abrir(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel"))) }) {
                                    Icon(Icons.Outlined.Call, contentDescription = null)
                                    Text(" Llamar")
                                }
                            }
                            c?.email?.takeIf { it.isNotBlank() }?.let { mail ->
                                OutlinedButton(onClick = { abrir(context, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$mail"))) }) {
                                    Icon(Icons.Outlined.Email, contentDescription = null)
                                    Text(" Escribir")
                                }
                            }
                        }
                    }
                }

                // Oferta ya enviada (proveedor) u ofertas recibidas (administrador)
                s.miOferta?.let { o ->
                    Bloque("Tu oferta") {
                        Text(nombreEstadoOferta(o), fontWeight = FontWeight.SemiBold)
                        ResumenOferta(o)
                        if (o.tipo != "RECHAZADA") {
                            OutlinedButton(onClick = { chat = o }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null)
                                Text(if (o.mensajes > 0) " Chat con el cliente (${o.mensajes})" else " Chat con el cliente")
                            }
                        }
                    }
                }
                if (esAdmin) {
                    Bloque("Ofertas (${s.ofertas.orEmpty().size})") {
                        if (s.ofertas.isNullOrEmpty()) Text("Aún no hay ofertas.", color = Colores.gris)
                        s.ofertas.orEmpty().forEach { o ->
                            Surface(color = Colores.fondo, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Colores.borde), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    o.proveedor?.let { NombreProveedor(it) }
                                    Text(nombreEstadoOferta(o).removePrefix("¡").removeSuffix("!"), style = MaterialTheme.typography.bodySmall, color = Colores.gris)
                                    ResumenOferta(o)
                                }
                            }
                        }
                    }
                }

                if (!puedeOfertar) {
                    Bloque(if (sinVentanas) "Trabajo pedido" else "Ventanas pedidas") {
                        if (sinVentanas) Text(s.observaciones.orEmpty())
                        s.items.forEach { FilaItem(it) }
                    }
                } else {
                    Bloque("Tu oferta") {
                        Opciones(
                            "¿Qué harás con esta solicitud?",
                            if (sinVentanas) listOf("ACEPTADA" to "Ofertar", "RECHAZADA" to "No tomar")
                            else listOf("ACEPTADA" to "Aceptar", "MODIFICADA" to "Modificar", "RECHAZADA" to "No tomar"),
                            accion,
                        ) {
                            accion = it
                            error = null
                        }
                        Text(
                            when (accion) {
                                "ACEPTADA" -> if (sinVentanas) "Indica el precio total del trabajo." else "Define el precio unitario de cada ventana."
                                "MODIFICADA" -> "Ajusta medidas, cantidades y precios, y explica al cliente qué cambiaste."
                                else -> "La solicitud pasará a Cerradas. El cliente solo verá que no tomaste el trabajo."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Colores.gris,
                        )
                    }

                    if (!sinVentanas) {
                        Bloque("Ventanas") {
                            borradores.forEach { b ->
                                val i = b.original
                                Surface(color = Colores.fondo, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Colores.borde)) {
                                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        FilaItem(i)
                                        if (accion == "MODIFICADA") {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                CampoTexto("Ancho", b.ancho, { b.ancho = soloDigitos(it).take(5) }, Modifier.weight(1f), teclado = KeyboardType.Number, ayuda = "mm")
                                                CampoTexto("Alto", b.alto, { b.alto = soloDigitos(it).take(5) }, Modifier.weight(1f), teclado = KeyboardType.Number, ayuda = "mm")
                                                CampoTexto("Cant.", b.cantidad, { b.cantidad = soloDigitos(it).take(2) }, Modifier.weight(0.7f), teclado = KeyboardType.Number)
                                            }
                                        }
                                        if (accion != "RECHAZADA") {
                                            CampoTexto(
                                                "Precio unitario", b.precio, { b.precio = soloDigitos(it).take(9) },
                                                teclado = KeyboardType.Number, prefijo = "$ ",
                                                ayuda = b.precio.toLongOrNull()?.let { "${pesos(it)} c/u · ${pesos(it * (b.cantidad.toIntOrNull() ?: 0))} en total" },
                                            )
                                        }
                                    }
                                }
                            }
                            if (accion != "RECHAZADA") {
                                OutlinedButton(onClick = { sugerirPrecios() }, enabled = !sugiriendo && !enviando, modifier = Modifier.fillMaxWidth()) {
                                    if (sugiriendo) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                    else Icon(Icons.Outlined.Calculate, contentDescription = null)
                                    Text(" Calcular precios sugeridos")
                                }
                                Text(
                                    "Usa el mismo cálculo que Crear cotización (perfiles, quincallería y vidrio) con $GANANCIA_REFERENCIAL% de ganancia. Puedes ajustarlos.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Colores.gris,
                                )
                            }
                        }
                    }

                    Bloque("Condiciones y mensaje") {
                        if (accion != "RECHAZADA") {
                            if (sinVentanas) {
                                CampoTexto(
                                    "Precio total", totalServicio, { totalServicio = soloDigitos(it).take(9) },
                                    teclado = KeyboardType.Number, prefijo = "$ ", ayuda = totalServicio.toLongOrNull()?.let { pesos(it) },
                                )
                            }
                            CampoTexto(
                                "Plazo (opcional)", plazo, { plazo = soloDigitos(it).take(3) },
                                teclado = KeyboardType.Number, ayuda = "Días hábiles para terminar el trabajo",
                            )
                        }
                        CampoTexto(
                            when (accion) {
                                "ACEPTADA" -> "Mensaje para el cliente (opcional)"
                                "MODIFICADA" -> "Qué modificaste"
                                else -> "Motivo"
                            },
                            mensaje, { mensaje = it }, lineas = 3,
                            ayuda = if (accion == "ACEPTADA") "Ej: incluye retiro de la ventana antigua y garantía de 1 año." else null,
                        )
                    }

                    total?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Total", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(pesos(it), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Colores.azul)
                        }
                    }
                    Button(
                        onClick = { enviar() },
                        enabled = !enviando,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        if (enviando) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text(
                            when (accion) {
                                "ACEPTADA" -> "Enviar oferta"
                                "MODIFICADA" -> "Enviar propuesta"
                                else -> "No tomar este trabajo"
                            },
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    chat?.let { o ->
        DialogoChat(o.id, "Chat · Solicitud N° ${s.numero}", puedeEscribir = o.estado != "DESCARTADA" && s.estado != "CANCELADA") { chat = null }
    }
}

@Composable
private fun ResumenOferta(o: Oferta) {
    o.total?.let { Text("Total: ${pesos(it)}" + (o.plazoDias?.let { d -> " · $d días hábiles" } ?: ""), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Colores.menuFondo) }
    o.mensaje?.takeIf { it.isNotBlank() }?.let { Text("“$it”", style = MaterialTheme.typography.bodyMedium) }
    o.items.forEach { FilaItem(it) }
    Text("Enviada el ${fechaLegible(o.fecha?.take(10))}", style = MaterialTheme.typography.bodySmall, color = Colores.gris)
}

@Composable
private fun Bloque(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Colores.borde),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            contenido()
        }
    }
}

@Composable
private fun FilaItem(item: ItemVentana) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MiniVentana(item, Modifier.size(48.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text("${item.cantidad} × ${item.descripcion}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(detalleItem(item), style = MaterialTheme.typography.bodySmall, color = Colores.gris)
            item.observaciones?.takeIf { it.isNotBlank() }?.let { Text("Obs.: $it", style = MaterialTheme.typography.bodySmall, color = Colores.gris) }
        }
        item.precioUnitario?.let { Text(pesos(it), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold) }
    }
}
