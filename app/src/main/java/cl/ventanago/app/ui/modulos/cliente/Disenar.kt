package cl.ventanago.app.ui.modulos.cliente

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import cl.ventanago.app.ui.comun.LocalAvisos
import cl.ventanago.app.ui.comun.MensajeError
import cl.ventanago.app.ui.comun.Selector
import cl.ventanago.app.ui.comun.normalizar
import cl.ventanago.app.ui.modulos.soloDigitos
import cl.ventanago.app.ui.navegacion.Pantalla
import cl.ventanago.app.ventana.ConfigVentana
import cl.ventanago.app.ventana.DibujoVentana
import cl.ventanago.app.ventana.MAX_CANTIDAD
import cl.ventanago.app.ventana.MIN_MM
import cl.ventanago.app.ventana.ModeloVentana
import cl.ventanago.app.ventana.colorMarco
import cl.ventanago.app.ventana.colorVidrio
import cl.ventanago.app.ventana.hojasDePauta
import cl.ventanago.app.ventana.limitesDeModelo
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
    when {
        pautas == null && error != null -> EstadoError(error!!) { error = null; recarga++ }
        pautas == null -> EstadoCargando("Cargando el catálogo de ventanas…")
        elegida == null -> SelectorPauta(pautas!!) { elegidaId = it.pauta.pautaId }
        else -> ConfigurarVentana(elegida, alCambiarPauta = { elegidaId = null }, abrir = abrir, conCuenta = conCuenta)
    }
}

@Composable
private fun SelectorPauta(pautas: List<PautaCliente>, alElegir: (PautaCliente) -> Unit) {
    var busqueda by remember { mutableStateOf("") }
    val filtradas = remember(pautas, busqueda) {
        val t = normalizar(busqueda.trim())
        pautas.filter { t.isEmpty() || normalizar("${it.pauta.nombre} ${it.serie} ${it.pauta.tipoPauta?.nombre.orEmpty()}").contains(t) }
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text("Diseña tu ventana", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Elige el tipo de ventana. Después ajustas medidas y colores, y puedes verla en tu pared.", color = Colores.gris)
            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                placeholder = { Text("Buscar (corredera, puerta, baño…)") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
        }
        if (filtradas.isEmpty()) item { EstadoVacio("Sin resultados", "Prueba con otra palabra.") }
        filtradas.groupBy { it.serie }.forEach { (serie, delaSerie) ->
            item {
                Text("Serie $serie", style = MaterialTheme.typography.titleSmall, color = Colores.textoSecundario, modifier = Modifier.padding(top = 8.dp))
            }
            items(delaSerie, key = { it.pauta.pautaId ?: it.pauta.nombre }) { p ->
                Card(
                    onClick = { alElegir(p) },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        ImagenCatalogo(p.pauta.tipoPauta?.rutaImagen, tamano = 64.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.pauta.nombre.trim(), fontWeight = FontWeight.SemiBold)
                            Text(
                                listOfNotNull(p.pauta.tipoPauta?.nombre, p.pauta.descripcion?.takeIf { it.isNotBlank() }).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = Colores.gris,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun errorMedida(texto: String, maximo: Int): String? {
    val valor = texto.toIntOrNull() ?: return "Ingresa una medida."
    return if (valor < MIN_MM || valor > maximo) "Entre $MIN_MM y $maximo mm." else null
}

@Composable
private fun ConfigurarVentana(elegida: PautaCliente, alCambiarPauta: () -> Unit, abrir: (Pantalla) -> Unit, conCuenta: Boolean) {
    val context = LocalContext.current
    val avisar = LocalAvisos.current
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

    var ancho by rememberSaveable { mutableStateOf("1200") }
    var alto by rememberSaveable { mutableStateOf("1000") }
    var cantidad by rememberSaveable { mutableStateOf("1") }
    var color by remember { mutableStateOf<ColorVentana?>(null) }
    var vidrio by remember { mutableStateOf<Vidrio?>(null) }
    var observaciones by rememberSaveable { mutableStateOf("") }
    var validar by remember { mutableStateOf(false) }
    var agregando by remember { mutableStateOf(false) }
    var errorAgregar by remember { mutableStateOf<String?>(null) }
    var enCarrito by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(colores, vidrios) {
        if (color == null) color = colores?.firstOrNull()
        if (vidrio == null) vidrio = vidrios?.firstOrNull()
    }

    val limites = limitesDeModelo(elegida.modelo, elegida.hojas)
    val errorAncho = errorMedida(ancho, limites.maxAnchoMm)
    val errorAlto = errorMedida(alto, limites.maxAltoMm)
    val errorCantidad = (cantidad.toIntOrNull() ?: 0).let { if (it in 1..MAX_CANTIDAD) null else "Entre 1 y $MAX_CANTIDAD." }
    val medidasOk = errorAncho == null && errorAlto == null

    // Mientras se escribe, el dibujo usa la medida válida más cercana.
    val config = ConfigVentana(
        anchoMm = (ancho.toIntOrNull() ?: MIN_MM).coerceIn(MIN_MM, limites.maxAnchoMm),
        altoMm = (alto.toIntOrNull() ?: MIN_MM).coerceIn(MIN_MM, limites.maxAltoMm),
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
            Cotizaciones.cotizarVentana(Ventana(cantidad = 1, ancho = ancho.toInt(), alto = alto.toInt(), color = color, vidrio = vidrio, pauta = pauta)).precioNeto
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    // Lo que puede hacer el teléfono, para avisarlo antes de tocar el botón.
    var soporte by remember { mutableStateOf<SoporteAR>(SoporteAR.Verificando) }
    LaunchedEffect(Unit) { soporte = DetectorAR.revisar(context) }

    val conRefuerzo = pauta.isReforzada == true &&
        ((ancho.toIntOrNull() ?: 0) >= pauta.horizontalReforzada || (alto.toIntOrNull() ?: 0) >= pauta.verticalReforzada)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = alCambiarPauta) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Text(" Cambiar tipo de ventana")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            ImagenCatalogo(pauta.tipoPauta?.rutaImagen, tamano = 56.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(pauta.nombre.trim(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Serie ${elegida.serie}", color = Colores.gris, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (catalogoBasico) MensajeError("No se pudo cargar el catálogo de colores y vidrios; se muestran opciones básicas sin precio.")

        // Vista previa y realidad aumentada
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Colores.borde), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .background(Color(0xFFEFF2F6), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    DibujoVentana(config, Modifier.fillMaxSize().padding(12.dp))
                }
                Text("${config.anchoMm} × ${config.altoMm} mm", color = Colores.gris, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { RealidadAumentadaActivity.abrir(context, config, pauta.nombre.trim()) },
                    enabled = soporte !is SoporteAR.SinCamara,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(if (soporte is SoporteAR.SoloCamara) Icons.Outlined.CameraAlt else Icons.Outlined.ViewInAr, contentDescription = null)
                    Text(if (soporte is SoporteAR.SoloCamara) "  Ver en mi pared (vista simple)" else "  Ver en mi pared")
                }
                Text(
                    when (val s = soporte) {
                        SoporteAR.Verificando -> "Revisando tu teléfono…"
                        is SoporteAR.Completa -> if (s.liviana) "Realidad aumentada en modo liviano, a tamaño real." else "Realidad aumentada: la ventana a tamaño real sobre tu pared."
                        is SoporteAR.InstalarArcore -> "Tu teléfono es compatible; te pediremos instalar un componente gratuito de Google."
                        is SoporteAR.SoloCamara -> "Tu teléfono no tiene realidad aumentada: verás la ventana sobre la cámara con tamaño aproximado."
                        is SoporteAR.SinCamara -> s.motivo
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Colores.gris,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        if (pauta.isReforzada == true) {
            Surface(color = if (conRefuerzo) Color(0xFFFFF4E5) else Colores.azulClaro, shape = RoundedCornerShape(10.dp)) {
                Text(
                    if (conRefuerzo) "Con estas medidas la ventana lleva perfiles reforzados."
                    else "Desde ${pauta.horizontalReforzada} mm de ancho o ${pauta.verticalReforzada} mm de alto lleva perfiles reforzados.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoTexto(
                "Ancho (mm)", ancho, { ancho = soloDigitos(it).take(5) }, Modifier.weight(1f),
                teclado = KeyboardType.Number,
                error = errorAncho.takeIf { validar || ancho.isNotEmpty() },
                ayuda = "Máx. ${limites.maxAnchoMm}",
            )
            CampoTexto(
                "Alto (mm)", alto, { alto = soloDigitos(it).take(5) }, Modifier.weight(1f),
                teclado = KeyboardType.Number,
                error = errorAlto.takeIf { validar || alto.isNotEmpty() },
                ayuda = "Máx. ${limites.maxAltoMm}",
            )
        }
        Selector("Color del marco", colores.orEmpty(), color, texto = { it.nombre }, alElegir = { color = it }, cargando = colores == null)
        Selector("Vidrio", vidrios.orEmpty(), vidrio, texto = { it.nombre }, alElegir = { vidrio = it }, cargando = vidrios == null)
        CampoTexto("Cantidad", cantidad, { cantidad = soloDigitos(it).take(2) }, teclado = KeyboardType.Number, error = errorCantidad.takeIf { validar })
        CampoTexto("Observaciones (opcional)", observaciones, { observaciones = it }, lineas = 2)

        // Precio referencial
        Surface(color = Colores.azulClaro, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Precio referencial", style = MaterialTheme.typography.labelLarge, color = Colores.azul)
                when {
                    problemas.isNotEmpty() || catalogoBasico -> Text("Precio a confirmar por la empresa.", style = MaterialTheme.typography.bodyMedium)
                    !medidasOk -> Text("Ingresa medidas válidas para calcularlo.", style = MaterialTheme.typography.bodySmall)
                    costo == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("  Calculando…", style = MaterialTheme.typography.bodySmall)
                    }
                    else -> {
                        val unidades = (cantidad.toIntOrNull() ?: 1).coerceAtLeast(1)
                        val neto = (costo!! * factorVenta(GANANCIA_REFERENCIAL, 0)).roundToLong()
                        val conIva = (neto * 1.19).roundToLong()
                        Text("${pesos(conIva)} c/u con IVA", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Colores.menuFondo)
                        if (unidades > 1) Text("${pesos(conIva * unidades)} por $unidades unidades", style = MaterialTheme.typography.bodyMedium)
                        Text("Neto ${pesos(neto)} + IVA. La empresa confirma el precio final al responder tu solicitud.", style = MaterialTheme.typography.bodySmall, color = Colores.textoSecundario)
                    }
                }
            }
        }

        errorAgregar?.let { MensajeError(it) }
        Button(
            onClick = {
                validar = true
                errorAgregar = null
                val c = color
                val v = vidrio
                if (!medidasOk || errorCantidad != null || c == null || v == null || agregando) return@Button
                alcance.launch {
                    agregando = true
                    try {
                        // Sin cuenta se guarda en el teléfono; al entrar pasa al carrito de la cuenta.
                        val agregar: suspend (ItemVentana) -> List<ItemVentana> = if (conCuenta) ClienteApi::agregar else { item -> CarritoLocal.agregar(item) }
                        val carrito = agregar(
                            ItemVentana(
                                descripcion = pauta.nombre.trim(),
                                pautaId = pauta.pautaId,
                                serieNombre = elegida.serie,
                                imagenPauta = pauta.tipoPauta?.rutaImagen?.takeIf { it.isNotBlank() },
                                hojas = elegida.hojas,
                                anchoMm = ancho.toInt(),
                                altoMm = alto.toInt(),
                                cantidad = cantidad.toInt(),
                                colorId = c.colorId,
                                colorNombre = c.nombre,
                                vidrioId = v.vidrioId,
                                vidrioNombre = v.nombre,
                                observaciones = observaciones.trim(),
                            ),
                        )
                        enCarrito = carrito.size
                        avisar("Ventana agregada al carrito")
                        validar = false
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        errorAgregar = e.mensaje()
                    }
                    agregando = false
                }
            },
            enabled = !agregando,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
        ) {
            if (agregando) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            else {
                Icon(Icons.Filled.AddShoppingCart, contentDescription = null)
                Text("  Agregar al carrito")
            }
        }
        enCarrito?.let { n ->
            OutlinedButton(onClick = { abrir(Pantalla.CARRITO) }, modifier = Modifier.fillMaxWidth()) {
                Text("Ver carrito ($n ${if (n == 1) "ventana" else "ventanas"}) y pedir cotización")
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
