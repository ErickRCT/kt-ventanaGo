package cl.ventanago.app.ui.modulos.empresa

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Button
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.ventanago.app.datos.GANANCIA_REFERENCIAL
import cl.ventanago.app.datos.Oferta
import cl.ventanago.app.datos.Solicitud
import cl.ventanago.app.datos.nombreServicio
import cl.ventanago.app.datos.pesos
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.comun.Bloque
import cl.ventanago.app.ui.comun.CampoTexto
import cl.ventanago.app.ui.comun.EstadoCargando
import cl.ventanago.app.ui.comun.EstadoError
import cl.ventanago.app.ui.comun.EstadoMensaje
import cl.ventanago.app.ui.comun.MensajeError
import cl.ventanago.app.ui.comun.Opciones
import cl.ventanago.app.ui.comun.escribirCorreo
import cl.ventanago.app.ui.comun.fechaLegible
import cl.ventanago.app.ui.comun.llamar
import cl.ventanago.app.ui.modulos.cliente.FilaItem
import cl.ventanago.app.ui.modulos.mercado.DialogoChat
import cl.ventanago.app.ui.modulos.mercado.EtiquetaEstadoSolicitud
import cl.ventanago.app.ui.modulos.mercado.FotosSolicitud
import cl.ventanago.app.ui.modulos.mercado.NombreProveedor
import cl.ventanago.app.ui.modulos.soloDigitos

private fun nombreEstadoOferta(o: Oferta) = when {
    o.tipo == "RECHAZADA" -> "No tomaste este trabajo"
    o.estado == "ELEGIDA" -> "¡El cliente eligió tu oferta!"
    o.estado == "DESCARTADA" -> "El cliente eligió otra oferta o canceló"
    else -> "Oferta enviada: esperando que el cliente decida"
}

/**
 * Detalle de una solicitud para el proveedor (destino "solicitud/{numero}" de la navegación; también se llega
 * tocando una notificación). Si aún no oferta, envía su oferta; después ve el estado de su oferta y conversa
 * con el cliente. [empresa] es el modelo que comparte con la lista de solicitudes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalleSolicitudEmpresa(numero: Long, esAdmin: Boolean, empresa: SolicitudesEmpresaViewModel, alVolver: () -> Unit) {
    // Al llegar desde una notificación la lista puede no estar cargada (o no traer aún la solicitud nueva).
    var buscada by rememberSaveable { mutableStateOf(empresa.solicitud(numero) != null) }
    LaunchedEffect(numero) {
        if (empresa.solicitud(numero) == null) empresa.recargar()
        buscada = true
    }
    val s = empresa.solicitud(numero)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Solicitud N° $numero") },
                navigationIcon = { IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
                actions = { if (s != null) Row(Modifier.padding(end = 12.dp)) { if (esAdmin) EtiquetaEstadoSolicitud(s.estado) else EtiquetaOferta(s) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Colores.barra, titleContentColor = Colores.sobreBarra, navigationIconContentColor = Colores.sobreBarra, actionIconContentColor = Colores.sobreBarra),
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
        ) {
            when {
                s != null -> {
                    val oferta: OfertaViewModel = viewModel(key = "oferta-$numero") { OfertaViewModel(s) }
                    ContenidoDetalle(s, esAdmin, oferta) { actualizada ->
                        empresa.reemplazar(actualizada)
                        alVolver()
                    }
                }
                empresa.solicitudes == null && empresa.error != null -> EstadoError(empresa.error!!) { empresa.reintentar() }
                empresa.solicitudes == null || !buscada -> EstadoCargando("Cargando solicitud…")
                else -> EstadoMensaje(Icons.Outlined.Inbox, "No encontramos la solicitud N° $numero", "Puede que el cliente la haya enviado a otros proveedores.")
            }
        }
    }
}

@Composable
private fun ContenidoDetalle(s: Solicitud, esAdmin: Boolean, vm: OfertaViewModel, alEnviar: (Solicitud) -> Unit) {
    val puedeOfertar = s.estado == "PENDIENTE" && s.miOferta == null && !esAdmin
    val desplazamiento = rememberScrollState()
    LaunchedEffect(vm.error) { if (vm.error != null) desplazamiento.animateScrollTo(0) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(desplazamiento)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        vm.error?.let { MensajeError(it) }
        BloqueCliente(s)

        // Oferta ya enviada (proveedor) u ofertas recibidas (administrador)
        s.miOferta?.let { o ->
            Bloque("Tu oferta", espacio = 8.dp) {
                Text(nombreEstadoOferta(o), fontWeight = FontWeight.SemiBold)
                ResumenOferta(o)
                if (o.tipo != "RECHAZADA") {
                    OutlinedButton(onClick = { vm.chat = o }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null)
                        Text(if (o.mensajes > 0) " Chat con el cliente (${o.mensajes})" else " Chat con el cliente")
                    }
                }
            }
        }
        if (esAdmin) {
            Bloque("Ofertas (${s.ofertas.orEmpty().size})", espacio = 8.dp) {
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

        if (puedeOfertar) {
            FormularioOferta(vm) { vm.enviar(alEnviar) }
        } else {
            Bloque(if (s.items.isEmpty()) "Trabajo pedido" else "Ventanas pedidas", espacio = 8.dp) {
                if (s.items.isEmpty()) Text(s.observaciones.orEmpty())
                s.items.forEach { FilaItem(it, tamano = 48.dp, conObservaciones = true, sufijoPrecio = "", enCm = false) }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    vm.chat?.let { o ->
        DialogoChat(o.id, "Chat · Solicitud N° ${s.numero}", puedeEscribir = o.estado != "DESCARTADA" && s.estado != "CANCELADA") { vm.chat = null }
    }
}

@Composable
private fun BloqueCliente(s: Solicitud) {
    val context = LocalContext.current
    val sinVentanas = s.items.isEmpty()
    Bloque("Cliente", espacio = 8.dp) {
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
                    OutlinedButton(onClick = { context.llamar(tel) }) {
                        Icon(Icons.Outlined.Call, contentDescription = null)
                        Text(" Llamar")
                    }
                }
                c?.email?.takeIf { it.isNotBlank() }?.let { mail ->
                    OutlinedButton(onClick = { context.escribirCorreo(mail) }) {
                        Icon(Icons.Outlined.Email, contentDescription = null)
                        Text(" Escribir")
                    }
                }
            }
        }
    }
}

@Composable
private fun FormularioOferta(vm: OfertaViewModel, alEnviar: () -> Unit) {
    val accion = vm.accion
    val sinVentanas = vm.sinVentanas
    Bloque("Tu oferta", espacio = 8.dp) {
        Opciones(
            "¿Qué harás con esta solicitud?",
            if (sinVentanas) listOf("ACEPTADA" to "Ofertar", "RECHAZADA" to "No tomar")
            else listOf("ACEPTADA" to "Aceptar", "MODIFICADA" to "Modificar", "RECHAZADA" to "No tomar"),
            accion,
            vm::elegirAccion,
        )
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
        Bloque("Ventanas", espacio = 8.dp) {
            vm.borradores.forEach { b ->
                Surface(color = Colores.fondo, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Colores.borde)) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilaItem(b.original, tamano = 48.dp, conObservaciones = true, sufijoPrecio = "", enCm = false)
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
                OutlinedButton(onClick = vm::sugerirPrecios, enabled = !vm.sugiriendo && !vm.enviando, modifier = Modifier.fillMaxWidth()) {
                    if (vm.sugiriendo) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
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

    Bloque("Condiciones y mensaje", espacio = 8.dp) {
        if (accion != "RECHAZADA") {
            if (sinVentanas) {
                CampoTexto(
                    "Precio total", vm.totalServicio, { vm.totalServicio = soloDigitos(it).take(9) },
                    teclado = KeyboardType.Number, prefijo = "$ ", ayuda = vm.totalServicio.toLongOrNull()?.let { pesos(it) },
                )
            }
            CampoTexto(
                "Plazo (opcional)", vm.plazo, { vm.plazo = soloDigitos(it).take(3) },
                teclado = KeyboardType.Number, ayuda = "Días hábiles para terminar el trabajo",
            )
        }
        CampoTexto(
            when (accion) {
                "ACEPTADA" -> "Mensaje para el cliente (opcional)"
                "MODIFICADA" -> "Qué modificaste"
                else -> "Motivo"
            },
            vm.mensaje, { vm.mensaje = it }, lineas = 3,
            ayuda = if (accion == "ACEPTADA") "Ej: incluye retiro de la ventana antigua y garantía de 1 año." else null,
        )
    }

    vm.total?.let {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Total", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(pesos(it), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Colores.azul)
        }
    }
    Button(
        onClick = alEnviar,
        enabled = !vm.enviando,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
    ) {
        if (vm.enviando) CircularProgressIndicator(Modifier.size(22.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
        else Text(
            when (accion) {
                "ACEPTADA" -> "Enviar oferta"
                "MODIFICADA" -> "Enviar propuesta"
                else -> "No tomar este trabajo"
            },
        )
    }
}

@Composable
private fun ResumenOferta(o: Oferta) {
    o.total?.let { Text("Total: ${pesos(it)}" + (o.plazoDias?.let { d -> " · $d días hábiles" } ?: ""), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Colores.textoFuerte) }
    o.mensaje?.takeIf { it.isNotBlank() }?.let { Text("“$it”", style = MaterialTheme.typography.bodyMedium) }
    o.items.forEach { FilaItem(it, tamano = 48.dp, conObservaciones = true, sufijoPrecio = "", enCm = false) }
    Text("Enviada el ${fechaLegible(o.fecha?.take(10))}", style = MaterialTheme.typography.bodySmall, color = Colores.gris)
}
