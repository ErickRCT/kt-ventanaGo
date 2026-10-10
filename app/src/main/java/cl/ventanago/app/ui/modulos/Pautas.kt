package cl.ventanago.app.ui.modulos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cl.ventanago.app.datos.Pauta
import cl.ventanago.app.datos.PautaPerfil
import cl.ventanago.app.datos.PautaQuincalleria
import cl.ventanago.app.datos.PautaVidrio
import cl.ventanago.app.datos.Perfil
import cl.ventanago.app.datos.Quincalleria
import cl.ventanago.app.datos.Recursos
import cl.ventanago.app.datos.Serie
import cl.ventanago.app.datos.TipoPauta
import cl.ventanago.app.datos.mensaje
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.comun.CampoTexto
import cl.ventanago.app.ui.comun.DialogoBuscar
import cl.ventanago.app.ui.comun.FormularioPantalla
import cl.ventanago.app.ui.comun.Interruptor
import cl.ventanago.app.ui.comun.Mantenedor
import cl.ventanago.app.ui.comun.Selector
import cl.ventanago.app.ui.comun.ejecutar
import cl.ventanago.app.ui.comun.rememberEstadoGuardado

// ---------- Lista ----------

@Composable
fun PantallaPautas() = Mantenedor(
    titulo = "Pautas",
    descripcion = "Receta de fabricación de cada producto: perfiles, quincallería y vidrios con sus descuentos.",
    cargar = { Recursos.pautas.listar().sortedWith(compareBy({ it.serie?.nombre?.lowercase() }, { it.nombre.lowercase() })) },
    clave = { it.pautaId ?: it.nombre },
    textoBusqueda = { p ->
        listOf(p.nombre, p.descripcion.orEmpty(), p.serie?.nombre.orEmpty(), p.tipoPauta?.nombre.orEmpty())
            .plus(p.quincallerias.orEmpty().mapNotNull { it.quincalleria?.nombre })
            .plus(p.perfiles.orEmpty().mapNotNull { it.perfil?.codigo })
            .joinToString(" ")
    },
    nombre = { it.nombre },
    textoNuevo = "Nueva pauta",
    formulario = { item, terminar -> FormularioPauta(item, terminar) },
    eliminar = { Recursos.pautas.eliminar(it.pautaId!!) },
) { p ->
    Column(Modifier.weight(1f)) {
        Text(p.nombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            listOfNotNull(p.serie?.nombre, p.tipoPauta?.nombre, p.descripcion?.takeIf { it.isNotBlank() }).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = Colores.gris,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Etiqueta("${p.perfiles.orEmpty().size} perfiles", Colores.fondo, Colores.textoSecundario)
            Etiqueta("${p.quincallerias.orEmpty().size} quinc.", Colores.fondo, Colores.textoSecundario)
            Etiqueta("${p.vidrios.orEmpty().size} vidrios", Colores.fondo, Colores.textoSecundario)
            if (p.isReforzada == true) Etiqueta("Reforzada", Color(0xFFFFF4E5), Color(0xFF8A5300))
        }
    }
}

// ---------- Líneas editables (texto mientras se escribe; se convierten al guardar) ----------

private class LineaPerfil(val perfil: Perfil, val orientacion: String, val corte: String?, cantidad: Long, variacion: Long, dividir: Boolean) {
    var cantidad by mutableStateOf(cantidad.toString())
    var variacion by mutableStateOf(variacion.toString())
    var dividir by mutableStateOf(dividir)
    val cantidadValida get() = (cantidad.toLongOrNull() ?: 0) > 0
}

private class LineaQuincalleria(val quincalleria: Quincalleria, cantidad: Int, variacionH: Int, variacionV: Int) {
    val porMetro = quincalleria.unidad == "Mt"
    var cantidad by mutableStateOf(cantidad.toString())
    var variacionH by mutableStateOf(variacionH.toString())
    var variacionV by mutableStateOf(variacionV.toString())
    val valida
        get() = if (porMetro) (variacionH.toIntOrNull() ?: 0) > 0 && (variacionV.toIntOrNull() ?: 0) > 0
        else (cantidad.toIntOrNull() ?: 0) > 0
}

private class LineaVidrio(cantidad: Long, variacionH: Long, variacionV: Long, formula: String) {
    var cantidad by mutableStateOf(cantidad.toString())
    var variacionH by mutableStateOf(variacionH.toString())
    var variacionV by mutableStateOf(variacionV.toString())
    var formula by mutableStateOf(formula)
    val valida
        get() = (cantidad.toLongOrNull() ?: 0) > 0 && (variacionH.toLongOrNull() ?: 0) != 0L && (variacionV.toLongOrNull() ?: 0) != 0L
}

/** Deja solo dígitos y, si se permite, un signo menos al inicio. */
private fun entero(texto: String, conSigno: Boolean = false): String {
    val digitos = texto.filter { it.isDigit() }.take(6)
    return if (conSigno && texto.startsWith("-")) "-$digitos" else digitos
}

/** Campo numérico; con [conSigno] muestra un botón ± porque el teclado numérico de Android no siempre trae el signo menos. */
@Composable
private fun CampoNumero(
    etiqueta: String,
    valor: String,
    alCambiar: (String) -> Unit,
    modifier: Modifier = Modifier,
    conSigno: Boolean = false,
    sufijo: String? = null,
    error: Boolean = false,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { alCambiar(entero(it, conSigno)) },
        label = { Text(etiqueta, maxLines = 1) },
        suffix = sufijo?.let { { Text(it) } },
        trailingIcon = if (conSigno) {
            { TextButton(onClick = { alCambiar(if (valor.startsWith("-")) valor.drop(1) else "-$valor") }) { Text("±", fontWeight = FontWeight.Bold) } }
        } else null,
        isError = error,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

@Composable
private fun Seccion(numero: Int, titulo: String, descripcion: String, contenido: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Colores.borde),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Colores.azul, shape = RoundedCornerShape(50)) {
                    Text(
                        "$numero",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
                Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            }
            Text(descripcion, style = MaterialTheme.typography.bodySmall, color = Colores.gris)
            contenido()
        }
    }
}

/** Tarjeta de un elemento agregado a la pauta, con botón para quitarlo. */
@Composable
private fun Linea(titulo: String, detalle: String?, error: Boolean, alQuitar: () -> Unit, contenido: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = Colores.fondo,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (error) Colores.error else Colores.borde),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(titulo, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    detalle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Colores.gris) }
                }
                IconButton(onClick = alQuitar) { Icon(Icons.Outlined.DeleteOutline, contentDescription = "Quitar", tint = Colores.error) }
            }
            Column(Modifier.padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = contenido)
        }
    }
}

@Composable
private fun BotonAgregar(texto: String, habilitado: Boolean = true, alPulsar: () -> Unit) {
    OutlinedButton(onClick = alPulsar, enabled = habilitado, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Text(texto, modifier = Modifier.padding(start = 6.dp))
    }
}

private fun orientacionTexto(o: String?) = if (o == "V") "Vertical" else "Horizontal"

// ---------- Formulario ----------

@Composable
private fun FormularioPauta(inicial: Pauta?, terminar: (String?) -> Unit) {
    val estado = rememberEstadoGuardado()
    val alcance = rememberCoroutineScope()
    val series = rememberOpciones({ Recursos.series.listar().sortedBy { it.nombre.lowercase() } }) { estado.error = it }
    val tipos = rememberOpciones({ Recursos.tiposPauta.listar().sortedBy { it.nombre.lowercase() } }) { estado.error = it }

    var serie by remember { mutableStateOf<Serie?>(inicial?.serie) }
    var nombre by remember { mutableStateOf(inicial?.nombre.orEmpty()) }
    var descripcion by remember { mutableStateOf(inicial?.descripcion.orEmpty()) }
    var tipo by remember { mutableStateOf<TipoPauta?>(inicial?.tipoPauta) }
    var reforzada by remember { mutableStateOf(inicial?.isReforzada == true) }
    var verticalReforzada by remember { mutableStateOf(inicial?.verticalReforzada?.takeIf { it > 0 }?.toString().orEmpty()) }
    var horizontalReforzada by remember { mutableStateOf(inicial?.horizontalReforzada?.takeIf { it > 0 }?.toString().orEmpty()) }

    val perfiles = remember {
        mutableStateListOf<LineaPerfil>().apply {
            inicial?.perfiles.orEmpty().filter { it.perfil != null }
                .sortedBy { it.perfil!!.codigo }
                .forEach { add(LineaPerfil(it.perfil!!, it.orientacion, it.corte, it.cantidad, it.variacion, it.dividir)) }
        }
    }
    val quincallerias = remember {
        mutableStateListOf<LineaQuincalleria>().apply {
            inicial?.quincallerias.orEmpty().filter { it.quincalleria != null }
                .sortedBy { it.quincalleria!!.nombre }
                .forEach { add(LineaQuincalleria(it.quincalleria!!, it.cantidad, it.variacionH, it.variacionV)) }
        }
    }
    val vidrios = remember {
        mutableStateListOf<LineaVidrio>().apply {
            inicial?.vidrios.orEmpty().forEach { add(LineaVidrio(it.cantidad, it.variacionH, it.variacionV, it.formula.orEmpty())) }
        }
    }

    // Perfiles y quincallería disponibles dependen de la serie elegida.
    var perfilesSerie by remember { mutableStateOf<List<Perfil>?>(null) }
    var quincalleriaSerie by remember { mutableStateOf<List<Quincalleria>?>(null) }
    LaunchedEffect(serie?.serieId) {
        val id = serie?.serieId ?: return@LaunchedEffect
        perfilesSerie = null
        quincalleriaSerie = null
        try {
            perfilesSerie = Recursos.perfilesDeSerie(id).sortedBy { it.codigo }
            quincalleriaSerie = Recursos.quincalleriaDeSerie(id).sortedBy { it.nombre }
        } catch (e: Exception) {
            estado.error = e.mensaje()
            perfilesSerie = perfilesSerie ?: emptyList()
            quincalleriaSerie = quincalleriaSerie ?: emptyList()
        }
    }

    var buscandoPerfil by remember { mutableStateOf(false) }
    var buscandoQuincalleria by remember { mutableStateOf(false) }
    var validar by remember { mutableStateOf(false) }

    val refuerzoValido = !reforzada ||
        ((verticalReforzada.toIntOrNull() ?: 0) > 0 && (horizontalReforzada.toIntOrNull() ?: 0) > 0)

    fun problemas() = listOfNotNull(
        "Elige la serie.".takeIf { serie == null },
        "Ingresa el nombre.".takeIf { nombre.isBlank() },
        "Elige el tipo de pauta.".takeIf { tipo == null },
        "Indica las medidas de refuerzo (mayores a 0).".takeIf { !refuerzoValido },
        "Agrega al menos un perfil.".takeIf { perfiles.isEmpty() },
        "Revisa la cantidad de los perfiles (mayor a 0).".takeIf { perfiles.any { !it.cantidadValida } },
        "Agrega al menos una quincallería.".takeIf { quincallerias.isEmpty() },
        "Revisa la quincallería: cantidad para piezas y variaciones para metros (mayores a 0).".takeIf { quincallerias.any { !it.valida } },
        "Agrega al menos un vidrio.".takeIf { vidrios.isEmpty() },
        "Revisa los vidrios: cantidad mayor a 0 y variaciones distintas de 0.".takeIf { vidrios.any { !it.valida } },
    )

    FormularioPantalla(
        titulo = if (inicial == null) "Nueva pauta" else "Editar pauta",
        guardando = estado.guardando,
        error = estado.error,
        alCerrar = { terminar(null) },
        alGuardar = {
            validar = true
            val faltan = problemas()
            if (faltan.isNotEmpty()) {
                estado.error = faltan.joinToString("\n") { "• $it" }
                return@FormularioPantalla
            }
            estado.error = null
            val pauta = (inicial ?: Pauta()).copy(
                nombre = nombre.trim(),
                descripcion = descripcion.trim(),
                serie = serie,
                tipoPauta = tipo,
                isReforzada = reforzada,
                verticalReforzada = if (reforzada) verticalReforzada.toInt() else 0,
                horizontalReforzada = if (reforzada) horizontalReforzada.toInt() else 0,
                perfiles = perfiles.map {
                    PautaPerfil(
                        perfil = it.perfil,
                        orientacion = it.orientacion,
                        corte = it.corte,
                        cantidad = it.cantidad.toLong(),
                        variacion = it.variacion.toLongOrNull() ?: 0,
                        dividir = it.dividir,
                    )
                },
                quincallerias = quincallerias.map {
                    PautaQuincalleria(
                        quincalleria = it.quincalleria,
                        cantidad = if (it.porMetro) 0 else it.cantidad.toInt(),
                        variacionH = if (it.porMetro) it.variacionH.toInt() else 0,
                        variacionV = if (it.porMetro) it.variacionV.toInt() else 0,
                    )
                },
                vidrios = vidrios.map {
                    PautaVidrio(
                        cantidad = it.cantidad.toLong(),
                        variacionH = it.variacionH.toLong(),
                        variacionV = it.variacionV.toLong(),
                        formula = it.formula.trim(),
                    )
                },
            )
            estado.ejecutar(alcance, {
                if (inicial == null) Recursos.pautas.agregar(pauta) else Recursos.pautas.modificar(pauta)
            }) { terminar(if (inicial == null) "Pauta creada" else "Pauta actualizada") }
        },
    ) {
        Seccion(1, "Serie", "Los perfiles y la quincallería disponibles dependen de la serie.") {
            Selector(
                "Serie", series.orEmpty(), series?.find { it.serieId == serie?.serieId } ?: serie,
                texto = { it.nombre },
                alElegir = {
                    if (it.serieId != serie?.serieId && serie != null) {
                        // Lo agregado pertenece a la serie anterior.
                        perfiles.clear()
                        quincallerias.clear()
                    }
                    serie = it
                },
                cargando = series == null,
                error = REQUERIDO.takeIf { validar && serie == null },
            )
        }

        Seccion(2, "Datos básicos", "Nombre, tipo de producto y medidas desde las que se refuerza.") {
            CampoTexto("Nombre", nombre, { nombre = it }, error = REQUERIDO.takeIf { validar && nombre.isBlank() })
            CampoTexto("Descripción (opcional)", descripcion, { descripcion = it }, lineas = 2)
            Selector(
                "Tipo de pauta", tipos.orEmpty(), tipos?.find { it.tipoPautaId == tipo?.tipoPautaId } ?: tipo,
                texto = { it.nombre },
                alElegir = { tipo = it },
                cargando = tipos == null,
                error = REQUERIDO.takeIf { validar && tipo == null },
            )
            Interruptor("Reforzada", reforzada, {
                reforzada = it
                if (!it) {
                    verticalReforzada = ""
                    horizontalReforzada = ""
                }
            }, "Usa perfiles reforzados sobre ciertas medidas")
            if (reforzada) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoNumero(
                        "Vertical", verticalReforzada, { verticalReforzada = it },
                        Modifier.weight(1f), sufijo = "mm",
                        error = validar && (verticalReforzada.toIntOrNull() ?: 0) <= 0,
                    )
                    CampoNumero(
                        "Horizontal", horizontalReforzada, { horizontalReforzada = it },
                        Modifier.weight(1f), sufijo = "mm",
                        error = validar && (horizontalReforzada.toIntOrNull() ?: 0) <= 0,
                    )
                }
            }
        }

        Seccion(3, "Perfiles", "Cantidad de cada perfil y variación (mm) respecto de la medida de la ventana.") {
            perfiles.forEach { linea ->
                key(linea) {
                    Linea(
                        titulo = listOfNotNull(linea.perfil.codigo, linea.perfil.descripcion?.takeIf { it.isNotBlank() }).joinToString(" · "),
                        detalle = listOfNotNull(orientacionTexto(linea.orientacion), linea.perfil.tipoPerfil?.nombre).joinToString(" · "),
                        error = validar && !linea.cantidadValida,
                        alQuitar = { perfiles.remove(linea) },
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CampoNumero("Cantidad", linea.cantidad, { linea.cantidad = it }, Modifier.weight(0.8f), error = validar && !linea.cantidadValida)
                            CampoNumero("Variación", linea.variacion, { linea.variacion = it }, Modifier.weight(1.2f), conSigno = true, sufijo = "mm")
                        }
                        Interruptor("Dividir", linea.dividir, { linea.dividir = it })
                    }
                }
            }
            BotonAgregar(if (serie == null) "Elige primero la serie" else "Agregar perfil", habilitado = serie != null) { buscandoPerfil = true }
        }

        Seccion(4, "Quincallería", "Por pieza se indica la cantidad; por metro, cuánto se suma al ancho y al alto.") {
            quincallerias.forEach { linea ->
                key(linea) {
                    Linea(
                        titulo = linea.quincalleria.nombre,
                        detalle = if (linea.porMetro) "Por metro (Mt)" else "Por pieza (Pz)",
                        error = validar && !linea.valida,
                        alQuitar = { quincallerias.remove(linea) },
                    ) {
                        if (linea.porMetro) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CampoNumero("Variación H", linea.variacionH, { linea.variacionH = it }, Modifier.weight(1f), sufijo = "mm", error = validar && !linea.valida)
                                CampoNumero("Variación V", linea.variacionV, { linea.variacionV = it }, Modifier.weight(1f), sufijo = "mm", error = validar && !linea.valida)
                            }
                        } else {
                            CampoNumero("Cantidad", linea.cantidad, { linea.cantidad = it }, Modifier.fillMaxWidth(), error = validar && !linea.valida)
                        }
                    }
                }
            }
            BotonAgregar(if (serie == null) "Elige primero la serie" else "Agregar quincallería", habilitado = serie != null) { buscandoQuincalleria = true }
        }

        Seccion(5, "Vidrios", "Paños de vidrio: cantidad, descuentos (mm) y fórmula.") {
            vidrios.forEachIndexed { i, linea ->
                key(linea) {
                    Linea(
                        titulo = "Vidrio ${i + 1}",
                        detalle = null,
                        error = validar && !linea.valida,
                        alQuitar = { vidrios.remove(linea) },
                    ) {
                        CampoNumero("Cantidad", linea.cantidad, { linea.cantidad = it }, Modifier.fillMaxWidth(), error = validar && (linea.cantidad.toLongOrNull() ?: 0) <= 0)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CampoNumero("Variación H", linea.variacionH, { linea.variacionH = it }, Modifier.weight(1f), conSigno = true, error = validar && (linea.variacionH.toLongOrNull() ?: 0) == 0L)
                            CampoNumero("Variación V", linea.variacionV, { linea.variacionV = it }, Modifier.weight(1f), conSigno = true, error = validar && (linea.variacionV.toLongOrNull() ?: 0) == 0L)
                        }
                        CampoTexto("Fórmula", linea.formula, { linea.formula = it })
                    }
                }
            }
            BotonAgregar("Agregar vidrio") { vidrios.add(LineaVidrio(1, 0, 0, "")) }
        }
    }

    if (buscandoPerfil) {
        DialogoBuscar(
            titulo = "Agregar perfil",
            opciones = perfilesSerie.orEmpty(),
            texto = { "${it.codigo} · ${it.descripcion.orEmpty()}" },
            detalle = { listOfNotNull(orientacionTexto(it.orientacion), it.tipoPerfil?.nombre).joinToString(" · ") },
            vacio = if (perfilesSerie == null) "Cargando perfiles…" else "La serie no tiene perfiles.",
            alElegir = {
                perfiles.add(LineaPerfil(it, it.orientacion ?: "H", null, 1, 0, false))
                buscandoPerfil = false
            },
            alCerrar = { buscandoPerfil = false },
        )
    }
    if (buscandoQuincalleria) {
        val agregadas = quincallerias.mapNotNull { it.quincalleria.quincalleriaId }.toSet()
        DialogoBuscar(
            titulo = "Agregar quincallería",
            opciones = quincalleriaSerie.orEmpty().filter { it.quincalleriaId !in agregadas },
            texto = { it.nombre },
            detalle = { if (it.unidad == "Mt") "Por metro" else "Por pieza" },
            vacio = if (quincalleriaSerie == null) "Cargando quincallería…" else "No queda quincallería de esta serie por agregar.",
            alElegir = {
                quincallerias.add(LineaQuincalleria(it, if (it.unidad == "Mt") 0 else 1, 0, 0))
                buscandoQuincalleria = false
            },
            alCerrar = { buscandoQuincalleria = false },
        )
    }
}
