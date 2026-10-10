package cl.ventanago.app.ui.modulos.cliente

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.ventanago.app.datos.BorradorSolicitud
import cl.ventanago.app.datos.CarritoLocal
import cl.ventanago.app.datos.ClienteApi
import cl.ventanago.app.datos.Contacto
import cl.ventanago.app.datos.ItemVentana
import cl.ventanago.app.datos.NuevaSolicitud
import cl.ventanago.app.datos.SERVICIOS
import cl.ventanago.app.datos.emailValido
import cl.ventanago.app.datos.mensaje
import cl.ventanago.app.datos.telefonoValido
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.comun.CampoTexto
import cl.ventanago.app.ui.comun.EstadoCargando
import cl.ventanago.app.ui.comun.EstadoError
import cl.ventanago.app.ui.comun.EstadoMensaje
import cl.ventanago.app.ui.comun.MensajeError
import cl.ventanago.app.ui.navegacion.Pantalla
import cl.ventanago.app.ventana.ConfigVentana
import cl.ventanago.app.ventana.DibujoVentana
import cl.ventanago.app.ventana.colorMarco
import cl.ventanago.app.ventana.colorVidrio
import cl.ventanago.app.ventana.modeloDeTextos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

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

/** Carrito (equivale a Carrito de la web): ajustar cantidades y enviar la solicitud de cotización a la empresa. */
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
    var validar by remember { mutableStateOf(false) }
    var enviada by remember { mutableStateOf<Long?>(null) }

    // Se guarda mientras se escribe, para no perderlo al entrar a la cuenta o cerrar la app.
    LaunchedEffect(nombre, email, telefono, direccion, servicios.toList(), observaciones) {
        if (enviada == null) {
            CarritoLocal.borrador = BorradorSolicitud(Contacto(nombre, email, telefono, direccion), servicios.toList(), observaciones)
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

    val errorNombre = "Ingresa tu nombre.".takeIf { nombre.isBlank() }
    val errorEmail = "Ingresa un correo válido.".takeIf { !emailValido(email) }
    val errorTelefono = "Ingresa un teléfono chileno válido.".takeIf { telefono.isNotBlank() && !telefonoValido(telefono) }
    val necesitaDireccion = servicios.any { it != "FABRICACION" }
    val errorDireccion = "Para instalación o flete necesitamos la dirección.".takeIf { necesitaDireccion && direccion.isBlank() }
    val errorServicios = "Elige al menos un servicio.".takeIf { servicios.isEmpty() }

    val lista = if (conCuenta) itemsCuenta else CarritoLocal.items
    // Cambios del carrito: en la cuenta (backend) o en el teléfono.
    fun cambiarCantidad(item: ItemVentana, cantidad: Int) = accion("Actualizando…") {
        if (conCuenta) { itemsCuenta = ClienteApi.cambiarCantidad(item.id!!, cantidad) } else { CarritoLocal.cambiarCantidad(item.id!!, cantidad) }
    }
    fun quitar(item: ItemVentana) = accion("Quitando…") {
        if (conCuenta) { itemsCuenta = ClienteApi.quitar(item.id!!) } else { CarritoLocal.quitar(item.id!!) }
    }
    when {
        lista == null && errorCarga != null -> EstadoError(errorCarga!!) { errorCarga = null; recarga++ }
        lista == null -> EstadoCargando("Cargando tu carrito…")
        lista.isEmpty() -> EstadoMensaje(
            Icons.Outlined.ShoppingCart,
            "Tu carrito está vacío",
            "Diseña una ventana y agrégala para pedir tu cotización.",
            "Diseñar ventana",
        ) { abrir(Pantalla.DISENAR_VENTANA) }
        else -> Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Tu carrito", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            val unidades = lista.sumOf { it.cantidad }
            Text("$unidades ${if (unidades == 1) "ventana" else "ventanas"}. La empresa te responderá con el precio.", color = Colores.gris)
            error?.let { MensajeError(it) }

            lista.forEach { item ->
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

            Card(colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Colores.borde), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pedir cotización", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Servicios", style = MaterialTheme.typography.labelLarge, color = if (validar && errorServicios != null) Colores.error else Colores.textoSecundario)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SERVICIOS.forEach { (valor, etiqueta) ->
                            val elegido = valor in servicios
                            FilterChip(
                                selected = elegido,
                                onClick = { if (elegido) servicios.remove(valor) else servicios.add(valor) },
                                label = { Text(etiqueta) },
                                leadingIcon = if (elegido) ({ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }) else null,
                            )
                        }
                    }
                    CampoTexto("Nombre", nombre, { nombre = it }, mayusculas = KeyboardCapitalization.Words, error = errorNombre.takeIf { validar })
                    CampoTexto("Correo", email, { email = it.trim() }, teclado = KeyboardType.Email, mayusculas = KeyboardCapitalization.None, error = errorEmail.takeIf { validar })
                    CampoTexto("Teléfono (opcional)", telefono, { telefono = it }, teclado = KeyboardType.Phone, error = errorTelefono.takeIf { validar }, ayuda = "Ej: +56 9 1234 5678")
                    CampoTexto(
                        if (necesitaDireccion) "Dirección" else "Dirección (opcional)", direccion, { direccion = it },
                        error = errorDireccion.takeIf { validar },
                    )
                    CampoTexto("Comentarios para la empresa (opcional)", observaciones, { observaciones = it }, lineas = 2)
                    Button(
                        onClick = {
                            validar = true
                            if (listOf(errorNombre, errorEmail, errorTelefono, errorDireccion, errorServicios).any { it != null }) {
                                error = "Revisa los datos marcados en rojo."
                                return@Button
                            }
                            if (!conCuenta) {
                                // Las ventanas y los datos ya están guardados en el teléfono: pasan a la cuenta al entrar.
                                alPedirIngreso("Para enviar tu solicitud, ingresa con tu correo. Tus ventanas y tus datos quedan guardados.")
                                return@Button
                            }
                            accion("Enviando…") {
                                val solicitud = ClienteApi.enviarSolicitud(
                                    NuevaSolicitud(
                                        Contacto(nombre.trim(), email.trim(), telefono.trim(), direccion.trim()),
                                        servicios.toList(),
                                        observaciones.trim(),
                                    ),
                                )
                                enviada = solicitud.numero
                                CarritoLocal.borrador = null
                                itemsCuenta = emptyList()
                            }
                        },
                        enabled = ocupado == null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        if (ocupado != null) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text(if (conCuenta) "Enviar solicitud" else "Ingresar y enviar solicitud")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    enviada?.let { numero ->
        AlertDialog(
            onDismissRequest = { enviada = null },
            title = { Text("¡Solicitud enviada!") },
            text = { Text("Tu solicitud N° $numero llegó a la empresa. Te avisaremos aquí cuando responda con el precio.") },
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
