package cl.ventanago.app.ui.modulos.cotizaciones

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cl.ventanago.app.datos.Cliente
import cl.ventanago.app.datos.Cotizacion
import cl.ventanago.app.datos.Cotizaciones
import cl.ventanago.app.datos.Documentos
import cl.ventanago.app.datos.Recursos
import cl.ventanago.app.datos.Ventana
import cl.ventanago.app.datos.estadosCotizacion
import cl.ventanago.app.datos.factorVenta
import cl.ventanago.app.datos.mensaje
import cl.ventanago.app.datos.pesos
import cl.ventanago.app.datos.precioVentaUnitario
import cl.ventanago.app.datos.resumen
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.comun.CampoTexto
import cl.ventanago.app.ui.comun.DialogoBuscar
import cl.ventanago.app.ui.comun.DialogoConfirmar
import cl.ventanago.app.ui.comun.EstadoCargando
import cl.ventanago.app.ui.comun.EstadoError
import cl.ventanago.app.ui.comun.MensajeError
import cl.ventanago.app.ui.comun.Opciones
import cl.ventanago.app.ui.modulos.soloDigitos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun numero(texto: String) = texto.toIntOrNull() ?: 0

/** Porcentaje entre 0 y 100. */
private fun porcentaje(texto: String) = soloDigitos(texto).take(3)

fun fechaLegible(fecha: String?): String = try {
    LocalDate.parse(fecha).format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-CL")))
} catch (_: Exception) {
    fecha.orEmpty()
}

/**
 * Crear o editar una cotización (equivale a CrearCotizacion de la web). Las ventanas se guardan en el
 * servidor apenas se agregan; los datos y precios se guardan con "Guardar" (o antes de generar un PDF).
 *
 * [terminar] recibe un mensaje si hubo cambios guardados (para recargar la lista) o null.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorCotizacion(id: Long?, terminar: (String?) -> Unit) {
    val alcance = rememberCoroutineScope()
    val context = LocalContext.current

    var cotizacion by remember { mutableStateOf<Cotizacion?>(null) }
    var errorCarga by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var trabajando by remember { mutableStateOf<String?>(null) }
    var huboCambios by remember { mutableStateOf(false) }
    var sinGuardar by remember { mutableStateOf(false) }

    // Campos editables
    var nombre by remember { mutableStateOf("") }
    var cliente by remember { mutableStateOf<Cliente?>(null) }
    var estadoCot by remember { mutableStateOf("CREADA") }
    var ganancia by remember { mutableStateOf("50") }
    var descuento by remember { mutableStateOf("0") }
    var flete by remember { mutableStateOf("") }
    var valorFlete by remember { mutableStateOf("") }
    var instalacion by remember { mutableStateOf("") }
    var valorInstalacion by remember { mutableStateOf("") }
    var valorManoDeObra by remember { mutableStateOf("") }
    var otrosGastos by remember { mutableStateOf("") }
    var valorOtrosGastos by remember { mutableStateOf("") }
    var condiciones by remember { mutableStateOf("") }

    fun cargarCampos(c: Cotizacion) {
        nombre = c.nombreCotizacion.orEmpty()
        cliente = c.cliente
        estadoCot = estadosCotizacion.firstOrNull { it.first.equals(c.estado, true) }?.first ?: "CREADA"
        ganancia = (c.ganancia ?: 0).toString()
        descuento = (c.descuento ?: 0).toString()
        flete = c.flete.orEmpty()
        valorFlete = c.valorFlete?.takeIf { it > 0 }?.toString().orEmpty()
        instalacion = c.instalacion.orEmpty()
        valorInstalacion = c.valorInstalacion?.takeIf { it > 0 }?.toString().orEmpty()
        valorManoDeObra = c.valorManoDeObra?.takeIf { it > 0 }?.toString().orEmpty()
        otrosGastos = c.otrosGastos.orEmpty()
        valorOtrosGastos = c.valorOtrosGastos?.takeIf { it > 0 }?.toString().orEmpty()
        condiciones = c.condiciones.orEmpty()
        sinGuardar = false
    }

    LaunchedEffect(recarga) {
        if (id == null && cotizacion == null) {
            val nueva = Cotizacion(fecha = LocalDate.now().toString(), ganancia = 50, descuento = 0, estado = "CREADA", ventanas = emptyList())
            cotizacion = nueva
            cargarCampos(nueva)
            return@LaunchedEffect
        }
        val idCargar = cotizacion?.cotizacionId ?: id ?: return@LaunchedEffect
        try {
            val c = Cotizaciones.obtener(idCargar)
            cotizacion = c
            cargarCampos(c)
            errorCarga = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorCarga = e.mensaje()
        }
    }

    /** La cotización con lo que hay en pantalla (para el resumen y para guardar). */
    fun actual(): Cotizacion = (cotizacion ?: Cotizacion()).copy(
        nombreCotizacion = nombre.trim().ifBlank { null },
        cliente = cliente,
        estado = estadoCot,
        ganancia = numero(ganancia),
        descuento = numero(descuento).coerceAtMost(100),
        flete = flete.trim().ifBlank { null },
        valorFlete = numero(valorFlete),
        instalacion = instalacion.trim().ifBlank { null },
        valorInstalacion = numero(valorInstalacion),
        valorManoDeObra = numero(valorManoDeObra),
        otrosGastos = otrosGastos.trim().ifBlank { null },
        valorOtrosGastos = numero(valorOtrosGastos),
        condiciones = condiciones.trim().ifBlank { null },
    )

    /** Guarda los datos y precios; si la cotización aún no existe, la crea. Devuelve el id. */
    suspend fun guardar(): Long {
        val datos = actual()
        val idGuardado = if (datos.cotizacionId == null) {
            Cotizaciones.agregar(datos).cotizacionId
                ?: throw IllegalStateException("El servidor no devolvió el número de la cotización.")
        } else {
            Cotizaciones.modificar(datos)
            datos.cotizacionId
        }
        val fresca = Cotizaciones.obtener(idGuardado)
        cotizacion = fresca
        cargarCampos(fresca)
        huboCambios = true
        return idGuardado
    }

    fun tarea(descripcion: String, accion: suspend () -> Unit) {
        if (trabajando != null) return
        alcance.launch {
            trabajando = descripcion
            error = null
            try {
                accion()
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalStateException) {
                error = e.message
            } catch (e: Exception) {
                error = e.mensaje()
            } finally {
                trabajando = null
            }
        }
    }

    fun documento(ruta: (Long) -> String, archivo: (Long) -> String, compartir: Boolean = false) = tarea("Generando PDF…") {
        if (actual().ventanas.isNullOrEmpty()) {
            error = "Agrega al menos una ventana antes de generar el documento."
            return@tarea
        }
        val idDoc = if (sinGuardar || cotizacion?.cotizacionId == null) guardar() else cotizacion!!.cotizacionId!!
        val pdf: File = Documentos.descargar(context, ruta(idDoc), archivo(idDoc))
        if (compartir) Documentos.compartir(context, pdf) else Documentos.abrir(context, pdf)
    }

    var agregandoVentana by remember { mutableStateOf(false) }
    var eligiendoCliente by remember { mutableStateOf(false) }
    var aEliminar by remember { mutableStateOf<Ventana?>(null) }
    var confirmarSalida by remember { mutableStateOf(false) }

    fun cerrar() {
        if (sinGuardar) confirmarSalida = true
        else terminar(if (huboCambios) "Cotización guardada" else null)
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
                        title = {
                            Text(
                                cotizacion?.cotizacionId?.let { "Cotización N° $it" } ?: "Nueva cotización",
                                maxLines = 1,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { cerrar() }) { Icon(Icons.Filled.Close, contentDescription = "Cerrar") }
                        },
                        actions = {
                            TextButton(
                                onClick = { tarea("Guardando…") { guardar() } },
                                enabled = trabajando == null && cotizacion != null,
                            ) { Text("GUARDAR", color = Color.White, fontWeight = FontWeight.Bold) }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Colores.azul,
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White,
                        ),
                    )
                    if (trabajando != null) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            val c = cotizacion
            when {
                c == null && errorCarga != null -> Column(Modifier.padding(padding)) {
                    EstadoError(errorCarga!!) { errorCarga = null; recarga++ }
                }
                c == null -> Column(Modifier.padding(padding)) { EstadoCargando("Cargando cotización…") }
                else -> {
                    val vista = actual()
                    val resumen = vista.resumen()
                    val factor = factorVenta(vista.ganancia, vista.descuento)
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
                        trabajando?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Colores.gris) }

                        Bloque("Datos") {
                            CampoTexto("Nombre de la cotización", nombre, { nombre = it; sinGuardar = true }, ayuda = "Ej: Casa Pérez, ventanas segundo piso")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Cliente", style = MaterialTheme.typography.labelLarge, color = Colores.textoSecundario)
                                    Text(cliente?.nombre ?: "Sin cliente", fontWeight = FontWeight.SemiBold, color = if (cliente == null) Colores.gris else Colores.texto)
                                    cliente?.let {
                                        Text(listOfNotNull(it.rut, it.telefono, it.email).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = Colores.gris)
                                    }
                                }
                                OutlinedButton(onClick = { eligiendoCliente = true }) {
                                    Icon(Icons.Outlined.PersonSearch, contentDescription = null)
                                    Text(if (cliente == null) " Elegir" else " Cambiar")
                                }
                            }
                            Opciones("Estado", estadosCotizacion, estadoCot) { estadoCot = it; sinGuardar = true }
                            Text("Fecha: ${fechaLegible(c.fecha)}", style = MaterialTheme.typography.bodySmall, color = Colores.gris)
                        }

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
                                        IconButton(onClick = { aEliminar = v }, enabled = trabajando == null) {
                                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Quitar ventana", tint = Colores.error)
                                        }
                                    }
                                }
                            }
                            Button(onClick = { agregandoVentana = true }, enabled = trabajando == null, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Text(" Agregar ventana")
                            }
                        }

                        Bloque("Precios", "La ganancia y el descuento se aplican a cada ventana.") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CampoTexto("Ganancia", ganancia, { ganancia = porcentaje(it); sinGuardar = true }, Modifier.weight(1f), teclado = KeyboardType.Number, ayuda = "%")
                                CampoTexto(
                                    "Descuento", descuento, { descuento = porcentaje(it); sinGuardar = true }, Modifier.weight(1f),
                                    teclado = KeyboardType.Number, ayuda = "%",
                                    error = "Máximo 100".takeIf { numero(descuento) > 100 },
                                )
                            }
                            Extra("Flete", flete, { flete = it; sinGuardar = true }, valorFlete, { valorFlete = soloDigitos(it); sinGuardar = true })
                            Extra("Instalación", instalacion, { instalacion = it; sinGuardar = true }, valorInstalacion, { valorInstalacion = soloDigitos(it); sinGuardar = true })
                            CampoTexto("Mano de obra", valorManoDeObra, { valorManoDeObra = soloDigitos(it); sinGuardar = true }, teclado = KeyboardType.Number, prefijo = "$ ")
                            Extra("Otros gastos", otrosGastos, { otrosGastos = it; sinGuardar = true }, valorOtrosGastos, { valorOtrosGastos = soloDigitos(it); sinGuardar = true })
                            CampoTexto("Condiciones", condiciones, { condiciones = it; sinGuardar = true }, lineas = 2, ayuda = "Aparecen al final del PDF. Ej: 50% de anticipo, entrega en 15 días.")
                        }

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
                            if (sinGuardar) Text("Hay cambios sin guardar.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8A5300))
                        }

                        Bloque("Documentos", "Se guardan los cambios antes de generar el PDF.") {
                            Button(
                                onClick = { documento({ "/documentos/cotizacion/$it" }, { "cotizacion_$it.pdf" }) },
                                enabled = trabajando == null,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Outlined.Description, contentDescription = null)
                                Text(" Ver PDF de cotización")
                            }
                            OutlinedButton(
                                onClick = { documento({ "/documentos/cotizacion/$it" }, { "cotizacion_$it.pdf" }, compartir = true) },
                                enabled = trabajando == null,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Outlined.Share, contentDescription = null)
                                Text(" Compartir cotización")
                            }
                            OutlinedButton(
                                onClick = { documento({ "/documentos/orden-de-trabajo/$it" }, { "orden_de_trabajo_$it.pdf" }) },
                                enabled = trabajando == null,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Outlined.Engineering, contentDescription = null)
                                Text(" Orden de trabajo")
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    if (agregandoVentana) {
        FormularioVentana(
            factorVenta = factorVenta(numero(ganancia), numero(descuento)),
            alGuardar = { ventana ->
                // La ventana necesita una cotización guardada a la cual pertenecer.
                val idCot = cotizacion?.cotizacionId ?: guardar()
                Cotizaciones.agregarVentana(ventana.copy(cotizacionId = idCot))
                val fresca = Cotizaciones.obtener(idCot)
                cotizacion = fresca
                huboCambios = true
            },
            alCerrar = { agregandoVentana = false },
        )
    }

    if (eligiendoCliente) {
        var clientes by remember { mutableStateOf<List<Cliente>?>(null) }
        LaunchedEffect(Unit) {
            clientes = try {
                Recursos.clientes.listar().sortedBy { it.nombre.lowercase() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.mensaje()
                emptyList()
            }
        }
        DialogoBuscar(
            titulo = "Elegir cliente",
            opciones = clientes.orEmpty(),
            texto = { it.nombre },
            detalle = { listOfNotNull(it.rut, it.comuna?.nombre).joinToString(" · ") },
            vacio = if (clientes == null) "Cargando clientes…" else "No hay clientes. Créalos en el módulo Clientes.",
            alElegir = {
                cliente = it
                sinGuardar = true
                eligiendoCliente = false
            },
            alCerrar = { eligiendoCliente = false },
        )
    }

    aEliminar?.let { v ->
        DialogoConfirmar(
            titulo = "¿Quitar ventana?",
            mensaje = "Se quitará \"${v.descripcion ?: v.pauta?.nombre.orEmpty()}\" de la cotización.",
            textoConfirmar = "Quitar",
            procesando = trabajando != null,
            alConfirmar = {
                tarea("Quitando ventana…") {
                    Cotizaciones.eliminarVentana(v.ventanaId!!)
                    cotizacion = Cotizaciones.obtener(cotizacion!!.cotizacionId!!)
                    huboCambios = true
                    aEliminar = null
                }
            },
            alCancelar = { aEliminar = null },
        )
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("¿Guardar los cambios?") },
            text = { Text("Modificaste datos o precios de la cotización que aún no se guardan.") },
            confirmButton = {
                Button(onClick = {
                    confirmarSalida = false
                    tarea("Guardando…") {
                        guardar()
                        terminar("Cotización guardada")
                    }
                }) { Text("Guardar") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") }
                    TextButton(onClick = {
                        confirmarSalida = false
                        terminar(if (huboCambios) "Cotización guardada" else null)
                    }) { Text("Descartar", color = Colores.error) }
                }
            },
        )
    }
}

@Composable
private fun Bloque(titulo: String, descripcion: String? = null, contenido: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Colores.borde),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column {
                Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                descripcion?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Colores.gris) }
            }
            contenido()
        }
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
