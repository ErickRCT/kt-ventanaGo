package cl.ventanago.app.ui.comun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.ventanago.app.datos.mensaje
import cl.ventanago.app.ui.Colores
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.Normalizer

/**
 * Pantalla de mantenedor (equivale a las tablas CRUD del front web): título, buscador, lista en tarjetas,
 * botón para crear, edición al tocar una tarjeta y eliminación con confirmación.
 *
 * [formulario] recibe el registro a editar (null = nuevo) y una función para cerrarlo: con un mensaje
 * cuando se guardó (se muestra y la lista se recarga) o con null si se canceló.
 *
 * [soloLectura]: solo se consulta (sin crear, editar ni eliminar), como la empresa con el catálogo de VentanaGo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Mantenedor(
    titulo: String,
    descripcion: String,
    cargar: suspend () -> List<T>,
    clave: (T) -> Any,
    textoBusqueda: (T) -> String,
    nombre: (T) -> String,
    textoNuevo: String?,
    formulario: @Composable (item: T?, alTerminar: (String?) -> Unit) -> Unit,
    eliminar: (suspend (T) -> Unit)? = null,
    editable: Boolean = true,
    soloLectura: Boolean = false,
    fila: @Composable RowScope.(T) -> Unit,
) {
    @Suppress("NAME_SHADOWING") val textoNuevo = textoNuevo.takeUnless { soloLectura }
    @Suppress("NAME_SHADOWING") val eliminar = eliminar.takeUnless { soloLectura }
    @Suppress("NAME_SHADOWING") val editable = editable && !soloLectura
    val avisar = LocalAvisos.current
    val alcance = rememberCoroutineScope()

    var items by remember { mutableStateOf<List<T>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var refrescando by remember { mutableStateOf(false) }
    var recarga by remember { mutableIntStateOf(0) }
    var busqueda by remember { mutableStateOf("") }

    var formularioAbierto by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<T?>(null) }
    var aEliminar by remember { mutableStateOf<T?>(null) }
    var eliminando by remember { mutableStateOf(false) }

    LaunchedEffect(recarga) {
        try {
            items = cargar()
            error = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (items == null) error = e.mensaje() else avisar(e.mensaje())
        }
        refrescando = false
    }

    val filtrados = remember(items, busqueda) {
        val termino = normalizar(busqueda.trim())
        items.orEmpty().filter { termino.isEmpty() || normalizar(textoBusqueda(it)).contains(termino) }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Spacer(Modifier.weight(1f))
                    items?.let {
                        Text(
                            if (busqueda.isBlank()) "${it.size} registros" else "${filtrados.size} de ${it.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Colores.gris,
                        )
                    }
                }
                Text(descripcion, style = MaterialTheme.typography.bodyMedium, color = Colores.gris)
                if (soloLectura) {
                    Text(
                        "Solo consulta: este catálogo lo administra VentanaGo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Colores.textoSecundario,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    placeholder = { Text("Buscar en ${titulo.lowercase()}") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (busqueda.isNotEmpty()) {
                            IconButton(onClick = { busqueda = "" }) { Icon(Icons.Filled.Clear, contentDescription = "Limpiar") }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Colores.superficie,
                        focusedContainerColor = Colores.superficie,
                        unfocusedBorderColor = Colores.borde,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
            }

            when {
                items == null && error != null -> EstadoError(error!!) {
                    error = null
                    recarga++
                }
                items == null -> EsqueletoLista("Cargando…", filas = 6, conImagen = false)
                else -> PullToRefreshBox(
                    isRefreshing = refrescando,
                    onRefresh = {
                        refrescando = true
                        recarga++
                    },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (filtrados.isEmpty()) {
                        // Dentro de una lista para que el gesto de recargar funcione también sin registros.
                        LazyColumn(Modifier.fillMaxSize()) {
                            item {
                                if (busqueda.isBlank()) EstadoVacio("Aún no hay registros", textoNuevo?.let { "Usa \"$it\" para agregar el primero." })
                                else EstadoVacio("Sin resultados", "Ningún registro coincide con \"$busqueda\".")
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(filtrados, key = clave) { item ->
                                Card(
                                    onClick = {
                                        if (editable) {
                                            editando = item
                                            formularioAbierto = true
                                        }
                                    },
                                    enabled = editable,
                                    colors = CardDefaults.cardColors(containerColor = Colores.superficie, disabledContainerColor = Colores.superficie),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Row(
                                        Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        fila(item)
                                        if (editable) {
                                            IconButton(onClick = {
                                                editando = item
                                                formularioAbierto = true
                                            }) { Icon(Icons.Outlined.Edit, contentDescription = "Editar", tint = Colores.azul) }
                                        }
                                        if (eliminar != null) {
                                            IconButton(onClick = { aEliminar = item }) {
                                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Eliminar", tint = Colores.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (textoNuevo != null && items != null) {
            ExtendedFloatingActionButton(
                onClick = {
                    editando = null
                    formularioAbierto = true
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(textoNuevo) },
                containerColor = Colores.azul,
                contentColor = Colores.sobreAzul,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            )
        }
    }

    if (formularioAbierto) {
        formulario(editando) { mensaje ->
            formularioAbierto = false
            editando = null
            if (mensaje != null) {
                avisar(mensaje)
                recarga++
            }
        }
    }

    aEliminar?.let { item ->
        DialogoConfirmar(
            titulo = "¿Eliminar registro?",
            mensaje = "Se eliminará \"${nombre(item)}\". Esta acción no se puede deshacer.",
            textoConfirmar = "Eliminar",
            procesando = eliminando,
            alConfirmar = {
                alcance.launch {
                    eliminando = true
                    try {
                        eliminar!!(item)
                        avisar("\"${nombre(item)}\" se eliminó")
                        recarga++
                    } catch (e: Exception) {
                        avisar(e.mensaje())
                    }
                    eliminando = false
                    aEliminar = null
                }
            },
            alCancelar = { aEliminar = null },
        )
    }
}

/** Minúsculas y sin tildes, para que "quincalleria" encuentre "Quincallería". */
fun normalizar(texto: String): String =
    Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

/** Estado del formulario: guarda en un hilo de corrutina y expone si está guardando y el error. */
class EstadoGuardado {
    var guardando by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
}

@Composable
fun rememberEstadoGuardado() = remember { EstadoGuardado() }

/** Ejecuta [accion] mostrando el progreso; si falla, deja el mensaje en [EstadoGuardado.error]. */
fun EstadoGuardado.ejecutar(alcance: CoroutineScope, accion: suspend () -> Unit, alExito: () -> Unit) {
    if (guardando) return
    alcance.launch {
        guardando = true
        error = null
        try {
            accion()
            alExito()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.mensaje()
        } finally {
            guardando = false
        }
    }
}
