package cl.ventanago.app.ui.modulos.cliente

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.ventanago.app.datos.Oferta
import cl.ventanago.app.datos.Solicitud
import cl.ventanago.app.datos.mensaje
import cl.ventanago.app.datos.nombreServicio
import cl.ventanago.app.datos.ofertaElegida
import cl.ventanago.app.datos.ofertasValidas
import cl.ventanago.app.datos.pesos
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.comun.CampoTexto
import cl.ventanago.app.ui.comun.DialogoConfirmar
import cl.ventanago.app.ui.comun.EstadoCargando
import cl.ventanago.app.ui.comun.EstadoError
import cl.ventanago.app.ui.comun.EstadoMensaje
import cl.ventanago.app.ui.comun.MensajeError
import cl.ventanago.app.ui.comun.MostrarAvisos
import cl.ventanago.app.ui.comun.escribirCorreo
import cl.ventanago.app.ui.comun.fechaLegible
import cl.ventanago.app.ui.comun.llamar
import cl.ventanago.app.ui.modulos.Etiqueta
import cl.ventanago.app.ui.modulos.mercado.DialogoChat
import cl.ventanago.app.ui.modulos.mercado.DialogoProveedor
import cl.ventanago.app.ui.modulos.mercado.Estrellas
import cl.ventanago.app.ui.modulos.mercado.EtiquetaEstadoSolicitud
import cl.ventanago.app.ui.modulos.mercado.FotosSolicitud
import cl.ventanago.app.ui.modulos.mercado.NombreProveedor
import cl.ventanago.app.ui.modulos.mercado.SelectorEstrellas
import cl.ventanago.app.ui.navegacion.Pantalla

/**
 * Mis cotizaciones: cada solicitud con las ofertas de los proveedores para compararlas, conversar con ellos y
 * elegir una. Con el trabajo hecho, el cliente valora al proveedor. [destacar]: solicitud que se abre desplegada
 * (la de una notificación que se tocó).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMisCotizaciones(abrir: (Pantalla) -> Unit, destacar: Long? = null, vm: MisCotizacionesViewModel = viewModel()) {
    MostrarAvisos(vm)
    val trabajando = vm.trabajando
    val lista = vm.solicitudes
    when {
        lista == null && vm.error != null -> EstadoError(vm.error!!) { vm.reintentar() }
        lista == null -> EstadoCargando("Cargando tus cotizaciones…")
        lista.isEmpty() -> EstadoMensaje(
            Icons.Outlined.History, "Aún no has pedido cotizaciones",
            "Cuando envíes tu carrito, los proveedores de tu zona te harán ofertas y las verás aquí.", "Diseñar ventana",
        ) { abrir(Pantalla.DISENAR_VENTANA) }
        else -> PullToRefreshBox(isRefreshing = vm.refrescando, onRefresh = { vm.cargar(manual = true) }, modifier = Modifier.fillMaxSize()) {
            val estadoLista = rememberLazyListState()
            LaunchedEffect(destacar, lista.size) {
                val i = lista.indexOfFirst { it.numero == destacar }
                if (i >= 0) estadoLista.animateScrollToItem(i + 1)
            }
            LazyColumn(state = estadoLista, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                item {
                    Text("Mis cotizaciones", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Compara las ofertas, conversa con los proveedores y elige la que más te convenga. Desliza hacia abajo para actualizar.", color = Colores.gris)
                }
                items(lista, key = { it.numero }) { s ->
                    TarjetaSolicitud(
                        s,
                        abiertaAlInicio = s.numero == destacar,
                        alChat = { o -> vm.chat = o to (o.estado != "DESCARTADA" && s.estado != "CANCELADA") },
                        alVerProveedor = { vm.proveedor = it },
                        alElegir = { o -> vm.porElegir = s to o },
                        alCancelar = { vm.porCancelar = s },
                        alValorar = { vm.porValorar = s },
                    )
                }
            }
        }
    }

    vm.chat?.let { (o, abierta) ->
        DialogoChat(o.id, o.proveedor?.nombre ?: "Proveedor", abierta) { vm.cerrarChat() }
    }
    vm.proveedor?.let { DialogoProveedor(it) { vm.proveedor = null } }
    vm.porElegir?.let { (s, o) ->
        AlertDialog(
            onDismissRequest = { if (!trabajando) vm.porElegir = null },
            title = { Text("¿Elegir a ${o.proveedor?.nombre ?: "este proveedor"}?") },
            text = {
                Text(
                    "Aceptarás su oferta de ${o.total?.let { pesos(it) } ?: "—"}. Le enviaremos tus datos de contacto para coordinar, " +
                        "y avisaremos a los demás proveedores que elegiste otra oferta.",
                )
            },
            confirmButton = {
                Button(onClick = { vm.elegir(s, o) }, enabled = !trabajando) {
                    if (trabajando) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp) else Text("Elegir oferta")
                }
            },
            dismissButton = { TextButton(onClick = { vm.porElegir = null }, enabled = !trabajando) { Text("Volver") } },
        )
    }
    vm.porCancelar?.let { s ->
        DialogoConfirmar(
            titulo = "¿Cancelar la solicitud N° ${s.numero}?",
            mensaje = "Los proveedores ya no podrán ofertar y avisaremos a quienes te enviaron ofertas.",
            textoConfirmar = "Cancelar solicitud",
            procesando = trabajando,
            alConfirmar = { vm.cancelar(s) },
            alCancelar = { if (!trabajando) vm.porCancelar = null },
        )
    }
    vm.porValorar?.let { s ->
        DialogoValorar(s, trabajando, alCerrar = { if (!trabajando) vm.porValorar = null }) { estrellas, comentario -> vm.valorar(s, estrellas, comentario) }
    }
}

@Composable
private fun TarjetaSolicitud(
    s: Solicitud,
    abiertaAlInicio: Boolean,
    alChat: (Oferta) -> Unit,
    alVerProveedor: (Long) -> Unit,
    alElegir: (Oferta) -> Unit,
    alCancelar: () -> Unit,
    alValorar: () -> Unit,
) {
    val context = LocalContext.current
    var abierta by remember(abiertaAlInicio) { mutableStateOf(abiertaAlInicio) }
    val validas = s.ofertasValidas().sortedBy { it.total ?: Long.MAX_VALUE }
    val elegida = s.ofertaElegida()
    val masBarata = validas.firstOrNull()?.total

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Colores.borde),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Solicitud N° ${s.numero}", fontWeight = FontWeight.Bold)
                    Text(
                        listOfNotNull(fechaLegible(s.fecha?.take(10)), s.comuna?.nombre, s.servicios.joinToString(", ") { nombreServicio(it) })
                            .filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = Colores.gris,
                    )
                }
                EtiquetaEstadoSolicitud(s.estado)
            }

            // Resumen de lo pedido, desplegable
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (s.items.isEmpty()) "Servicio sin ventanas" else "${s.items.sumOf { it.cantidad }} ventana(s) · ${s.items.size} tipo(s)",
                    style = MaterialTheme.typography.bodyMedium, color = Colores.textoSecundario, modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { abierta = !abierta }) {
                    Text(if (abierta) "Ocultar" else "Ver lo pedido")
                    Icon(if (abierta) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
                }
            }
            AnimatedVisibility(abierta) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    s.items.forEach { FilaItem(it) }
                    s.observaciones?.takeIf { it.isNotBlank() }?.let {
                        Text(if (s.items.isEmpty()) it else "Tus comentarios: $it", style = MaterialTheme.typography.bodySmall, color = Colores.textoSecundario)
                    }
                    FotosSolicitud(s.numero, s.fotos)
                }
            }

            when (s.estado) {
                "PENDIENTE" -> {
                    if (validas.isEmpty()) {
                        Surface(color = Colores.fondo, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "Esperando ofertas. Te avisaremos cuando un proveedor responda.",
                                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(10.dp),
                            )
                        }
                    } else {
                        Text(
                            "${validas.size} ${if (validas.size == 1) "oferta" else "ofertas"} · de menor a mayor precio",
                            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                        )
                        validas.forEach { o ->
                            TarjetaOferta(
                                o, s, destacada = validas.size > 1 && o.total == masBarata,
                                alChat = { alChat(o) }, alVerProveedor = alVerProveedor, alElegir = { alElegir(o) },
                            )
                        }
                    }
                    val noTomaron = s.ofertas.orEmpty().count { it.tipo == "RECHAZADA" }
                    if (noTomaron > 0) {
                        Text("$noTomaron proveedor(es) no pueden tomar este trabajo.", style = MaterialTheme.typography.bodySmall, color = Colores.gris)
                    }
                    TextButton(onClick = alCancelar) { Text("Cancelar solicitud", color = Colores.error) }
                }
                "ADJUDICADA", "TERMINADA" -> elegida?.let { o ->
                    TarjetaOferta(o, s, destacada = false, alChat = { alChat(o) }, alVerProveedor = alVerProveedor, alElegir = null)
                    o.contactoProveedor?.let { c ->
                        Surface(color = Colores.azulClaro, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Contacto de ${c.nombre}", fontWeight = FontWeight.SemiBold)
                                Text(listOf(c.email, c.telefono).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (c.telefono.isNotBlank()) {
                                        OutlinedButton(onClick = { context.llamar(c.telefono) }) {
                                            Icon(Icons.Outlined.Call, contentDescription = null)
                                            Text(" Llamar")
                                        }
                                    }
                                    OutlinedButton(onClick = { context.escribirCorreo(c.email) }) {
                                        Icon(Icons.Outlined.Email, contentDescription = null)
                                        Text(" Escribir")
                                    }
                                }
                            }
                        }
                    }
                    if (s.estado == "ADJUDICADA") {
                        Button(onClick = alValorar, modifier = Modifier.fillMaxWidth()) { Text("El trabajo está listo: valorar") }
                        TextButton(onClick = alCancelar) { Text("Cancelar solicitud", color = Colores.error) }
                    }
                    s.valoracion?.let { v ->
                        Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(10.dp)) {
                                Text("Tu valoración: ${"★".repeat(v.estrellas)}${"☆".repeat(5 - v.estrellas)}", fontWeight = FontWeight.SemiBold)
                                v.comentario?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                }
                "CANCELADA" -> Text("Cancelaste esta solicitud.", style = MaterialTheme.typography.bodyMedium, color = Colores.gris)
            }
        }
    }
}

/** Una oferta para comparar: proveedor con su nota, total, plazo y las ventanas con su precio. */
@Composable
private fun TarjetaOferta(
    o: Oferta,
    s: Solicitud,
    destacada: Boolean,
    alChat: () -> Unit,
    alVerProveedor: (Long) -> Unit,
    alElegir: (() -> Unit)?,
) {
    var detalle by remember { mutableStateOf(false) }
    Surface(
        color = if (o.estado == "ELEGIDA") Color(0xFFF1F8F1) else Colores.fondo,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(if (destacada) 2.dp else 1.dp, if (destacada) Colores.exito else Colores.borde),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            o.proveedor?.let { p ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NombreProveedor(p, Modifier.weight(1f))
                    if (destacada) Etiqueta("Mejor precio", Color(0xFFE8F5E9), Colores.exito)
                    if (o.estado == "ELEGIDA") Etiqueta("Elegida", Color(0xFFE8F5E9), Colores.exito)
                }
                Estrellas(p.promedio, p.valoraciones)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(o.total?.let { pesos(it) } ?: "—", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Colores.menuFondo)
                Spacer(Modifier.width(8.dp))
                o.plazoDias?.let { Text("en $it días hábiles", style = MaterialTheme.typography.bodySmall, color = Colores.textoSecundario, modifier = Modifier.padding(bottom = 4.dp)) }
            }
            if (o.tipo == "MODIFICADA") {
                Etiqueta("Propone cambios en medidas o cantidades", Colores.azulClaro, Colores.azul)
            }
            o.mensaje?.takeIf { it.isNotBlank() }?.let { Text("“$it”", style = MaterialTheme.typography.bodyMedium) }
            if (o.items.isNotEmpty()) {
                TextButton(onClick = { detalle = !detalle }, contentPadding = PaddingValues(0.dp)) {
                    Text(if (detalle) "Ocultar detalle" else "Ver detalle por ventana")
                }
                AnimatedVisibility(detalle) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        o.items.forEach { item ->
                            val original = s.items.firstOrNull { it.id == item.id }
                            FilaItem(item)
                            if (original != null && (original.anchoMm != item.anchoMm || original.altoMm != item.altoMm || original.cantidad != item.cantidad)) {
                                Text(
                                    "Pediste ${original.cantidad} × ${original.anchoMm} × ${original.altoMm} mm",
                                    style = MaterialTheme.typography.bodySmall, color = Colores.azul,
                                )
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = alChat) {
                    Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(if (o.mensajes > 0) " Chat (${o.mensajes})" else " Preguntar")
                }
                o.proveedor?.let { p -> TextButton(onClick = { alVerProveedor(p.cuentaId) }) { Text("Ver perfil") } }
                Spacer(Modifier.weight(1f))
                alElegir?.let { Button(onClick = it) { Text("Elegir") } }
            }
        }
    }
}

@Composable
private fun DialogoValorar(s: Solicitud, enviando: Boolean, alCerrar: () -> Unit, alEnviar: (Int, String) -> Unit) {
    var estrellas by remember { mutableIntStateOf(0) }
    var comentario by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("¿Cómo te fue con ${s.ofertaElegida()?.proveedor?.nombre ?: "el proveedor"}?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tu valoración ayuda a otros clientes a elegir. La solicitud quedará terminada.", style = MaterialTheme.typography.bodySmall, color = Colores.gris)
                error?.let { MensajeError(it) }
                SelectorEstrellas(estrellas) { estrellas = it; error = null }
                CampoTexto("Comentario (opcional)", comentario, { comentario = it.take(1000) }, lineas = 3)
            }
        },
        confirmButton = {
            Button(onClick = { if (estrellas == 0) error = "Elige de 1 a 5 estrellas." else alEnviar(estrellas, comentario.trim()) }, enabled = !enviando) {
                if (enviando) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp) else Text("Enviar valoración")
            }
        },
        dismissButton = { TextButton(onClick = alCerrar, enabled = !enviando) { Text("Más tarde") } },
    )
}
