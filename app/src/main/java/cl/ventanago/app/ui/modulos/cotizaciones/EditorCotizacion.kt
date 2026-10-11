package cl.ventanago.app.ui.modulos.cotizaciones

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cl.ventanago.app.datos.Cotizacion
import cl.ventanago.app.datos.estadosCotizacion
import cl.ventanago.app.datos.factorVenta
import cl.ventanago.app.datos.pesos
import cl.ventanago.app.datos.precioVentaUnitario
import cl.ventanago.app.datos.resumen
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.comun.Bloque
import cl.ventanago.app.ui.comun.CampoTexto
import cl.ventanago.app.ui.comun.DialogoBuscar
import cl.ventanago.app.ui.comun.DialogoConfirmar
import cl.ventanago.app.ui.comun.EstadoCargando
import cl.ventanago.app.ui.comun.EstadoError
import cl.ventanago.app.ui.comun.MensajeError
import cl.ventanago.app.ui.comun.Opciones
import cl.ventanago.app.ui.comun.fechaLegible
import cl.ventanago.app.ui.modulos.soloDigitos
import java.util.Locale

/**
 * Crear o editar una cotización (equivale a CrearCotizacion de la web). Los datos y la lógica están en
 * [EstadoEditorCotizacion]; aquí solo la pantalla.
 *
 * [terminar] recibe un mensaje si hubo cambios guardados (para recargar la lista) o null.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorCotizacion(id: Long?, terminar: (String?) -> Unit) {
    val e = rememberEstadoEditorCotizacion(id)

    fun cerrar() {
        if (e.sinGuardar) e.confirmarSalida = true
        else terminar(if (e.huboCambios) "Cotización guardada" else null)
    }

    Dialog(
        onDismissRequest = { cerrar() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnClickOutside = false),
    ) {
        BackHandler { cerrar() }
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = { Text(e.cotizacion?.cotizacionId?.let { "Cotización N° $it" } ?: "Nueva cotización", maxLines = 1) },
                        navigationIcon = { IconButton(onClick = { cerrar() }) { Icon(Icons.Filled.Close, contentDescription = "Cerrar") } },
                        actions = {
                            TextButton(
                                onClick = { e.tarea("Guardando…") { e.guardar() } },
                                enabled = e.trabajando == null && e.cotizacion != null,
                            ) { Text("GUARDAR", color = Color.White, fontWeight = FontWeight.Bold) }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Colores.azul,
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White,
                        ),
                    )
                    if (e.trabajando != null) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            val c = e.cotizacion
            when {
                c == null && e.errorCarga != null -> Column(Modifier.padding(padding)) { EstadoError(e.errorCarga!!) { e.cargar() } }
                c == null -> Column(Modifier.padding(padding)) { EstadoCargando("Cargando cotización…") }
                else -> {
                    val desplazamiento = rememberScrollState()
                    LaunchedEffect(e.error) { if (e.error != null) desplazamiento.animateScrollTo(0) }
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
                        e.error?.let { MensajeError(it) }
                        e.trabajando?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Colores.gris) }
                        BloqueDatos(e, c)
                        BloqueVentanas(e, c)
                        BloquePrecios(e)
                        BloqueResumen(e)
                        BloqueDocumentos(e)
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    Dialogos(e, terminar)
}

@Composable
private fun BloqueDatos(e: EstadoEditorCotizacion, c: Cotizacion) {
    Bloque("Datos") {
        CampoTexto("Nombre de la cotización", e.nombre, { e.editar { nombre = it } }, ayuda = "Ej: Casa Pérez, ventanas segundo piso")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                val cliente = e.cliente
                Text("Cliente", style = MaterialTheme.typography.labelLarge, color = Colores.textoSecundario)
                Text(cliente?.nombre ?: "Sin cliente", fontWeight = FontWeight.SemiBold, color = if (cliente == null) Colores.gris else Colores.texto)
                cliente?.let {
                    Text(listOfNotNull(it.rut, it.telefono, it.email).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = Colores.gris)
                }
            }
            OutlinedButton(onClick = e::abrirClientes) {
                Icon(Icons.Outlined.PersonSearch, contentDescription = null)
                Text(if (e.cliente == null) " Elegir" else " Cambiar")
            }
        }
        Opciones("Estado", estadosCotizacion, e.estadoCot) { e.editar { estadoCot = it } }
        Text("Fecha: ${fechaLegible(c.fecha)}", style = MaterialTheme.typography.bodySmall, color = Colores.gris)
    }
}

@Composable
private fun BloqueVentanas(e: EstadoEditorCotizacion, c: Cotizacion) {
    val vista = e.actual()
    val factor = factorVenta(vista.ganancia, vista.descuento)
    Bloque("Ventanas", "${c.ventanas.orEmpty().sumOf { it.cantidad ?: 1 }} unidades · ${"%.2f".format(Locale.forLanguageTag("es-CL"), c.totalm2 ?: 0.0)} m²") {
        if (c.ventanas.isNullOrEmpty()) {
            Text("Aún no hay ventanas. Agrega la primera para calcular el precio.", color = Colores.gris, style = MaterialTheme.typography.bodyMedium)
        }
        c.ventanas.orEmpty().sortedBy { it.ventanaId }.forEach { v ->
            val unidades = v.cantidad ?: 1
            val unitario = precioVentaUnitario(v, factor)
            Surface(
                color = Colores.fondo,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Colores.borde),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            listOfNotNull(v.descripcion?.takeIf { it.isNotBlank() }, v.pauta?.nombre).distinct().joinToString(" · "),
                            fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${v.ancho} × ${v.alto} mm · ${v.color?.nombre.orEmpty()} · ${v.vidrio?.nombre.orEmpty()}",
                            style = MaterialTheme.typography.bodySmall, color = Colores.gris,
                        )
                        Text(
                            "$unidades × ${pesos(unitario)} = ${pesos(unitario * unidades)}",
                            style = MaterialTheme.typography.bodyMedium, color = Colores.azul, fontWeight = FontWeight.SemiBold,
                        )
                        v.observaciones?.takeIf { it.isNotBlank() }?.let {
                            Text("Obs: $it", style = MaterialTheme.typography.bodySmall, color = Colores.gris)
                        }
                    }
                    IconButton(onClick = { e.aEliminar = v }, enabled = e.trabajando == null) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Quitar ventana", tint = Colores.error)
                    }
                }
            }
        }
        Button(onClick = { e.agregandoVentana = true }, enabled = e.trabajando == null, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(" Agregar ventana")
        }
    }
}

@Composable
private fun BloquePrecios(e: EstadoEditorCotizacion) {
    Bloque("Precios", "La ganancia y el descuento se aplican a cada ventana.") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoTexto("Ganancia", e.ganancia, { e.editar { ganancia = porcentaje(it) } }, Modifier.weight(1f), teclado = KeyboardType.Number, ayuda = "%")
            CampoTexto(
                "Descuento", e.descuento, { e.editar { descuento = porcentaje(it) } }, Modifier.weight(1f),
                teclado = KeyboardType.Number, ayuda = "%",
                error = "Máximo 100".takeIf { numero(e.descuento) > 100 },
            )
        }
        Extra("Flete", e.flete, { e.editar { flete = it } }, e.valorFlete, { e.editar { valorFlete = soloDigitos(it) } })
        Extra("Instalación", e.instalacion, { e.editar { instalacion = it } }, e.valorInstalacion, { e.editar { valorInstalacion = soloDigitos(it) } })
        CampoTexto("Mano de obra", e.valorManoDeObra, { e.editar { valorManoDeObra = soloDigitos(it) } }, teclado = KeyboardType.Number, prefijo = "$ ")
        Extra("Otros gastos", e.otrosGastos, { e.editar { otrosGastos = it } }, e.valorOtrosGastos, { e.editar { valorOtrosGastos = soloDigitos(it) } })
        CampoTexto("Condiciones", e.condiciones, { e.editar { condiciones = it } }, lineas = 2, ayuda = "Aparecen al final del PDF. Ej: 50% de anticipo, entrega en 15 días.")
    }
}

@Composable
private fun BloqueResumen(e: EstadoEditorCotizacion) {
    val resumen = e.actual().resumen()
    Bloque("Resumen") {
        Fila("Costo de fabricación", pesos(resumen.costo), secundaria = true)
        Fila("Ventanas (con ganancia y descuento)", pesos(resumen.ventanas))
        if (resumen.extras > 0) Fila("Costos extras", pesos(resumen.extras))
        HorizontalDivider(color = Colores.borde)
        Fila("Neto", pesos(resumen.neto))
        Fila("IVA 19%", pesos(resumen.iva))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Total", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(pesos(resumen.total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Colores.azul)
        }
        if (e.sinGuardar) Text("Hay cambios sin guardar.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8A5300))
    }
}

@Composable
private fun BloqueDocumentos(e: EstadoEditorCotizacion) {
    val libre = e.trabajando == null
    Bloque("Documentos", "Se guardan los cambios antes de generar el PDF.") {
        Button(
            onClick = { e.documento({ "/documentos/cotizacion/$it" }, { "cotizacion_$it.pdf" }) },
            enabled = libre,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.Description, contentDescription = null)
            Text(" Ver PDF de cotización")
        }
        OutlinedButton(
            onClick = { e.documento({ "/documentos/cotizacion/$it" }, { "cotizacion_$it.pdf" }, compartir = true) },
            enabled = libre,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.Share, contentDescription = null)
            Text(" Compartir cotización")
        }
        OutlinedButton(
            onClick = { e.documento({ "/documentos/orden-de-trabajo/$it" }, { "orden_de_trabajo_$it.pdf" }) },
            enabled = libre,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.Engineering, contentDescription = null)
            Text(" Orden de trabajo")
        }
    }
}

@Composable
private fun Dialogos(e: EstadoEditorCotizacion, terminar: (String?) -> Unit) {
    if (e.agregandoVentana) {
        FormularioVentana(
            factorVenta = factorVenta(numero(e.ganancia), numero(e.descuento)),
            alGuardar = e::agregarVentana,
            alCerrar = { e.agregandoVentana = false },
        )
    }

    if (e.eligiendoCliente) {
        val clientes = e.clientes
        DialogoBuscar(
            titulo = "Elegir cliente",
            opciones = clientes.orEmpty(),
            texto = { it.nombre },
            detalle = { listOfNotNull(it.rut, it.comuna?.nombre).joinToString(" · ") },
            vacio = if (clientes == null) "Cargando clientes…" else "No hay clientes. Créalos en el módulo Clientes.",
            alElegir = e::elegirCliente,
            alCerrar = { e.eligiendoCliente = false },
        )
    }

    e.aEliminar?.let { v ->
        DialogoConfirmar(
            titulo = "¿Quitar ventana?",
            mensaje = "Se quitará \"${v.descripcion ?: v.pauta?.nombre.orEmpty()}\" de la cotización.",
            textoConfirmar = "Quitar",
            procesando = e.trabajando != null,
            alConfirmar = { e.quitarVentana(v) },
            alCancelar = { e.aEliminar = null },
        )
    }

    if (e.confirmarSalida) {
        AlertDialog(
            onDismissRequest = { e.confirmarSalida = false },
            title = { Text("¿Guardar los cambios?") },
            text = { Text("Modificaste datos o precios de la cotización que aún no se guardan.") },
            confirmButton = {
                Button(onClick = {
                    e.confirmarSalida = false
                    e.tarea("Guardando…") {
                        e.guardar()
                        terminar("Cotización guardada")
                    }
                }) { Text("Guardar") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { e.confirmarSalida = false }) { Text("Cancelar") }
                    TextButton(onClick = {
                        e.confirmarSalida = false
                        terminar(if (e.huboCambios) "Cotización guardada" else null)
                    }) { Text("Descartar", color = Colores.error) }
                }
            },
        )
    }
}

/** Costo extra: descripción opcional y valor en pesos. */
@Composable
private fun Extra(etiqueta: String, descripcion: String, alCambiarDescripcion: (String) -> Unit, valor: String, alCambiarValor: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CampoTexto(etiqueta, descripcion, alCambiarDescripcion, Modifier.weight(1.2f), ayuda = "Detalle")
        CampoTexto("Valor", valor, alCambiarValor, Modifier.weight(1f), teclado = KeyboardType.Number, prefijo = "$ ")
    }
}

@Composable
private fun Fila(etiqueta: String, valor: String, secundaria: Boolean = false) {
    Row {
        Text(
            etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = if (secundaria) Colores.gris else Colores.texto,
            modifier = Modifier.weight(1f),
        )
        Text(valor, style = MaterialTheme.typography.bodyMedium, fontWeight = if (secundaria) FontWeight.Normal else FontWeight.SemiBold, color = if (secundaria) Colores.gris else Colores.texto)
    }
}
