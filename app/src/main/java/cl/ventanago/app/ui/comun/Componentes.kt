package cl.ventanago.app.ui.comun

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cl.ventanago.app.ui.Colores

/** Muestra un mensaje breve (snackbar) desde cualquier pantalla. */
val LocalAvisos = staticCompositionLocalOf<(String) -> Unit> { {} }

@Composable
fun CampoTexto(
    etiqueta: String,
    valor: String,
    alCambiar: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    teclado: KeyboardType = KeyboardType.Text,
    mayusculas: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    lineas: Int = 1,
    prefijo: String? = null,
    ayuda: String? = null,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        isError = error != null,
        supportingText = (error ?: ayuda)?.let { { Text(it) } },
        singleLine = lineas == 1,
        minLines = lineas,
        prefix = prefijo?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = teclado, capitalization = mayusculas),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Lista desplegable de solo lectura (equivale al Select/Autocomplete del front web). */
@Composable
fun <T> Selector(
    etiqueta: String,
    opciones: List<T>,
    seleccionado: T?,
    texto: (T) -> String,
    alElegir: (T) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    habilitado: Boolean = true,
    cargando: Boolean = false,
) {
    var abierto by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = seleccionado?.let(texto) ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = habilitado,
            label = { Text(etiqueta) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            trailingIcon = {
                if (cargando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        // Capa que recibe el toque: el campo es de solo lectura y no abre el teclado.
        Box(
            Modifier
                .matchParentSize()
                .padding(top = 8.dp, bottom = if (error != null) 24.dp else 0.dp)
                .clickable(enabled = habilitado && !cargando) { abierto = true },
        )
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            if (opciones.isEmpty()) {
                DropdownMenuItem(text = { Text("Sin opciones") }, onClick = { abierto = false }, enabled = false)
            }
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = {
                        Text(
                            texto(opcion),
                            fontWeight = if (opcion == seleccionado) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        alElegir(opcion)
                        abierto = false
                    },
                )
            }
        }
    }
}

@Composable
fun Interruptor(etiqueta: String, valor: Boolean, alCambiar: (Boolean) -> Unit, descripcion: String? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { alCambiar(!valor) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(etiqueta, style = MaterialTheme.typography.bodyLarge)
            descripcion?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Colores.gris) }
        }
        Switch(checked = valor, onCheckedChange = alCambiar)
    }
}

/** Opción única entre pocas alternativas (equivale a los RadioGroup del front web). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Opciones(etiqueta: String, opciones: List<Pair<T, String>>, valor: T, alCambiar: (T) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(etiqueta, style = MaterialTheme.typography.labelLarge, color = Colores.textoSecundario)
        Spacer(Modifier.height(6.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            opciones.forEachIndexed { i, (opcion, texto) ->
                SegmentedButton(
                    selected = opcion == valor,
                    onClick = { alCambiar(opcion) },
                    shape = SegmentedButtonDefaults.itemShape(i, opciones.size),
                ) { Text(texto) }
            }
        }
    }
}

/** Formulario a pantalla completa con barra superior (cerrar, título y Guardar). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioPantalla(
    titulo: String,
    guardando: Boolean,
    error: String?,
    alGuardar: () -> Unit,
    alCerrar: () -> Unit,
    textoGuardar: String = "Guardar",
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = { if (!guardando) alCerrar() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(titulo, maxLines = 1) },
                    navigationIcon = {
                        IconButton(onClick = alCerrar, enabled = !guardando) {
                            Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                        }
                    },
                    actions = {
                        if (guardando) {
                            CircularProgressIndicator(
                                Modifier
                                    .padding(end = 16.dp)
                                    .size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            TextButton(onClick = alGuardar) {
                                Text(textoGuardar.uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Colores.azul,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White,
                    ),
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding()
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                error?.let { MensajeError(it) }
                contenido()
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = alGuardar,
                    enabled = !guardando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                ) { Text(textoGuardar) }
            }
        }
    }
}

@Composable
fun MensajeError(texto: String) {
    Surface(color = Color(0xFFFDECEA), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = Colores.error)
            Spacer(Modifier.width(10.dp))
            Text(texto, color = Color(0xFF5F2120), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun DialogoConfirmar(
    titulo: String,
    mensaje: String,
    textoConfirmar: String,
    procesando: Boolean,
    alConfirmar: () -> Unit,
    alCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!procesando) alCancelar() },
        title = { Text(titulo) },
        text = { Text(mensaje) },
        confirmButton = {
            Button(
                onClick = alConfirmar,
                enabled = !procesando,
                colors = ButtonDefaults.buttonColors(containerColor = Colores.error),
            ) {
                if (procesando) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                else Text(textoConfirmar)
            }
        },
        dismissButton = { TextButton(onClick = alCancelar, enabled = !procesando) { Text("Cancelar") } },
    )
}

@Composable
fun EstadoCargando(mensaje: String = "Cargando…") {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(mensaje, color = Colores.gris, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            "Si el servidor estaba inactivo, la primera carga puede tardar hasta un minuto.",
            style = MaterialTheme.typography.bodySmall,
            color = Colores.gris,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun EstadoMensaje(
    icono: ImageVector,
    titulo: String,
    detalle: String? = null,
    accion: String? = null,
    alAccionar: () -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(72.dp)
                .background(Colores.azulClaro, RoundedCornerShape(36.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icono, contentDescription = null, tint = Colores.azul, modifier = Modifier.size(36.dp)) }
        Spacer(Modifier.height(16.dp))
        Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        detalle?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = Colores.gris, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
        }
        accion?.let {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = alAccionar) { Text(it) }
        }
    }
}

@Composable
fun EstadoError(mensaje: String, alReintentar: () -> Unit) =
    EstadoMensaje(Icons.Outlined.CloudOff, "No se pudieron cargar los datos", mensaje, "Reintentar", alReintentar)

@Composable
fun EstadoVacio(titulo: String, detalle: String? = null) = EstadoMensaje(Icons.Outlined.Inbox, titulo, detalle)
