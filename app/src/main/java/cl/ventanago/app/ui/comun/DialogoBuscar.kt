package cl.ventanago.app.ui.comun

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.ventanago.app.ui.Colores

/** Lista con buscador para elegir un elemento (equivale a los Autocomplete del front web). */
@Composable
fun <T> DialogoBuscar(
    titulo: String,
    opciones: List<T>,
    texto: (T) -> String,
    detalle: (T) -> String? = { null },
    alElegir: (T) -> Unit,
    alCerrar: () -> Unit,
    vacio: String = "No hay opciones disponibles.",
) {
    var busqueda by remember { mutableStateOf("") }
    val filtradas = remember(opciones, busqueda) {
        val termino = normalizar(busqueda.trim())
        opciones.filter { termino.isEmpty() || normalizar(texto(it) + " " + detalle(it).orEmpty()).contains(termino) }
    }
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(titulo) },
        text = {
            Column {
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    placeholder = { Text("Buscar") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(Modifier.heightIn(min = 80.dp, max = 380.dp), contentAlignment = Alignment.Center) {
                    if (filtradas.isEmpty()) {
                        Text(if (opciones.isEmpty()) vacio else "Sin resultados.", color = Colores.gris, modifier = Modifier.padding(16.dp))
                    } else {
                        LazyColumn {
                            items(filtradas) { opcion ->
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { alElegir(opcion) }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                ) {
                                    Text(texto(opcion), fontWeight = FontWeight.SemiBold)
                                    detalle(opcion)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Colores.gris) }
                                }
                                HorizontalDivider(color = Colores.borde)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = alCerrar) { Text("Cerrar") } },
    )
}
