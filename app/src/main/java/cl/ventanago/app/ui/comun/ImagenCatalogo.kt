package cl.ventanago.app.ui.comun

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cl.ventanago.app.datos.Imagenes
import cl.ventanago.app.datos.mensaje
import cl.ventanago.app.ui.Colores
import coil3.compose.SubcomposeAsyncImage

/** Imagen del catálogo a partir de su ruta relativa; si no hay o no carga, muestra un ícono. */
@Composable
fun ImagenCatalogo(ruta: String?, modifier: Modifier = Modifier, tamano: Dp = 56.dp) {
    val url = Imagenes.url(ruta)
    Box(
        modifier
            .size(tamano)
            .clip(RoundedCornerShape(8.dp))
            .background(Colores.fondo)
            .border(1.dp, Colores.borde, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (url == null) {
            Icon(Icons.Outlined.Image, contentDescription = null, tint = Colores.borde)
        } else {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                loading = { CircularProgressIndicator(Modifier.padding(tamano / 4), strokeWidth = 2.dp) },
                error = { Icon(Icons.Outlined.ImageNotSupported, contentDescription = null, tint = Colores.gris) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Campo para elegir una de las imágenes de catálogo publicadas en el front (pautas/imagenes.json). */
@Composable
fun SelectorImagen(ruta: String?, alElegir: (String?) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text("Imagen", style = MaterialTheme.typography.labelLarge, color = Colores.textoSecundario)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ImagenCatalogo(ruta, tamano = 88.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    ruta?.substringAfterLast('/') ?: "Sin imagen",
                    style = MaterialTheme.typography.bodySmall,
                    color = Colores.gris,
                    maxLines = 2,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { abierto = true }) { Text(if (ruta == null) "Elegir" else "Cambiar") }
                    if (ruta != null) TextButton(onClick = { alElegir(null) }) { Text("Quitar") }
                }
            }
        }
    }
    if (abierto) {
        DialogoImagenes(
            actual = ruta,
            alElegir = {
                alElegir(it)
                abierto = false
            },
            alCerrar = { abierto = false },
        )
    }
}

@Composable
private fun DialogoImagenes(actual: String?, alElegir: (String) -> Unit, alCerrar: () -> Unit) {
    var rutas by remember { mutableStateOf<List<String>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try {
            rutas = Imagenes.disponibles()
        } catch (e: Exception) {
            error = e.mensaje()
        }
    }
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Elegir imagen") },
        text = {
            Box(Modifier.heightIn(min = 120.dp, max = 460.dp), contentAlignment = Alignment.Center) {
                when {
                    error != null -> MensajeError(error!!)
                    rutas == null -> CircularProgressIndicator()
                    else -> LazyVerticalGrid(GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(rutas!!) { ruta ->
                            val elegida = ruta == actual
                            Box(
                                Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        BorderStroke(if (elegida) 3.dp else 1.dp, if (elegida) Colores.azul else Colores.borde),
                                        RoundedCornerShape(8.dp),
                                    )
                                    .clickable { alElegir(ruta) },
                            ) {
                                SubcomposeAsyncImage(
                                    model = Imagenes.url(ruta),
                                    contentDescription = ruta.substringAfterLast('/'),
                                    contentScale = ContentScale.Crop,
                                    loading = { Box(Modifier.background(Colores.fondo)) },
                                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                                )
                                if (elegida) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = "Elegida",
                                        tint = Colores.azul,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = alCerrar) { Text("Cerrar") } },
    )
}
