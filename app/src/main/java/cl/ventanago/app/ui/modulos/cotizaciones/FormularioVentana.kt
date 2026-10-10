package cl.ventanago.app.ui.modulos.cotizaciones

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.ventanago.app.datos.ColorVentana
import cl.ventanago.app.datos.Cotizaciones
import cl.ventanago.app.datos.Pauta
import cl.ventanago.app.datos.Recursos
import cl.ventanago.app.datos.Serie
import cl.ventanago.app.datos.Ventana
import cl.ventanago.app.datos.Vidrio
import cl.ventanago.app.datos.mensaje
import cl.ventanago.app.datos.pesos
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.comun.CampoTexto
import cl.ventanago.app.ui.comun.FormularioPantalla
import cl.ventanago.app.ui.comun.ImagenCatalogo
import cl.ventanago.app.ui.comun.Selector
import cl.ventanago.app.ui.comun.ejecutar
import cl.ventanago.app.ui.comun.rememberEstadoGuardado
import cl.ventanago.app.ui.modulos.REQUERIDO
import cl.ventanago.app.ui.modulos.rememberOpciones
import cl.ventanago.app.ui.modulos.soloDigitos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.math.roundToLong

/**
 * Agrega una ventana a la cotización (equivale a ListaModelos + FormularioProducto de la web):
 * serie → pauta → medidas, color y vidrio, con el precio calculado por el backend mientras se escribe.
 */
@Composable
fun FormularioVentana(factorVenta: Double, alGuardar: suspend (Ventana) -> Unit, alCerrar: () -> Unit) {
    val estado = rememberEstadoGuardado()
    val alcance = rememberCoroutineScope()
    val series = rememberOpciones({ Recursos.series.listar().sortedBy { it.nombre.lowercase() } }) { estado.error = it }
    val colores = rememberOpciones({ Recursos.colores.listar().sortedBy { it.nombre.lowercase() } }) { estado.error = it }
    val vidrios = rememberOpciones({ Recursos.vidrios.listar().sortedBy { it.nombre.lowercase() } }) { estado.error = it }

    var serie by remember { mutableStateOf<Serie?>(null) }
    var pautas by remember { mutableStateOf<List<Pauta>?>(null) }
    var pauta by remember { mutableStateOf<Pauta?>(null) }
    var item by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("1") }
    var ancho by remember { mutableStateOf("") }
    var alto by remember { mutableStateOf("") }
    var color by remember { mutableStateOf<ColorVentana?>(null) }
    var vidrio by remember { mutableStateOf<Vidrio?>(null) }
    var observaciones by remember { mutableStateOf("") }
    var validar by remember { mutableStateOf(false) }

    LaunchedEffect(serie?.serieId) {
        val id = serie?.serieId ?: return@LaunchedEffect
        pautas = null
        pauta = null
        pautas = try {
            Cotizaciones.pautasDeSerie(id).sortedBy { it.nombre.lowercase() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            estado.error = e.mensaje()
            emptyList()
        }
    }

    fun armar() = Ventana(
        descripcion = item.trim().ifBlank { pauta?.nombre.orEmpty() },
        cantidad = cantidad.toIntOrNull() ?: 0,
        ancho = ancho.toIntOrNull() ?: 0,
        alto = alto.toIntOrNull() ?: 0,
        observaciones = observaciones.trim(),
        color = color,
        vidrio = vidrio,
        pauta = pauta,
    )

    // Precio estimado: se pide al backend (mismo cálculo con el que se guardará) al dejar de escribir.
    var costo by remember { mutableStateOf<Long?>(null) }
    var calculando by remember { mutableStateOf(false) }
    val completa = pauta != null && color != null && vidrio != null && (ancho.toIntOrNull() ?: 0) > 0 && (alto.toIntOrNull() ?: 0) > 0
    LaunchedEffect(pauta?.pautaId, color?.colorId, vidrio?.vidrioId, ancho, alto) {
        costo = null
        if (!completa) return@LaunchedEffect
        delay(600)
        calculando = true
        costo = try {
            Cotizaciones.cotizarVentana(armar()).precioNeto
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        calculando = false
    }

    val reforzadaAncho = pauta?.isReforzada == true && (ancho.toIntOrNull() ?: 0) >= (pauta?.horizontalReforzada ?: Int.MAX_VALUE)
    val reforzadaAlto = pauta?.isReforzada == true && (alto.toIntOrNull() ?: 0) >= (pauta?.verticalReforzada ?: Int.MAX_VALUE)

    FormularioPantalla(
        titulo = "Agregar ventana",
        guardando = estado.guardando,
        error = estado.error,
        textoGuardar = "Agregar",
        alCerrar = alCerrar,
        alGuardar = {
            validar = true
            if (!completa || (cantidad.toIntOrNull() ?: 0) <= 0) {
                estado.error = "Completa la pauta, las medidas, el color, el vidrio y la cantidad."
                return@FormularioPantalla
            }
            estado.ejecutar(alcance, { alGuardar(armar()) }) { alCerrar() }
        },
    ) {
        Selector(
            "Serie", series.orEmpty(), serie,
            texto = { it.nombre },
            alElegir = { serie = it },
            cargando = series == null,
            error = REQUERIDO.takeIf { validar && serie == null },
        )

        if (serie != null) {
            Text("Pauta (producto)", style = MaterialTheme.typography.labelLarge, color = if (validar && pauta == null) Colores.error else Colores.textoSecundario)
            when {
                pautas == null -> CircularProgressIndicator(Modifier.size(28.dp))
                pautas!!.isEmpty() -> Text("Esta serie no tiene pautas.", color = Colores.gris)
                else -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    pautas!!.forEach { p ->
                        val elegida = p.pautaId == pauta?.pautaId
                        Surface(
                            color = if (elegida) Colores.azulClaro else Color.White,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(if (elegida) 2.dp else 1.dp, if (elegida) Colores.azul else Colores.borde),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pauta = p },
                        ) {
                            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                ImagenCatalogo(p.tipoPauta?.rutaImagen, tamano = 48.dp)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(p.nombre, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        listOfNotNull(p.tipoPauta?.nombre, p.descripcion?.takeIf { it.isNotBlank() }).joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Colores.gris,
                                    )
                                }
                                if (elegida) Icon(Icons.Filled.CheckCircle, contentDescription = "Elegida", tint = Colores.azul)
                            }
                        }
                    }
                }
            }
        }

        if (pauta != null) {
            if (pauta?.isReforzada == true) {
                Text(
                    "Se refuerza desde ${pauta!!.horizontalReforzada} mm de ancho y ${pauta!!.verticalReforzada} mm de alto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8A5300),
                )
            }
            CampoTexto("Ítem (dónde va)", item, { item = it }, ayuda = "Ej: Living, Dormitorio 1. Si lo dejas vacío se usa el nombre de la pauta.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoTexto(
                    "Ancho", ancho, { ancho = soloDigitos(it).take(5) }, Modifier.weight(1f),
                    teclado = KeyboardType.Number, prefijo = null,
                    error = REQUERIDO.takeIf { validar && (ancho.toIntOrNull() ?: 0) <= 0 },
                    ayuda = if (reforzadaAncho) "mm · reforzada" else "mm",
                )
                CampoTexto(
                    "Alto", alto, { alto = soloDigitos(it).take(5) }, Modifier.weight(1f),
                    teclado = KeyboardType.Number,
                    error = REQUERIDO.takeIf { validar && (alto.toIntOrNull() ?: 0) <= 0 },
                    ayuda = if (reforzadaAlto) "mm · reforzada" else "mm",
                )
            }
            CampoTexto(
                "Cantidad", cantidad, { cantidad = soloDigitos(it).take(4) },
                teclado = KeyboardType.Number,
                error = REQUERIDO.takeIf { validar && (cantidad.toIntOrNull() ?: 0) <= 0 },
            )
            Selector(
                "Color", colores.orEmpty(), color, texto = { "${it.nombre} · ${pesos(it.valor)}/kg" },
                alElegir = { color = it }, cargando = colores == null,
                error = REQUERIDO.takeIf { validar && color == null },
            )
            Selector(
                "Vidrio", vidrios.orEmpty(), vidrio, texto = { "${it.nombre} · ${pesos(it.valor)}/m²" },
                alElegir = { vidrio = it }, cargando = vidrios == null,
                error = REQUERIDO.takeIf { validar && vidrio == null },
            )
            CampoTexto("Observaciones", observaciones, { observaciones = it }, lineas = 2)

            Surface(color = Colores.azulClaro, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Precio estimado", style = MaterialTheme.typography.labelLarge, color = Colores.azul)
                    val unidades = (cantidad.toIntOrNull() ?: 0).coerceAtLeast(1)
                    when {
                        !completa -> Text("Completa medidas, color y vidrio para calcularlo.", style = MaterialTheme.typography.bodySmall, color = Colores.textoSecundario)
                        calculando || costo == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("  Calculando…", style = MaterialTheme.typography.bodySmall)
                        }
                        else -> {
                            val venta = (costo!! * factorVenta).roundToLong()
                            Text(
                                "${pesos(venta)} c/u · ${pesos(venta * unidades)} en total",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Colores.menuFondo,
                            )
                            Text("Costo de fabricación: ${pesos(costo!!)} c/u (sin IVA)", style = MaterialTheme.typography.bodySmall, color = Colores.textoSecundario)
                        }
                    }
                }
            }
        }
    }
}
