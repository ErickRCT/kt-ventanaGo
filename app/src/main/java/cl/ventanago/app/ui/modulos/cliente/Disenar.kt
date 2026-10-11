package cl.ventanago.app.ui.modulos.cliente

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cl.ventanago.app.ar.DetectorAR
import cl.ventanago.app.ar.RealidadAumentadaActivity
import cl.ventanago.app.ar.SoporteAR
import cl.ventanago.app.datos.CarritoLocal
import cl.ventanago.app.datos.ClienteApi
import cl.ventanago.app.datos.ColorVentana
import cl.ventanago.app.datos.Cotizaciones
import cl.ventanago.app.datos.GANANCIA_REFERENCIAL
import cl.ventanago.app.datos.ItemVentana
import cl.ventanago.app.datos.Pauta
import cl.ventanago.app.datos.Recursos
import cl.ventanago.app.datos.Ventana
import cl.ventanago.app.datos.Vidrio
import cl.ventanago.app.datos.factorVenta
import cl.ventanago.app.datos.mensaje
import cl.ventanago.app.datos.pesos
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.comun.CampoTexto
import cl.ventanago.app.ui.comun.EstadoCargando
import cl.ventanago.app.ui.comun.EstadoError
import cl.ventanago.app.ui.comun.EstadoVacio
import cl.ventanago.app.ui.comun.ImagenCatalogo
import cl.ventanago.app.ui.comun.MensajeError
import cl.ventanago.app.ui.comun.normalizar
import cl.ventanago.app.ui.modulos.soloDigitos
import cl.ventanago.app.ui.navegacion.Pantalla
import cl.ventanago.app.ventana.ConfigVentana
import cl.ventanago.app.ventana.DibujoVentana
import cl.ventanago.app.ventana.MAX_CANTIDAD
import cl.ventanago.app.ventana.MIN_MM
import cl.ventanago.app.ventana.ModeloVentana
import cl.ventanago.app.ventana.cm
import cl.ventanago.app.ventana.colorMarco
import cl.ventanago.app.ventana.colorVidrio
import cl.ventanago.app.ventana.hojasDePauta
import cl.ventanago.app.ventana.limitesDeModelo
import cl.ventanago.app.ventana.medidasCm
import cl.ventanago.app.ventana.modeloDePauta
import cl.ventanago.app.ventana.problemasDePrecio
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

/** Pauta lista para el cliente: con su serie y la forma y hojas deducidas para el dibujo. */
class PautaCliente(val pauta: Pauta, val serie: String, val hojas: Int, val modelo: ModeloVentana)

/** Pautas de todas las series, como las elige el administrador al cotizar. */
private suspend fun cargarPautas(): List<PautaCliente> = coroutineScope {
    Recursos.series.listar().sortedBy { it.nombre.lowercase() }.map { serie ->
        async {
            Cotizaciones.pautasDeSerie(serie.serieId!!).sortedBy { it.nombre.lowercase() }
                .map { PautaCliente(it, serie.nombre.trim(), hojasDePauta(it), modeloDePauta(it)) }
        }
    }.awaitAll().flatten()
}

// Si el backend no responde, el cliente igual puede diseñar con un catálogo básico (sin precio).
private val COLORES_BASICOS = listOf("Blanco", "Negro", "Titanio", "Madera", "Mate").map { ColorVentana(null, it, 0) }
private val VIDRIOS_BASICOS = listOf("Incoloro", "Bronce", "Gris").map { Vidrio(null, it, 0) }

/** Nombre de cada forma de ventana para los filtros del catálogo. */
private val ModeloVentana.nombre
    get() = when (this) {
        ModeloVentana.CORREDERA -> "Corredera"
        ModeloVentana.MONORRIEL -> "Monorriel"
        ModeloVentana.FIJA -> "Fija"
        ModeloVentana.ARCO -> "Arco"
        ModeloVentana.BATIENTE -> "Batiente"
        ModeloVentana.GRANERO -> "Granero"
        ModeloVentana.MAMPARA -> "Mampara"
    }

/** Medidas comunes para elegir con un toque (en centímetros). */
private class MedidaRapida(val nombre: String, val anchoCm: Int, val altoCm: Int)

private val MEDIDAS_RAPIDAS = listOf(
    MedidaRapida("Baño", 60, 60),
    MedidaRapida("Cocina", 100, 100),
    MedidaRapida("Dormitorio", 120, 100),
    MedidaRapida("Living", 180, 120),
    MedidaRapida("Ventanal", 200, 200),
)

private const val MIN_CM = MIN_MM / 10

/** Diseñar ventana (equivale a DisenarVentana de la web): elegir pauta, medidas y colores, verla en la pared y agregarla al carrito. */
@Composable
fun PantallaDisenar(abrir: (Pantalla) -> Unit, conCuenta: Boolean = true) {
    var pautas by remember { mutableStateOf<List<PautaCliente>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableIntStateOf(0) }
    var elegidaId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(recarga) {
        try {
            pautas = cargarPautas()
            error = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.mensaje()
        }
    }

    val elegida = pautas?.firstOrNull { it.pauta.pautaId == elegidaId }
    // Configurando una ventana, atrás vuelve a la lista de tipos (en vez de salir de la pantalla).
    BackHandler(enabled = elegida != null) { elegidaId = null }
    when {
        pautas == null && error != null -> EstadoError(error!!) { error = null; recarga++ }
        pautas == null -> EstadoCargando("Cargando el catálogo de ventanas…")
        elegida == null -> SelectorPauta(pautas!!) { elegidaId = it.pauta.pautaId }
        else -> ConfigurarVentana(elegida, alCambiarPauta = { elegidaId = null }, abrir = abrir, conCuenta = conCuenta)
    }
}

/** Catálogo en cuadrícula, con búsqueda y filtros por forma (solo las que hay en el catálogo). */
@Composable
private fun SelectorPauta(pautas: List<PautaCliente>, alElegir: (PautaCliente) -> Unit) {
    var busqueda by rememberSaveable { mutableStateOf("") }
    var forma by rememberSaveable { mutableStateOf<ModeloVentana?>(null) }
    val formas = remember(pautas) { pautas.map { it.modelo }.distinct().sortedBy { it.ordinal } }
    val filtradas = remember(pautas, busqueda, forma) {
        val t = normalizar(busqueda.trim())
        pautas.filter {
            (forma == null || it.modelo == forma) &&
                (t.isEmpty() || normalizar("${it.pauta.nombre} ${it.serie} ${it.pauta.tipoPauta?.nombre.orEmpty()} ${it.modelo.nombre}").contains(t))
        }
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(156.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Elige el tipo de ventana. Después ajustas medidas y colores, y puedes verla en tu pared.", color = Colores.gris)
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    placeholder = { Text("Buscar (corredera, puerta, baño…)") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (formas.size > 1) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { ChipFiltro("Todas", forma == null) { forma = null } }
                        items(formas) { f -> ChipFiltro(f.nombre, forma == f) { forma = if (forma == f) null else f } }
                    }
                }
            }
        }
        if (filtradas.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { EstadoVacio("Sin resultados", "Prueba con otra palabra u otro filtro.") }
        }
        items(filtradas, key = { it.pauta.pautaId ?: it.pauta.nombre }) { p -> TarjetaPauta(p) { alElegir(p) } }
    }
}

@Composable
private fun ChipFiltro(texto: String, elegido: Boolean, alTocar: () -> Unit) {
    FilterChip(
        selected = elegido,
        onClick = alTocar,
        label = { Text(texto) },
        leadingIcon = if (elegido) ({ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }) else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Colores.azulClaro,
            selectedLabelColor = Colores.textoFuerte,
            selectedLeadingIconColor = Colores.azul,
        ),
    )
}

@Composable
private fun TarjetaPauta(p: PautaCliente, alTocar: () -> Unit) {
    Card(
        onClick = alTocar,
        colors = CardDefaults.cardColors(containerColor = Colores.superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        ImagenCatalogo(
            p.pauta.tipoPauta?.rutaImagen,
            tamano = Dp.Unspecified,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(8.dp),
        )
        Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
            Text(p.pauta.nombre.trim(), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(p.modelo.nombre, "Serie ${p.serie}").joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = Colores.gris,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun errorMedida(texto: String, maximoMm: Int): String? {
    val valor = texto.toIntOrNull() ?: return "Ingresa una medida."
    return if (valor < MIN_CM || valor * 10 > maximoMm) "Entre $MIN_CM y ${cm(maximoMm)} cm." else null
}

/**
 * Configurar la ventana elegida: arriba el dibujo (fijo, para ver los cambios), al medio las opciones y abajo,
 * siempre a la vista, el precio y el botón para agregarla al carrito. Las medidas se escriben en centímetros y se
 * guardan en milímetros.
 */
@Composable
private fun ConfigurarVentana(elegida: PautaCliente, alCambiarPauta: () -> Unit, abrir: (Pantalla) -> Unit, conCuenta: Boolean) {
    val context = LocalContext.current
    val alcance = rememberCoroutineScope()
    val pauta = elegida.pauta

    var colores by remember { mutableStateOf<List<ColorVentana>?>(null) }
    var vidrios by remember { mutableStateOf<List<Vidrio>?>(null) }
    var catalogoBasico by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        try {
            colores = Recursos.colores.listar().sortedBy { it.nombre.lowercase() }
            vidrios = Recursos.vidrios.listar().sortedBy { it.nombre.lowercase() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            colores = COLORES_BASICOS
            vidrios = VIDRIOS_BASICOS
            catalogoBasico = true
        }
    }

    var ancho by rememberSaveable { mutableStateOf("120") }
    var alto by rememberSaveable { mutableStateOf("100") }
    var cantidad by rememberSaveable { mutableIntStateOf(1) }
    var color by remember { mutableStateOf<ColorVentana?>(null) }
    var vidrio by remember { mutableStateOf<Vidrio?>(null) }
    var observaciones by rememberSaveable { mutableStateOf("") }
    var validar by remember { mutableStateOf(false) }
    var agregando by remember { mutableStateOf(false) }
    var errorAgregar by remember { mutableStateOf<String?>(null) }
    // Unidades en el carrito tras agregar: mientras no es null, la barra de abajo ofrece ir al carrito.
    var recienAgregada by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(colores, vidrios) {
        if (color == null) color = colores?.firstOrNull()
        if (vidrio == null) vidrio = vidrios?.firstOrNull()
    }
    LaunchedEffect(recienAgregada) {
        if (recienAgregada != null) {
            delay(5000)
            recienAgregada = null
        }
    }

    val limites = limitesDeModelo(elegida.modelo, elegida.hojas)
    val errorAncho = errorMedida(ancho, limites.maxAnchoMm)
    val errorAlto = errorMedida(alto, limites.maxAltoMm)
    val medidasOk = errorAncho == null && errorAlto == null
    val anchoMm = (ancho.toIntOrNull() ?: 0) * 10
    val altoMm = (alto.toIntOrNull() ?: 0) * 10

    // Mientras se escribe, el dibujo usa la medida válida más cercana.
    val config = ConfigVentana(
        anchoMm = anchoMm.coerceIn(MIN_MM, limites.maxAnchoMm),
        altoMm = altoMm.coerceIn(MIN_MM, limites.maxAltoMm),
        hojas = elegida.hojas,
        modelo = elegida.modelo,
        colorMarco = colorMarco(color?.nombre),
        colorVidrio = colorVidrio(vidrio?.nombre),
    )

    // Precio referencial: el backend calcula el costo (igual que en Crear cotización) y se le aplica la ganancia de partida.
    val problemas = remember(pauta) { problemasDePrecio(pauta) }
    var costo by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(pauta.pautaId, ancho, alto, color, vidrio) {
        costo = null
        if (problemas.isNotEmpty() || !medidasOk || color?.colorId == null || vidrio?.vidrioId == null) return@LaunchedEffect
        delay(500)
        costo = try {
            Cotizaciones.cotizarVentana(Ventana(cantidad = 1, ancho = anchoMm, alto = altoMm, color = color, vidrio = vidrio, pauta = pauta)).precioNeto
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
    val neto = costo?.let { (it * factorVenta(GANANCIA_REFERENCIAL, 0)).roundToLong() }
    val conIva = neto?.let { (it * 1.19).roundToLong() }
    val sinPrecio = problemas.isNotEmpty() || catalogoBasico

    // Lo que puede hacer el teléfono, para avisarlo antes de tocar el botón.
    var soporte by remember { mutableStateOf<SoporteAR>(SoporteAR.Verificando) }
    LaunchedEffect(Unit) { soporte = DetectorAR.revisar(context) }

    val conRefuerzo = pauta.isReforzada == true && (anchoMm >= pauta.horizontalReforzada || altoMm >= pauta.verticalReforzada)

    fun agregar() {
        validar = true
        errorAgregar = null
        val c = color
        val v = vidrio
        if (!medidasOk || c == null || v == null || agregando) return
        alcance.launch {
            agregando = true
            try {
                // Sin cuenta se guarda en el teléfono; al entrar pasa al carrito de la cuenta.
                val agregarItem: suspend (ItemVentana) -> List<ItemVentana> = if (conCuenta) ClienteApi::agregar else { item -> CarritoLocal.agregar(item) }
                val carrito = agregarItem(
                    ItemVentana(
                        descripcion = pauta.nombre.trim(),
                        pautaId = pauta.pautaId,
                        serieNombre = elegida.serie,
                        imagenPauta = pauta.tipoPauta?.rutaImagen?.takeIf { it.isNotBlank() },
                        hojas = elegida.hojas,
                        anchoMm = anchoMm,
                        altoMm = altoMm,
                        cantidad = cantidad,
                        colorId = c.colorId,
                        colorNombre = c.nombre,
                        vidrioId = v.vidrioId,
                        vidrioNombre = v.nombre,
                        observaciones = observaciones.trim(),
                    ),
                )
                recienAgregada = carrito.sumOf { it.cantidad }
                validar = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorAgregar = e.mensaje()
            }
            agregando = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        VistaPrevia(config, soporte) { RealidadAumentadaActivity.abrir(context, config, pauta.nombre.trim()) }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Ventana elegida
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(pauta.nombre.trim(), style = MaterialTheme.typography.titleMedium)
                    Text("${elegida.modelo.nombre} · Serie ${elegida.serie}", color = Colores.gris, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = alCambiarPauta) { Text("Cambiar") }
            }
            textoSoporte(soporte)?.let { NotaInfo(it) }
            if (catalogoBasico) MensajeError("No se pudo cargar el catálogo de colores y vidrios; se muestran opciones básicas sin precio.")

            // Medidas
            Seccion("Medidas") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MEDIDAS_RAPIDAS.forEach { m ->
                        val cabe = m.anchoCm * 10 <= limites.maxAnchoMm && m.altoCm * 10 <= limites.maxAltoMm
                        if (cabe) {
                            ChipFiltro("${m.nombre} ${m.anchoCm}×${m.altoCm}", ancho == "${m.anchoCm}" && alto == "${m.altoCm}") {
                                ancho = "${m.anchoCm}"
                                alto = "${m.altoCm}"
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CampoTexto(
                        "Ancho", ancho, { ancho = soloDigitos(it).take(4) }, Modifier.weight(1f),
                        teclado = KeyboardType.Number,
                        sufijo = "cm",
                        error = errorAncho.takeIf { validar || ancho.isNotEmpty() },
                        ayuda = "Máx. ${cm(limites.maxAnchoMm)} cm",
                    )
                    CampoTexto(
                        "Alto", alto, { alto = soloDigitos(it).take(4) }, Modifier.weight(1f),
                        teclado = KeyboardType.Number,
                        sufijo = "cm",
                        error = errorAlto.takeIf { validar || alto.isNotEmpty() },
                        ayuda = "Máx. ${cm(limites.maxAltoMm)} cm",
                    )
                }
                if (pauta.isReforzada == true) {
                    NotaInfo(
                        if (conRefuerzo) "Con estas medidas la ventana lleva perfiles reforzados."
                        else "Desde ${cm(pauta.horizontalReforzada)} cm de ancho o ${cm(pauta.verticalReforzada)} cm de alto lleva perfiles reforzados.",
                        aviso = conRefuerzo,
                    )
                }
            }

            Seccion("Color del marco", color?.nombre) {
                if (colores == null) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                else MuestrasColor(colores!!, color) { color = it }
            }

            Seccion("Vidrio", vidrio?.nombre) {
                if (vidrios == null) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                else OpcionesVidrio(vidrios!!, vidrio) { vidrio = it }
            }

            Seccion("Cantidad") {
                Contador(cantidad, 1..MAX_CANTIDAD) { cantidad = it }
            }

            CampoTexto("Observaciones (opcional)", observaciones, { observaciones = it }, lineas = 2, ayuda = "Ej.: lleva malla mosquitera, va en un segundo piso.")

            Text(
                if (sinPrecio) "La empresa confirma el precio al responder tu solicitud."
                else "Precio referencial con IVA" + (neto?.let { " (neto ${pesos(it)} c/u)" } ?: "") +
                    ". La empresa confirma el precio final al responder tu solicitud.",
                style = MaterialTheme.typography.bodySmall,
                color = Colores.gris,
            )
            errorAgregar?.let { MensajeError(it) }
        }

        BarraPrecio(
            precio = when {
                sinPrecio -> EstadoPrecio.AConfirmar
                !medidasOk -> EstadoPrecio.RevisarMedidas
                conIva == null -> EstadoPrecio.Calculando
                else -> EstadoPrecio.Listo(conIva, cantidad)
            },
            agregando = agregando,
            recienAgregada = recienAgregada,
            alAgregar = ::agregar,
            alVerCarrito = { abrir(Pantalla.CARRITO) },
        )
    }
}

/** El dibujo de la ventana con sus medidas y el botón para verla en la pared. */
@Composable
private fun VistaPrevia(config: ConfigVentana, soporte: SoporteAR, alVerEnPared: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(190.dp)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .background(Colores.superficieTenue, MaterialTheme.shapes.large),
    ) {
        DibujoVentana(
            config,
            Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 52.dp),
        )
        Surface(
            color = Colores.superficie,
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp),
        ) {
            Text(
                medidasCm(config.anchoMm, config.altoMm),
                style = MaterialTheme.typography.labelLarge,
                color = Colores.textoFuerte,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        val simple = soporte is SoporteAR.SoloCamara
        FilledTonalButton(
            onClick = alVerEnPared,
            enabled = soporte !is SoporteAR.SinCamara,
            contentPadding = PaddingValues(horizontal = 14.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .height(36.dp),
        ) {
            Icon(if (simple) Icons.Outlined.CameraAlt else Icons.Outlined.ViewInAr, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(" Ver en mi pared", style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Explicación de lo que puede hacer el teléfono con la realidad aumentada, cuando hay algo que avisar. */
private fun textoSoporte(soporte: SoporteAR): String? = when (soporte) {
    SoporteAR.Verificando -> null
    is SoporteAR.Completa -> if (soporte.liviana) "Tu teléfono verá la ventana en la pared en modo liviano, a tamaño real." else null
    is SoporteAR.InstalarArcore -> "Para verla en tu pared te pediremos instalar un componente gratuito de Google."
    is SoporteAR.SoloCamara -> "Tu teléfono no tiene realidad aumentada: verás la ventana sobre la cámara con tamaño aproximado."
    is SoporteAR.SinCamara -> soporte.motivo
}

@Composable
private fun Seccion(titulo: String, elegido: String? = null, contenido: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(titulo, style = MaterialTheme.typography.titleSmall, color = Colores.textoFuerte)
            elegido?.let { Text(" · $it", style = MaterialTheme.typography.bodyMedium, color = Colores.gris, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        contenido()
    }
}

@Composable
private fun NotaInfo(texto: String, aviso: Boolean = false) {
    Surface(color = if (aviso) Colores.avisoFondo else Colores.azulClaro, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = if (aviso) Colores.aviso else Colores.azul, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(texto, style = MaterialTheme.typography.bodySmall, color = if (aviso) Colores.avisoTexto else Colores.texto)
        }
    }
}

/** Círculos con el color de cada marco; el elegido lleva un anillo y un visto. */
@Composable
private fun MuestrasColor(colores: List<ColorVentana>, elegido: ColorVentana?, alElegir: (ColorVentana) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        items(colores, key = { it.colorId ?: it.nombre }) { c ->
            val activo = c == elegido
            val tono = Color(0xFF000000 or colorMarco(c.nombre).toLong())
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(76.dp)
                    .clickable(role = Role.RadioButton) { alElegir(c) }
                    .semantics { contentDescription = "Color ${c.nombre}" + if (activo) ", elegido" else "" },
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .border(2.dp, if (activo) Colores.azul else Color.Transparent, CircleShape)
                        .padding(4.dp)
                        .background(tono, CircleShape)
                        .border(1.dp, Colores.borde, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (activo) {
                        // El visto contrasta con el color del marco (claro u oscuro).
                        val claro = (tono.red * 0.299f + tono.green * 0.587f + tono.blue * 0.114f) > 0.6f
                        Icon(Icons.Filled.Check, contentDescription = null, tint = if (claro) Color(0xFF1A2332) else Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                Text(
                    c.nombre.trim(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (activo) Colores.textoFuerte else Colores.gris,
                    fontWeight = if (activo) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** Fichas de vidrio con un cuadrito de su tono. */
@Composable
private fun OpcionesVidrio(vidrios: List<Vidrio>, elegido: Vidrio?, alElegir: (Vidrio) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        vidrios.forEach { v ->
            val activo = v == elegido
            FilterChip(
                selected = activo,
                onClick = { alElegir(v) },
                label = { Text(v.nombre.trim(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = {
                    Box(
                        Modifier
                            .size(16.dp)
                            .background(Color(0xFF000000 or colorVidrio(v.nombre).toLong()), RoundedCornerShape(4.dp))
                            .border(1.dp, Colores.borde, RoundedCornerShape(4.dp)),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Colores.azulClaro, selectedLabelColor = Colores.textoFuerte),
            )
        }
    }
}

/** Botones − y + con el número al medio. */
@Composable
private fun Contador(valor: Int, rango: IntRange, alCambiar: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = { alCambiar(valor - 1) }, enabled = valor > rango.first) {
            Icon(Icons.Filled.Remove, contentDescription = "Una menos")
        }
        Text(
            "$valor",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(56.dp),
        )
        FilledTonalIconButton(onClick = { alCambiar(valor + 1) }, enabled = valor < rango.last) {
            Icon(Icons.Filled.Add, contentDescription = "Una más")
        }
        Text(if (valor == 1) "  ventana" else "  ventanas", color = Colores.gris)
    }
}

private sealed interface EstadoPrecio {
    data object AConfirmar : EstadoPrecio
    data object RevisarMedidas : EstadoPrecio
    data object Calculando : EstadoPrecio
    data class Listo(val unitario: Long, val cantidad: Int) : EstadoPrecio
}

/** Barra fija de abajo: el precio y el botón para agregar; recién agregada, el aviso y el acceso al carrito. */
@Composable
private fun BarraPrecio(
    precio: EstadoPrecio,
    agregando: Boolean,
    recienAgregada: Int?,
    alAgregar: () -> Unit,
    alVerCarrito: () -> Unit,
) {
    Surface(color = Colores.superficie) {
        Column {
            HorizontalDivider(color = Colores.borde)
            AnimatedContent(recienAgregada, label = "barraPrecio") { unidades ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (unidades != null) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Colores.exito)
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(start = 10.dp),
                        ) {
                            Text("¡Agregada al carrito!", style = MaterialTheme.typography.titleSmall, color = Colores.textoFuerte)
                            Text("Llevas $unidades ${if (unidades == 1) "ventana" else "ventanas"}", style = MaterialTheme.typography.bodySmall, color = Colores.gris)
                        }
                        Button(onClick = alVerCarrito, modifier = Modifier.height(48.dp)) { Text("Ver carrito") }
                    } else {
                        Column(Modifier.weight(1f)) {
                            when (precio) {
                                EstadoPrecio.AConfirmar -> Text("Precio a confirmar", style = MaterialTheme.typography.titleMedium, color = Colores.textoFuerte)
                                EstadoPrecio.RevisarMedidas -> Text("Revisa las medidas", style = MaterialTheme.typography.titleMedium, color = Colores.error)
                                EstadoPrecio.Calculando -> Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Text("  Calculando precio…", style = MaterialTheme.typography.bodyMedium, color = Colores.gris)
                                }
                                is EstadoPrecio.Listo -> {
                                    Text(pesos(precio.unitario * precio.cantidad), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Colores.textoFuerte)
                                    Text(
                                        if (precio.cantidad > 1) "${precio.cantidad} × ${pesos(precio.unitario)} · con IVA" else "Referencial, con IVA",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Colores.gris,
                                    )
                                }
                            }
                        }
                        Button(onClick = alAgregar, enabled = !agregando, modifier = Modifier.height(48.dp)) {
                            if (agregando) {
                                CircularProgressIndicator(Modifier.size(20.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.AddShoppingCart, contentDescription = null, modifier = Modifier.size(20.dp))
                                Text("  Agregar")
                            }
                        }
                    }
                }
            }
        }
    }
}
