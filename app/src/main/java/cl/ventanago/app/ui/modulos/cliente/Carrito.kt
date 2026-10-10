package cl.ventanago.app.ui.modulos.cliente

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.ventanago.app.datos.BorradorSolicitud
import cl.ventanago.app.datos.CarritoLocal
import cl.ventanago.app.datos.ClienteApi
import cl.ventanago.app.datos.Comuna
import cl.ventanago.app.datos.Contacto
import cl.ventanago.app.datos.FotoNueva
import cl.ventanago.app.datos.ItemVentana
import cl.ventanago.app.datos.NuevaSolicitud
import cl.ventanago.app.datos.PrecioReferencia
import cl.ventanago.app.datos.ProveedorResumen
import cl.ventanago.app.datos.ProveedoresApi
import cl.ventanago.app.datos.SERVICIOS
import cl.ventanago.app.datos.emailValido
import cl.ventanago.app.datos.mensaje
import cl.ventanago.app.datos.pesos
import cl.ventanago.app.datos.telefonoValido
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.comun.CampoTexto
import cl.ventanago.app.ui.comun.EstadoCargando
import cl.ventanago.app.ui.comun.EstadoError
import cl.ventanago.app.ui.comun.EstadoMensaje
import cl.ventanago.app.ui.comun.MensajeError
import cl.ventanago.app.ui.comun.Opciones
import cl.ventanago.app.ui.modulos.mercado.DialogoProveedor
import cl.ventanago.app.ui.modulos.mercado.Estrellas
import cl.ventanago.app.ui.modulos.mercado.NombreProveedor
import cl.ventanago.app.ui.modulos.mercado.SelectorComuna
import cl.ventanago.app.ui.modulos.mercado.comprimirFoto
import cl.ventanago.app.ui.navegacion.Pantalla
import cl.ventanago.app.ventana.ConfigVentana
import cl.ventanago.app.ventana.DibujoVentana
import cl.ventanago.app.ventana.colorMarco
import cl.ventanago.app.ventana.colorVidrio
import cl.ventanago.app.ventana.modeloDeTextos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val MAX_FOTOS = 4
private const val MAX_PROVEEDORES = 10

/** Dibujo pequeño de una ventana del carrito o de una solicitud. */
@Composable
fun MiniVentana(item: ItemVentana, modifier: Modifier = Modifier) {
    val config = remember(item) {
        ConfigVentana(
            anchoMm = item.anchoMm.coerceAtLeast(300), altoMm = item.altoMm.coerceAtLeast(300), hojas = item.hojas,
            modelo = modeloDeTextos(item.imagenPauta, item.descripcion),
            colorMarco = colorMarco(item.colorNombre), colorVidrio = colorVidrio(item.vidrioNombre),
        )
    }
    Box(modifier.background(Color(0xFFEFF2F6), RoundedCornerShape(8.dp)).padding(4.dp)) {
        DibujoVentana(config, Modifier.fillMaxSize())
    }
}

fun detalleItem(item: ItemVentana) =
    "${item.anchoMm} × ${item.altoMm} mm · ${item.colorNombre} · ${item.vidrioNombre}" + (item.serieNombre?.let { " · Serie $it" } ?: "")

/** Rango referencial del total según el precio por m² de las ofertas que ya hay en VentanaGo. */
private fun rangoReferencial(items: List<ItemVentana>, ref: PrecioReferencia?): Pair<Long, Long>? {
    val p25 = ref?.p25PorM2 ?: return null
    val p75 = ref.p75PorM2 ?: return null
    val m2 = items.sumOf { it.anchoMm / 1000.0 * it.altoMm / 1000.0 * it.cantidad }
    if (m2 <= 0) return null
    // Redondeado a miles para que se lea como referencia y no como precio exacto.
    fun miles(v: Double) = Math.round(v / 1000) * 1000
    return miles(p25 * m2) to miles(p75 * m2)
}

/**
 * Carrito: ajustar cantidades y pedir cotización a los proveedores de la comuna (o a los que elija el cliente).
 * Sin ventanas, el cliente puede pedir un servicio describiéndolo (reparación, cambio de vidrio, visita técnica…).
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PantallaCarrito(
    emailCuenta: String,
    abrir: (Pantalla) -> Unit,
    /** false: sin cuenta, el carrito vive en el teléfono y al enviar se pide entrar. */
    conCuenta: Boolean = true,
    alPedirIngreso: (String) -> Unit = {},
) {
    val alcance = rememberCoroutineScope()
    val context = LocalContext.current
    var itemsCuenta by remember { mutableStateOf<List<ItemVentana>?>(null) }
    var errorCarga by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableIntStateOf(0) }
    var ocupado by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    // Datos del formulario: parten del borrador guardado (si se escribieron antes de entrar).
    val borrador = remember { CarritoLocal.borrador }
    var nombre by remember { mutableStateOf(borrador?.contacto?.nombre.orEmpty()) }
    var email by remember { mutableStateOf(borrador?.contacto?.email?.takeIf { it.isNotBlank() } ?: emailCuenta) }
    var telefono by remember { mutableStateOf(borrador?.contacto?.telefono.orEmpty()) }
    var direccion by remember { mutableStateOf(borrador?.contacto?.direccion.orEmpty()) }
    val servicios = remember { mutableStateListOf(*(borrador?.servicios ?: listOf("FABRICACION")).toTypedArray()) }
    var observaciones by remember { mutableStateOf(borrador?.observaciones.orEmpty()) }
    var comuna by remember { mutableStateOf(borrador?.comuna) }
    var elegirProveedores by remember { mutableStateOf(borrador?.proveedores?.isNotEmpty() == true) }
    val elegidos = remember { mutableStateListOf(*(borrador?.proveedores ?: emptyList()).toTypedArray()) }
    var sinVentanas by remember { mutableStateOf(borrador?.sinVentanas == true) }
    val fotos = remember { mutableStateListOf<Pair<FotoNueva, ImageBitmap>>() }
    var validar by remember { mutableStateOf(false) }
    var enviada by remember { mutableStateOf<Long?>(null) }

    var proveedoresZona by remember { mutableStateOf<List<ProveedorResumen>?>(null) }
    var proveedorAbierto by remember { mutableStateOf<Long?>(null) }
    var precioReferencia by remember { mutableStateOf<PrecioReferencia?>(null) }

    // Se guarda mientras se escribe, para no perderlo al entrar a la cuenta o cerrar la app.
    LaunchedEffect(nombre, email, telefono, direccion, servicios.toList(), observaciones, comuna, elegidos.toList(), elegirProveedores, sinVentanas) {
        if (enviada == null) {
            CarritoLocal.borrador = BorradorSolicitud(
                Contacto(nombre, email, telefono, direccion), servicios.toList(), observaciones, comuna,
                if (elegirProveedores) elegidos.toList() else emptyList(), sinVentanas,
            )
        }
    }

    LaunchedEffect(recarga) {
        if (!conCuenta) return@LaunchedEffect
        try {
            itemsCuenta = ClienteApi.carrito()
            errorCarga = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorCarga = e.mensaje()
        }
    }
    // Los datos de contacto se recuerdan de la última solicitud, como en la web.
    LaunchedEffect(Unit) {
        if (!conCuenta) return@LaunchedEffect
        try {
            ClienteApi.solicitudes().maxByOrNull { it.numero }?.contacto?.let { c ->
                if (nombre.isBlank()) nombre = c.nombre
                if (telefono.isBlank()) telefono = c.telefono
                if (direccion.isBlank()) direccion = c.direccion
                if (c.email.isNotBlank()) email = c.email
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Sin solicitudes previas o sin conexión: se completan a mano.
        }
    }
    LaunchedEffect(Unit) {
        precioReferencia = try {
            ProveedoresApi.precioReferencia()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
    // Proveedores que atienden la comuna: para contar a cuántos llega la solicitud o elegir algunos.
    LaunchedEffect(comuna?.comunaId) {
        val id = comuna?.comunaId ?: run { proveedoresZona = null; return@LaunchedEffect }
        proveedoresZona = null
        proveedoresZona = try {
            ProveedoresApi.listar(comunaId = id)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    val elegirFotos = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_FOTOS)) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        alcance.launch {
            ocupado = "Preparando fotos…"
            var fallidas = 0
            for (uri in uris.take(MAX_FOTOS - fotos.size)) {
                comprimirFoto(context, uri)?.let { fotos += it } ?: fallidas++
            }
            if (fallidas > 0) error = "No se pudieron leer $fallidas foto(s)."
            ocupado = null
        }
    }

    fun accion(texto: String, bloque: suspend () -> Unit) {
        if (ocupado != null) return
        alcance.launch {
            ocupado = texto
            error = null
            try {
                bloque()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.mensaje()
            }
            ocupado = null
        }
    }

    val lista = if (conCuenta) itemsCuenta else CarritoLocal.items
    val conVentanas = !lista.isNullOrEmpty()
    // Con ventanas en el carrito el formulario es para cotizarlas; sin ventanas, para pedir un servicio.
    val modoServicio = !conVentanas && sinVentanas
    val serviciosVisibles = if (modoServicio) SERVICIOS.filter { it.first != "FABRICACION" } else SERVICIOS
    val serviciosElegidos = servicios.filter { s -> serviciosVisibles.any { it.first == s } }
    val proveedoresAptos = proveedoresZona?.filter { p -> p.servicios.isEmpty() || serviciosElegidos.any { it in p.servicios } }

    val errorNombre = "Ingresa tu nombre.".takeIf { nombre.isBlank() }
    val errorEmail = "Ingresa un correo válido.".takeIf { !emailValido(email) }
    val errorTelefono = "Ingresa un teléfono chileno válido.".takeIf { telefono.isNotBlank() && !telefonoValido(telefono) }
    val necesitaDireccion = serviciosElegidos.any { it != "FABRICACION" }
    val errorDireccion = "Indica dónde se hará el trabajo.".takeIf { necesitaDireccion && direccion.isBlank() }
    val errorServicios = "Elige al menos un servicio.".takeIf { serviciosElegidos.isEmpty() }
    val errorComuna = "Elige la comuna donde se hará el trabajo.".takeIf { comuna == null }
    val errorDescripcion = "Describe el trabajo (al menos 10 caracteres).".takeIf { modoServicio && observaciones.trim().length < 10 }
    val errorProveedores = "Elige al menos un proveedor, o envíala a todos los de tu zona.".takeIf { elegirProveedores && elegidos.isEmpty() }

    // Cambios del carrito: en la cuenta (backend) o en el teléfono.
    fun cambiarCantidad(item: ItemVentana, cantidad: Int) = accion("Actualizando…") {
        if (conCuenta) { itemsCuenta = ClienteApi.cambiarCantidad(item.id!!, cantidad) } else { CarritoLocal.cambiarCantidad(item.id!!, cantidad) }
    }
    fun quitar(item: ItemVentana) = accion("Quitando…") {
        if (conCuenta) { itemsCuenta = ClienteApi.quitar(item.id!!) } else { CarritoLocal.quitar(item.id!!) }
    }

    fun enviar() {
        validar = true
        if (listOf(errorNombre, errorEmail, errorTelefono, errorDireccion, errorServicios, errorComuna, errorDescripcion, errorProveedores).any { it != null }) {
            error = "Revisa los datos marcados en rojo."
            return
        }
        if (!conCuenta) {
            // Las ventanas y los datos ya están guardados en el teléfono: pasan a la cuenta al entrar.
            alPedirIngreso("Para enviar tu solicitud, ingresa con tu correo. Tus ventanas y tus datos quedan guardados.")
            return
        }
        accion("Enviando…") {
            val solicitud = ClienteApi.enviarSolicitud(
                NuevaSolicitud(
                    Contacto(nombre.trim(), email.trim(), telefono.trim(), direccion.trim()),
                    serviciosElegidos,
                    observaciones.trim(),
                    comuna?.comunaId,
                    if (elegirProveedores) elegidos.toList() else emptyList(),
                    fotos.map { it.first },
                ),
            )
            enviada = solicitud.numero
            CarritoLocal.borrador = null
            itemsCuenta = emptyList()
            sinVentanas = false
            fotos.clear()
        }
    }

    when {
        lista == null && errorCarga != null -> EstadoError(errorCarga!!) { errorCarga = null; recarga++ }
        lista == null -> EstadoCargando("Cargando tu carrito…")
        !conVentanas && !sinVentanas -> Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                EstadoMensaje(
                    Icons.Outlined.ShoppingCart,
                    "Tu carrito está vacío",
                    "Diseña una ventana y agrégala para pedir tu cotización.",
                    "Diseñar ventana",
                ) { abrir(Pantalla.DISENAR_VENTANA) }
            }
            OutlinedButton(
                onClick = {
                    sinVentanas = true
                    servicios.remove("FABRICACION")
                    if (servicios.isEmpty()) servicios += "REPARACION"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Icon(Icons.Outlined.Handyman, contentDescription = null)
                Text(" Pedir una reparación u otro servicio")
            }
        }
        else -> Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (modoServicio) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pedir un servicio", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { sinVentanas = false }) { Text("Volver") }
                }
                Text("Cuéntanos qué necesitas y los proveedores de tu zona te enviarán sus ofertas.", color = Colores.gris)
            } else {
                Text("Tu carrito", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                val unidades = lista!!.sumOf { it.cantidad }
                Text("$unidades ${if (unidades == 1) "ventana" else "ventanas"}. Los proveedores de tu zona te enviarán sus ofertas.", color = Colores.gris)
            }
            error?.let { MensajeError(it) }

            if (!modoServicio) {
                lista!!.forEach { item ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Colores.borde), shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            MiniVentana(item, Modifier.size(64.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.descripcion, fontWeight = FontWeight.SemiBold)
                                Text(detalleItem(item), style = MaterialTheme.typography.bodySmall, color = Colores.gris)
                                item.observaciones?.takeIf { it.isNotBlank() }?.let { Text("Obs.: $it", style = MaterialTheme.typography.bodySmall, color = Colores.gris) }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { cambiarCantidad(item, item.cantidad - 1) },
                                        enabled = item.cantidad > 1 && ocupado == null,
                                    ) { Icon(Icons.Filled.Remove, contentDescription = "Disminuir cantidad") }
                                    Text("${item.cantidad}", fontWeight = FontWeight.Bold)
                                    IconButton(
                                        onClick = { cambiarCantidad(item, item.cantidad + 1) },
                                        enabled = item.cantidad < 99 && ocupado == null,
                                    ) { Icon(Icons.Filled.Add, contentDescription = "Aumentar cantidad") }
                                    Spacer(Modifier.weight(1f))
                                    IconButton(onClick = { quitar(item) }, enabled = ocupado == null) {
                                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Quitar del carrito", tint = Colores.error)
                                    }
                                }
                            }
                        }
                    }
                }
                TextButton(onClick = { abrir(Pantalla.DISENAR_VENTANA) }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(" Agregar otra ventana")
                }
                rangoReferencial(lista!!, precioReferencia)?.let { (desde, hasta) ->
                    Surface(color = Colores.azulClaro, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Precio referencial: entre ${pesos(desde)} y ${pesos(hasta)}", fontWeight = FontWeight.SemiBold, color = Colores.menuFondo)
                            Text(
                                "Según el precio por m² de ${precioReferencia?.muestras} ventanas cotizadas en VentanaGo. El precio real lo dan las ofertas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Colores.textoSecundario,
                            )
                        }
                    }
                }
            }

            Bloque(if (modoServicio) "¿Qué necesitas?" else "Pedir cotización") {
                Text("Servicios", style = MaterialTheme.typography.labelLarge, color = if (validar && errorServicios != null) Colores.error else Colores.textoSecundario)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    serviciosVisibles.forEach { (valor, etiqueta) ->
                        val elegido = valor in servicios
                        FilterChip(
                            selected = elegido,
                            onClick = { if (elegido) servicios.remove(valor) else servicios.add(valor) },
                            label = { Text(etiqueta) },
                            leadingIcon = if (elegido) ({ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }) else null,
                        )
                    }
                }
                CampoTexto(
                    if (modoServicio) "Describe el trabajo" else "Comentarios para los proveedores (opcional)",
                    observaciones, { observaciones = it }, lineas = if (modoServicio) 4 else 2,
                    error = errorDescripcion.takeIf { validar },
                    ayuda = if (modoServicio) "Ej: la ventana corredera del living no corre y entra aire." else null,
                )
                if (conCuenta) {
                    Text("Fotos del lugar (opcional)", style = MaterialTheme.typography.labelLarge, color = Colores.textoSecundario)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        fotos.forEachIndexed { i, (_, img) ->
                            Box(Modifier.size(72.dp)) {
                                Image(img, contentDescription = "Foto ${i + 1}", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)))
                                IconButton(
                                    onClick = { fotos.removeAt(i) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(26.dp)
                                        .background(Color(0x99000000), CircleShape),
                                ) { Icon(Icons.Filled.Close, contentDescription = "Quitar foto", tint = Color.White, modifier = Modifier.size(16.dp)) }
                            }
                        }
                        if (fotos.size < MAX_FOTOS) {
                            OutlinedButton(
                                onClick = { elegirFotos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                enabled = ocupado == null,
                                modifier = Modifier.height(72.dp),
                            ) {
                                Icon(Icons.Outlined.AddAPhoto, contentDescription = null)
                                Text(" Agregar")
                            }
                        }
                    }
                    Text("Ayudan a los proveedores a cotizar sin visitar. Hasta $MAX_FOTOS.", style = MaterialTheme.typography.bodySmall, color = Colores.gris)
                }
            }

            Bloque("¿Dónde es el trabajo?") {
                SelectorComuna(comuna, { nueva: Comuna? ->
                    comuna = nueva
                    elegidos.clear()
                }, error = errorComuna.takeIf { validar })
                CampoTexto(
                    if (necesitaDireccion) "Dirección" else "Dirección (opcional)", direccion, { direccion = it },
                    error = errorDireccion.takeIf { validar },
                    ayuda = "Solo la ve el proveedor que elijas.",
                )
            }

            if (comuna != null) {
                Bloque("¿A quién se la envías?") {
                    Opciones(
                        "",
                        listOf(false to "Todos los de mi zona", true to "Elegir proveedores"),
                        elegirProveedores,
                    ) { elegirProveedores = it }
                    val aptos = proveedoresAptos
                    when {
                        aptos == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("  Buscando proveedores en ${comuna?.nombre}…", color = Colores.gris)
                        }
                        aptos.isEmpty() -> Text(
                            "Aún no hay proveedores registrados para ${comuna?.nombre} con esos servicios. Igual puedes enviarla: llegará a los proveedores que atienden todas las zonas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Colores.gris,
                        )
                        !elegirProveedores -> Text(
                            "Tu solicitud llegará a ${aptos.size} ${if (aptos.size == 1) "proveedor" else "proveedores"} de ${comuna?.nombre}. Compararás sus ofertas y elegirás una.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        else -> {
                            if (validar && errorProveedores != null) Text(errorProveedores, color = Colores.error, style = MaterialTheme.typography.bodySmall)
                            aptos.forEach { p ->
                                val marcado = p.cuentaId in elegidos
                                Surface(
                                    onClick = { proveedorAbierto = p.cuentaId },
                                    color = if (marcado) Colores.azulClaro else Colores.fondo,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Colores.borde),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(Modifier.padding(start = 4.dp, end = 10.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = marcado,
                                            onCheckedChange = { si -> if (si) { if (elegidos.size < MAX_PROVEEDORES) elegidos += p.cuentaId } else elegidos -= p.cuentaId },
                                        )
                                        Column(Modifier.weight(1f)) {
                                            NombreProveedor(p)
                                            Estrellas(p.promedio, p.valoraciones)
                                        }
                                        Text("Ver ›", color = Colores.azul, style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Bloque("Tus datos") {
                CampoTexto("Nombre", nombre, { nombre = it }, mayusculas = KeyboardCapitalization.Words, error = errorNombre.takeIf { validar })
                CampoTexto("Correo", email, { email = it.trim() }, teclado = KeyboardType.Email, mayusculas = KeyboardCapitalization.None, error = errorEmail.takeIf { validar })
                CampoTexto("Teléfono (opcional)", telefono, { telefono = it }, teclado = KeyboardType.Phone, error = errorTelefono.takeIf { validar }, ayuda = "Ej: +56 9 1234 5678")
                Text(
                    "Los proveedores solo ven tu nombre y comuna. Tus datos de contacto los recibe el proveedor que elijas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Colores.gris,
                )
            }

            Button(
                onClick = { enviar() },
                enabled = ocupado == null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                if (ocupado != null) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    Text("  $ocupado")
                } else {
                    Text(if (conCuenta) "Enviar solicitud" else "Ingresar y enviar solicitud")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    proveedorAbierto?.let { id -> DialogoProveedor(id) { proveedorAbierto = null } }

    enviada?.let { numero ->
        AlertDialog(
            onDismissRequest = { enviada = null },
            title = { Text("¡Solicitud enviada!") },
            text = { Text("Tu solicitud N° $numero llegó a los proveedores. Te avisaremos aquí cuando lleguen ofertas para que las compares.") },
            confirmButton = {
                Button(onClick = {
                    enviada = null
                    abrir(Pantalla.MIS_COTIZACIONES)
                }) { Text("Ver mis cotizaciones") }
            },
            dismissButton = { TextButton(onClick = { enviada = null }) { Text("Cerrar") } },
        )
    }
}

@Composable
private fun Bloque(titulo: String, contenido: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Colores.borde), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            contenido()
        }
    }
}
