package cl.ventanago.app.ui.modulos.cliente

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.ventanago.app.datos.Oferta
import cl.ventanago.app.datos.Solicitud
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
import cl.ventanago.app.ventana.medidasCm

/** Cómo ordenar las ofertas para compararlas. */
private enum class Orden(val texto: String) { PRECIO("Menor precio"), PLAZO("Menor plazo"), VALORACION("Mejor valorados") }

/** Sellos de una oferta frente a las demás (solo con más de una oferta). */
private class Sellos(val masBarata: Boolean, val masRapida: Boolean, val mejorValorada: Boolean)

/**
 * Detalle de una solicitud del cliente: en qué paso va, lo que pidió y, mientras recibe ofertas, la comparación
 * para elegir una (ordenable por precio, plazo o valoración). Con un proveedor elegido muestra su contacto y,
 * al terminar, la valoración. Usa el mismo modelo que la lista de Mis cotizaciones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalleCotizacion(numero: Long, vm: MisCotizacionesViewModel) {
    MostrarAvisos(vm)
    val lista = vm.solicitudes
    val s = lista?.firstOrNull { it.numero == numero }
    when {
        lista == null && vm.error != null -> return EstadoError(vm.error!!) { vm.reintentar() }
        lista == null -> return EstadoCargando("Cargando tu solicitud…")
        s == null -> return EstadoMensaje(Icons.Outlined.SearchOff, "No encontramos la solicitud N° $numero", "Puede que ya no esté disponible.")
    }
    s!!
    var orden by rememberSaveable { mutableStateOf(Orden.PRECIO) }

    PullToRefreshBox(isRefreshing = vm.refrescando, onRefresh = { vm.cargar(manual = true) }, modifier = Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
            item(key = "resumen") { Resumen(s) }
            item(key = "pedido") { Pedido(s) }
            when (s.estado) {
                "PENDIENTE" -> ofertas(s, vm, orden) { orden = it }
                "ADJUDICADA", "TERMINADA" -> item(key = "elegida") { Elegida(s, vm) }
                "CANCELADA" -> item(key = "cancelada") {
                    Text("Cancelaste esta solicitud: los proveedores ya no pueden ofertar.", style = MaterialTheme.typography.bodyMedium, color = Colores.gris)
                }
            }
        }
    }
    Dialogos(vm)
}

@Composable
private fun Bloque(titulo: String? = null, contenido: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Colores.superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            titulo?.let { Text(it, style = MaterialTheme.typography.titleMedium, color = Colores.textoFuerte) }
            contenido()
        }
    }
}

@Composable
private fun Resumen(s: Solicitud) = Bloque {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            listOfNotNull(fechaLegible(s.fecha?.take(10)), s.comuna?.nombre).filter { it.isNotBlank() }.joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium, color = Colores.textoSecundario, modifier = Modifier.weight(1f),
        )
        EtiquetaEstadoSolicitud(s.estado)
    }
    Spacer(Modifier.size(4.dp))
    Seguimiento(s)
}

@Composable
private fun Pedido(s: Solicitud) = Bloque("Lo que pediste") {
    if (s.servicios.isNotEmpty()) {
        Text(s.servicios.joinToString(" · ") { nombreServicio(it) }, style = MaterialTheme.typography.bodySmall, color = Colores.gris)
    }
    s.items.forEach { FilaItem(it) }
    s.observaciones?.takeIf { it.isNotBlank() }?.let {
        Text(if (s.items.isEmpty()) it else "Tus comentarios: $it", style = MaterialTheme.typography.bodyMedium, color = Colores.textoSecundario)
    }
    FotosSolicitud(s.numero, s.fotos)
}

/** Ofertas recibidas para comparar, o la espera si aún no llegan. */
private fun androidx.compose.foundation.lazy.LazyListScope.ofertas(s: Solicitud, vm: MisCotizacionesViewModel, orden: Orden, alOrdenar: (Orden) -> Unit) {
    val validas = s.ofertasValidas()
    val noTomaron = s.ofertas.orEmpty().count { it.tipo == "RECHAZADA" }
    if (validas.isEmpty()) {
        item(key = "esperando") {
            Bloque {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.HourglassEmpty, contentDescription = null, tint = Colores.azul)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Esperando ofertas", style = MaterialTheme.typography.titleMedium, color = Colores.textoFuerte)
                        Text("Tu solicitud llegó a los proveedores de tu zona. Te avisaremos apenas uno responda.", style = MaterialTheme.typography.bodyMedium, color = Colores.textoSecundario)
                    }
                }
                if (noTomaron > 0) Text(textoNoTomaron(noTomaron), style = MaterialTheme.typography.bodySmall, color = Colores.gris)
            }
        }
    } else {
        item(key = "comparar") { CabeceraOfertas(validas.size, noTomaron, orden, alOrdenar) }
        items(ordenadas(validas, orden), key = { "oferta-${it.id}" }) { o ->
            TarjetaOferta(
                o, s, sellosDe(o, validas),
                alChat = { vm.chat = o to (o.estado != "DESCARTADA") },
                alVerProveedor = { vm.proveedor = it },
                alElegir = { vm.porElegir = s to o },
            )
        }
    }
    item(key = "cancelar") {
        TextButton(onClick = { vm.porCancelar = s }) { Text("Cancelar solicitud", color = Colores.error) }
    }
}

private fun textoNoTomaron(n: Int) = if (n == 1) "1 proveedor no puede tomar este trabajo." else "$n proveedores no pueden tomar este trabajo."

@Composable
private fun CabeceraOfertas(cantidad: Int, noTomaron: Int, orden: Orden, alOrdenar: (Orden) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "$cantidad ${if (cantidad == 1) "oferta" else "ofertas"} para comparar",
            style = MaterialTheme.typography.titleMedium, color = Colores.textoFuerte,
        )
        if (cantidad > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Orden.entries) { o ->
                    FilterChip(
                        selected = orden == o,
                        onClick = { alOrdenar(o) },
                        label = { Text(o.texto) },
                        leadingIcon = if (orden == o) ({ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }) else null,
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Colores.azulClaro, selectedLabelColor = Colores.textoFuerte),
                    )
                }
            }
        }
        if (noTomaron > 0) Text(textoNoTomaron(noTomaron), style = MaterialTheme.typography.bodySmall, color = Colores.gris)
    }
}

private fun ordenadas(ofertas: List<Oferta>, orden: Orden): List<Oferta> = when (orden) {
    Orden.PRECIO -> ofertas.sortedBy { it.total ?: Long.MAX_VALUE }
    Orden.PLAZO -> ofertas.sortedWith(compareBy<Oferta> { it.plazoDias ?: Int.MAX_VALUE }.thenBy { it.total ?: Long.MAX_VALUE })
    Orden.VALORACION -> ofertas.sortedWith(
        compareByDescending<Oferta> { it.proveedor?.promedio ?: 0.0 }.thenByDescending { it.proveedor?.valoraciones ?: 0 }.thenBy { it.total ?: Long.MAX_VALUE },
    )
}

private fun sellosDe(o: Oferta, todas: List<Oferta>): Sellos {
    if (todas.size < 2) return Sellos(false, false, false)
    val menorPrecio = todas.mapNotNull { it.total }.minOrNull()
    val menorPlazo = todas.mapNotNull { it.plazoDias }.minOrNull()
    val mejorNota = todas.mapNotNull { p -> p.proveedor?.takeIf { it.valoraciones > 0 }?.promedio }.maxOrNull()
    return Sellos(
        masBarata = o.total != null && o.total == menorPrecio,
        masRapida = o.plazoDias != null && o.plazoDias == menorPlazo,
        mejorValorada = mejorNota != null && o.proveedor?.valoraciones?.let { it > 0 } == true && o.proveedor.promedio == mejorNota,
    )
}

/** Una oferta para comparar: proveedor con su nota, total, plazo, sellos y las ventanas con su precio. */
@Composable
private fun TarjetaOferta(
    o: Oferta,
    s: Solicitud,
    sellos: Sellos,
    alChat: () -> Unit,
    alVerProveedor: (Long) -> Unit,
    alElegir: (() -> Unit)?,
) {
    var detalle by remember { mutableStateOf(false) }
    val destacada = sellos.masBarata || o.estado == "ELEGIDA"
    Card(
        colors = CardDefaults.cardColors(containerColor = if (o.estado == "ELEGIDA") Colores.exitoFondo else Colores.superficie),
        border = BorderStroke(if (destacada) 2.dp else 1.dp, if (destacada) Colores.exito else Colores.borde),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (sellos.masBarata || sellos.masRapida || sellos.mejorValorada || o.estado == "ELEGIDA") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (o.estado == "ELEGIDA") Etiqueta("Elegida", Colores.exitoFondo, Colores.exito)
                    if (sellos.masBarata) Etiqueta("Más barata", Colores.exitoFondo, Colores.exito)
                    if (sellos.masRapida) Etiqueta("Más rápida", Colores.azulClaro, Colores.azul)
                    if (sellos.mejorValorada) Etiqueta("Mejor valorada", Colores.avisoFondo, Colores.aviso)
                }
            }
            o.proveedor?.let { p ->
                NombreProveedor(p)
                Estrellas(p.promedio, p.valoraciones)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(o.total?.let { pesos(it) } ?: "—", style = MaterialTheme.typography.headlineSmall, color = Colores.textoFuerte)
                Spacer(Modifier.width(8.dp))
                o.plazoDias?.let {
                    Text(
                        "en $it ${if (it == 1) "día hábil" else "días hábiles"}",
                        style = MaterialTheme.typography.bodyMedium, color = Colores.textoSecundario, modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
            }
            if (o.tipo == "MODIFICADA") Etiqueta("Propone cambios en medidas o cantidades", Colores.azulClaro, Colores.azul)
            o.mensaje?.takeIf { it.isNotBlank() }?.let { Text("“$it”", style = MaterialTheme.typography.bodyMedium, color = Colores.texto) }
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
                                    "Pediste ${original.cantidad} × ${medidasCm(original.anchoMm, original.altoMm)}",
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

/** Con un proveedor elegido: su oferta, su contacto y, según el estado, valorar o la valoración que se dejó. */
@Composable
private fun Elegida(s: Solicitud, vm: MisCotizacionesViewModel) {
    val context = LocalContext.current
    val o = s.ofertaElegida() ?: return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Proveedor elegido", style = MaterialTheme.typography.titleMedium, color = Colores.textoFuerte)
        TarjetaOferta(
            o, s, Sellos(false, false, false),
            alChat = { vm.chat = o to (s.estado != "CANCELADA") },
            alVerProveedor = { vm.proveedor = it },
            alElegir = null,
        )
        o.contactoProveedor?.let { c ->
            Bloque("Contacto de ${c.nombre}") {
                Text(listOf(c.email, c.telefono).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = Colores.textoSecundario)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (c.telefono.isNotBlank()) {
                        Button(onClick = { context.llamar(c.telefono) }) {
                            Icon(Icons.Outlined.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(" Llamar")
                        }
                    }
                    OutlinedButton(onClick = { context.escribirCorreo(c.email) }) {
                        Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Escribir")
                    }
                }
            }
        }
        if (s.estado == "ADJUDICADA") {
            Button(onClick = { vm.porValorar = s }, modifier = Modifier.fillMaxWidth()) { Text("El trabajo está listo: valorar") }
            TextButton(onClick = { vm.porCancelar = s }) { Text("Cancelar solicitud", color = Colores.error) }
        }
        s.valoracion?.let { v ->
            Surface(color = Colores.exitoFondo, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Tu valoración: ${"★".repeat(v.estrellas)}${"☆".repeat(5 - v.estrellas)}", fontWeight = FontWeight.SemiBold, color = Colores.texto)
                    v.comentario?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Colores.textoSecundario) }
                }
            }
        }
    }
}

/** Chat, ficha del proveedor y confirmaciones (elegir, cancelar, valorar). */
@Composable
private fun Dialogos(vm: MisCotizacionesViewModel) {
    val trabajando = vm.trabajando
    // [MisCotizacionesViewModel.chat]: la oferta y si todavía se puede escribir.
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
                    if (trabajando) CircularProgressIndicator(Modifier.size(18.dp), color = LocalContentColor.current, strokeWidth = 2.dp) else Text("Elegir oferta")
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
                if (enviando) CircularProgressIndicator(Modifier.size(18.dp), color = LocalContentColor.current, strokeWidth = 2.dp) else Text("Enviar valoración")
            }
        },
        dismissButton = { TextButton(onClick = alCerrar, enabled = !enviando) { Text("Más tarde") } },
    )
}
