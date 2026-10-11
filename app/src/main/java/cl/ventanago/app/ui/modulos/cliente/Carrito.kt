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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.ventanago.app.datos.ItemVentana
import cl.ventanago.app.datos.PrecioReferencia
import cl.ventanago.app.datos.pesos
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.comun.Bloque
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
import cl.ventanago.app.ui.navegacion.Pantalla

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
@Composable
fun PantallaCarrito(
    emailCuenta: String,
    abrir: (Pantalla) -> Unit,
    /** false: sin cuenta, el carrito vive en el teléfono y al enviar se pide entrar. */
    conCuenta: Boolean = true,
    alPedirIngreso: (String) -> Unit = {},
    vm: CarritoViewModel = viewModel { CarritoViewModel(conCuenta, emailCuenta) },
) {
    val lista = vm.lista
    when {
        lista == null && vm.errorCarga != null -> EstadoError(vm.errorCarga!!) { vm.cargar() }
        lista == null -> EstadoCargando("Cargando tu carrito…")
        !vm.conVentanas && !vm.sinVentanas -> CarritoVacio(abrir, vm::pedirServicio)
        else -> FormularioCarrito(vm, lista, abrir) {
            if (!vm.enviar()) alPedirIngreso("Para enviar tu solicitud, ingresa con tu correo. Tus ventanas y tus datos quedan guardados.")
        }
    }

    vm.enviada?.let { numero ->
        AlertDialog(
            onDismissRequest = { vm.enviada = null },
            title = { Text("¡Solicitud enviada!") },
            text = { Text("Tu solicitud N° $numero llegó a los proveedores. Te avisaremos cuando lleguen ofertas para que las compares.") },
            confirmButton = {
                Button(onClick = {
                    vm.enviada = null
                    abrir(Pantalla.MIS_COTIZACIONES)
                }) { Text("Ver mis cotizaciones") }
            },
            dismissButton = { TextButton(onClick = { vm.enviada = null }) { Text("Cerrar") } },
        )
    }
}

@Composable
private fun CarritoVacio(abrir: (Pantalla) -> Unit, alPedirServicio: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            EstadoMensaje(
                Icons.Outlined.ShoppingCart,
                "Tu carrito está vacío",
                "Diseña una ventana y agrégala para pedir tu cotización.",
                "Diseñar ventana",
            ) { abrir(Pantalla.DISENAR_VENTANA) }
        }
        OutlinedButton(
            onClick = alPedirServicio,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Icon(Icons.Outlined.Handyman, contentDescription = null)
            Text(" Pedir una reparación u otro servicio")
        }
    }
}

@Composable
private fun FormularioCarrito(vm: CarritoViewModel, lista: List<ItemVentana>, abrir: (Pantalla) -> Unit, alEnviar: () -> Unit) {
    var proveedorAbierto by remember { mutableStateOf<Long?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (vm.modoServicio) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pedir un servicio", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { vm.sinVentanas = false }) { Text("Volver") }
            }
            Text("Cuéntanos qué necesitas y los proveedores de tu zona te enviarán sus ofertas.", color = Colores.gris)
        } else {
            Text("Tu carrito", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            val unidades = lista.sumOf { it.cantidad }
            Text("$unidades ${if (unidades == 1) "ventana" else "ventanas"}. Los proveedores de tu zona te enviarán sus ofertas.", color = Colores.gris)
        }
        vm.error?.let { MensajeError(it) }

        if (!vm.modoServicio) VentanasDelCarrito(vm, lista, abrir)
        SeccionServicios(vm)
        Bloque("¿Dónde es el trabajo?") {
            SelectorComuna(vm.comuna, vm::elegirComuna, error = vm.errorComuna.takeIf { vm.validar })
            CampoTexto(
                if (vm.necesitaDireccion) "Dirección" else "Dirección (opcional)", vm.direccion, { vm.direccion = it },
                error = vm.errorDireccion.takeIf { vm.validar },
                ayuda = "Solo la ve el proveedor que elijas.",
            )
        }
        if (vm.comuna != null) SeccionDestinatarios(vm) { proveedorAbierto = it }
        Bloque("Tus datos") {
            CampoTexto("Nombre", vm.nombre, { vm.nombre = it }, mayusculas = KeyboardCapitalization.Words, error = vm.errorNombre.takeIf { vm.validar })
            CampoTexto("Correo", vm.email, { vm.email = it.trim() }, teclado = KeyboardType.Email, mayusculas = KeyboardCapitalization.None, error = vm.errorEmail.takeIf { vm.validar })
            CampoTexto("Teléfono (opcional)", vm.telefono, { vm.telefono = it }, teclado = KeyboardType.Phone, error = vm.errorTelefono.takeIf { vm.validar }, ayuda = "Ej: +56 9 1234 5678")
            Text(
                "Los proveedores solo ven tu nombre y comuna. Tus datos de contacto los recibe el proveedor que elijas.",
                style = MaterialTheme.typography.bodySmall,
                color = Colores.gris,
            )
        }

        val ocupado = vm.ocupado
        Button(
            onClick = alEnviar,
            enabled = ocupado == null,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
        ) {
            if (ocupado != null) {
                CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                Text("  $ocupado")
            } else {
                Text(if (vm.conCuenta) "Enviar solicitud" else "Ingresar y enviar solicitud")
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    proveedorAbierto?.let { id -> DialogoProveedor(id) { proveedorAbierto = null } }
}

@Composable
private fun VentanasDelCarrito(vm: CarritoViewModel, lista: List<ItemVentana>, abrir: (Pantalla) -> Unit) {
    val libre = vm.ocupado == null
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
                        IconButton(onClick = { vm.cambiarCantidad(item, item.cantidad - 1) }, enabled = item.cantidad > 1 && libre) {
                            Icon(Icons.Filled.Remove, contentDescription = "Disminuir cantidad")
                        }
                        Text("${item.cantidad}", fontWeight = FontWeight.Bold)
                        IconButton(onClick = { vm.cambiarCantidad(item, item.cantidad + 1) }, enabled = item.cantidad < 99 && libre) {
                            Icon(Icons.Filled.Add, contentDescription = "Aumentar cantidad")
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { vm.quitar(item) }, enabled = libre) {
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
    rangoReferencial(lista, vm.precioReferencia)?.let { (desde, hasta) ->
        Surface(color = Colores.azulClaro, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Precio referencial: entre ${pesos(desde)} y ${pesos(hasta)}", fontWeight = FontWeight.SemiBold, color = Colores.menuFondo)
                Text(
                    "Según el precio por m² de ${vm.precioReferencia?.muestras} ventanas cotizadas en VentanaGo. El precio real lo dan las ofertas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Colores.textoSecundario,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeccionServicios(vm: CarritoViewModel) {
    val context = LocalContext.current
    val elegirFotos = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_FOTOS)) { uris ->
        if (uris.isNotEmpty()) vm.agregarFotos(context, uris)
    }
    Bloque(if (vm.modoServicio) "¿Qué necesitas?" else "Pedir cotización") {
        Text("Servicios", style = MaterialTheme.typography.labelLarge, color = if (vm.validar && vm.errorServicios != null) Colores.error else Colores.textoSecundario)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            vm.serviciosVisibles.forEach { (valor, etiqueta) ->
                val elegido = valor in vm.servicios
                FilterChip(
                    selected = elegido,
                    onClick = { vm.alternarServicio(valor) },
                    label = { Text(etiqueta) },
                    leadingIcon = if (elegido) ({ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }) else null,
                )
            }
        }
        CampoTexto(
            if (vm.modoServicio) "Describe el trabajo" else "Comentarios para los proveedores (opcional)",
            vm.observaciones, { vm.observaciones = it }, lineas = if (vm.modoServicio) 4 else 2,
            error = vm.errorDescripcion.takeIf { vm.validar },
            ayuda = if (vm.modoServicio) "Ej: la ventana corredera del living no corre y entra aire." else null,
        )
        if (vm.conCuenta) {
            Text("Fotos del lugar (opcional)", style = MaterialTheme.typography.labelLarge, color = Colores.textoSecundario)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                vm.fotos.forEachIndexed { i, (_, img) ->
                    Box(Modifier.size(72.dp)) {
                        Image(img, contentDescription = "Foto ${i + 1}", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)))
                        IconButton(
                            onClick = { vm.fotos.removeAt(i) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(26.dp)
                                .background(Color(0x99000000), CircleShape),
                        ) { Icon(Icons.Filled.Close, contentDescription = "Quitar foto", tint = Color.White, modifier = Modifier.size(16.dp)) }
                    }
                }
                if (vm.fotos.size < MAX_FOTOS) {
                    OutlinedButton(
                        onClick = { elegirFotos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        enabled = vm.ocupado == null,
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
}

@Composable
private fun SeccionDestinatarios(vm: CarritoViewModel, alVerProveedor: (Long) -> Unit) {
    val comuna = vm.comuna?.nombre
    Bloque("¿A quién se la envías?") {
        Opciones("", listOf(false to "Todos los de mi zona", true to "Elegir proveedores"), vm.elegirProveedores) { vm.elegirProveedores = it }
        val aptos = vm.proveedoresAptos
        when {
            aptos == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("  Buscando proveedores en $comuna…", color = Colores.gris)
            }
            aptos.isEmpty() -> Text(
                "Aún no hay proveedores registrados para $comuna con esos servicios. Igual puedes enviarla: llegará a los proveedores que atienden todas las zonas.",
                style = MaterialTheme.typography.bodySmall,
                color = Colores.gris,
            )
            !vm.elegirProveedores -> Text(
                "Tu solicitud llegará a ${aptos.size} ${if (aptos.size == 1) "proveedor" else "proveedores"} de $comuna. Compararás sus ofertas y elegirás una.",
                style = MaterialTheme.typography.bodyMedium,
            )
            else -> {
                vm.errorProveedores?.takeIf { vm.validar }?.let { Text(it, color = Colores.error, style = MaterialTheme.typography.bodySmall) }
                aptos.forEach { p ->
                    val marcado = p.cuentaId in vm.elegidos
                    Surface(
                        onClick = { alVerProveedor(p.cuentaId) },
                        color = if (marcado) Colores.azulClaro else Colores.fondo,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Colores.borde),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(start = 4.dp, end = 10.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = marcado, onCheckedChange = { vm.marcarProveedor(p.cuentaId, it) })
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
