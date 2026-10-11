package cl.ventanago.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.ventanago.app.R
import cl.ventanago.app.datos.Autenticacion
import cl.ventanago.app.datos.CarritoLocal
import cl.ventanago.app.datos.ClienteApi
import cl.ventanago.app.datos.IngresoGoogle
import cl.ventanago.app.datos.Recursos
import cl.ventanago.app.datos.Rol
import cl.ventanago.app.datos.Sesion
import cl.ventanago.app.datos.emailValido
import cl.ventanago.app.datos.mensaje
import cl.ventanago.app.ui.comun.MensajeError
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Ingreso: los clientes con Google o con un código que llega a su correo (sin contraseña), y el administrador
 * y los proveedores con contraseña, igual que en la web.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaLogin(autenticacion: Autenticacion, motivo: String? = null, alVolver: (() -> Unit)? = null) {
    val alcance = rememberCoroutineScope()
    val context = LocalContext.current
    val foco = LocalFocusManager.current
    var esCliente by rememberSaveable { mutableStateOf(true) }
    // Si ya escribió su correo en el carrito (sin cuenta), se usa ese.
    var email by rememberSaveable { mutableStateOf(CarritoLocal.borrador?.contacto?.email.orEmpty()) }
    var password by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var codigoEnviado by rememberSaveable { mutableStateOf(false) }
    var verPassword by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }
    var lento by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(autenticacion.aviso) }
    var info by remember { mutableStateOf<String?>(null) }
    // Segundos que faltan para poder pedir otro código (evita pedir varios seguidos).
    var esperaReenvio by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(esperaReenvio) {
        if (esperaReenvio > 0) {
            delay(1000)
            esperaReenvio--
        }
    }

    // Si el servidor de Render estaba dormido, se avisa que la primera respuesta tarda.
    LaunchedEffect(cargando) {
        lento = false
        if (cargando) {
            delay(6000)
            lento = true
        }
    }

    fun entrar(sesion: Sesion) {
        // Solo los roles que la app conoce (administrador, cliente y empresa).
        if (Rol.delBackend(sesion.cuenta.rol) != null) autenticacion.iniciar(sesion)
        else error = "Este tipo de cuenta todavía no está en la app. Por ahora úsala desde la versión web."
    }

    fun ejecutar(accion: suspend () -> Unit) {
        foco.clearFocus()
        if (cargando) return
        alcance.launch {
            cargando = true
            error = null
            try {
                accion()
            } catch (e: Exception) {
                error = e.mensaje()
            }
            cargando = false
        }
    }

    fun enviarCodigo() {
        if (!emailValido(email)) {
            error = "Ingresa un correo válido."
            return
        }
        ejecutar {
            ClienteApi.solicitarCodigo(email.trim())
            codigoEnviado = true
            codigo = ""
            esperaReenvio = 60
            info = "Te enviamos un código de 6 dígitos a ${email.trim()}. Revisa también la carpeta de spam."
        }
    }

    fun verificar() {
        if (codigo.length != 6) {
            error = "El código tiene 6 dígitos."
            return
        }
        ejecutar { entrar(ClienteApi.verificarCodigo(email.trim(), codigo)) }
    }

    fun ingresarAdmin() {
        error = when {
            !emailValido(email) -> "Ingresa un correo válido."
            password.isEmpty() -> "Ingresa tu contraseña."
            else -> null
        }
        if (error == null) ejecutar { entrar(Recursos.login(email.trim(), password)) }
    }

    alVolver?.let { BackHandler(enabled = !cargando, onBack = it) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        alVolver?.let {
            IconButton(onClick = it, enabled = !cargando, modifier = Modifier.align(Alignment.TopStart).padding(4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Colores.texto)
            }
        }
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(84.dp)
                    .background(Colores.marca, RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(84.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("VentanaGo", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Colores.textoFuerte)
            Text("Diseña, mira en tu pared y cotiza tus ventanas", color = Colores.gris, textAlign = TextAlign.Center)
            motivo?.let {
                Spacer(Modifier.height(16.dp))
                Surface(color = Colores.azulClaro, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(it, color = Colores.textoFuerte, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                }
            }
            Spacer(Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Colores.superficie),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf(true to "Soy cliente", false to "Empresa o admin").forEachIndexed { i, (cliente, texto) ->
                            SegmentedButton(
                                selected = esCliente == cliente,
                                onClick = {
                                    esCliente = cliente
                                    error = null
                                    info = null
                                },
                                shape = SegmentedButtonDefaults.itemShape(i, 2),
                                enabled = !cargando,
                            ) { Text(texto) }
                        }
                    }
                    error?.let { MensajeError(it) }
                    info?.takeIf { esCliente && codigoEnviado }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = Colores.textoSecundario)
                    }

                    if (esCliente && IngresoGoogle.disponible) {
                        BotonGoogle(enabled = !cargando) { ejecutar { IngresoGoogle.ingresar(context)?.let { entrar(it) } } }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HorizontalDivider(Modifier.weight(1f))
                            Text("  o con tu correo  ", style = MaterialTheme.typography.bodySmall, color = Colores.gris)
                            HorizontalDivider(Modifier.weight(1f))
                        }
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it.trim()
                            if (codigoEnviado) codigoEnviado = false
                        },
                        label = { Text("Correo") },
                        leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                        singleLine = true,
                        enabled = !cargando,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = if (esCliente) ImeAction.Send else ImeAction.Next),
                        keyboardActions = KeyboardActions(onSend = { enviarCodigo() }),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (esCliente) {
                        if (codigoEnviado) {
                            CampoCodigo(codigo, enabled = !cargando) {
                                codigo = it
                                // Con los 6 dígitos se entra solo, sin tener que tocar el botón.
                                if (it.length == 6) verificar()
                            }
                        }
                        Boton(if (codigoEnviado) "Ingresar" else "Recibir código por correo", cargando) {
                            if (codigoEnviado) verificar() else enviarCodigo()
                        }
                        if (codigoEnviado) {
                            TextButton(onClick = { enviarCodigo() }, enabled = !cargando && esperaReenvio == 0, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                Text(if (esperaReenvio > 0) "Reenviar código en ${esperaReenvio / 60}:${"%02d".format(esperaReenvio % 60)}" else "Reenviar código")
                            }
                        } else {
                            Text(
                                "Sin contraseña: te enviamos un código cada vez que entras. Si es tu primera vez, se crea tu cuenta.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Colores.gris,
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Contraseña") },
                            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { verPassword = !verPassword }) {
                                    Icon(
                                        if (verPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (verPassword) "Ocultar contraseña" else "Mostrar contraseña",
                                    )
                                }
                            },
                            visualTransformation = if (verPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            enabled = !cargando,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { ingresarAdmin() }),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Boton("Ingresar", cargando) { ingresarAdmin() }
                    }

                    if (lento) {
                        Text(
                            "Despertando el servidor… la primera conexión del día puede tardar hasta un minuto.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Colores.gris,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Boton(texto: String, cargando: Boolean, alPulsar: () -> Unit) {
    Button(
        onClick = alPulsar,
        enabled = !cargando,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
    ) {
        if (cargando) CircularProgressIndicator(Modifier.size(22.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
        else Text(texto)
    }
}

/** "Continuar con Google" con el logo y los colores de la guía de marca de Google (versión clara u oscura). */
@Composable
private fun BotonGoogle(enabled: Boolean, alPulsar: () -> Unit) {
    val oscuro = enModoOscuro()
    OutlinedButton(
        onClick = alPulsar,
        enabled = enabled,
        border = BorderStroke(1.dp, if (oscuro) Color(0xFF8E918F) else Color(0xFF747775)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (oscuro) Color(0xFF131314) else Color.White,
            contentColor = if (oscuro) Color(0xFFE3E3E3) else Color(0xFF1F1F1F),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
    ) {
        Image(painterResource(R.drawable.ic_google), contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        // La guía pide Roboto Medium: la fuente del sistema en Android.
        Text("Continuar con Google", fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

/** El código de 6 dígitos en casillas, una por dígito; el teclado numérico se abre solo. */
@Composable
private fun CampoCodigo(codigo: String, enabled: Boolean, alCambiar: (String) -> Unit) {
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { foco.requestFocus() }
    BasicTextField(
        value = codigo,
        onValueChange = { nuevo -> alCambiar(nuevo.filter(Char::isDigit).take(6)) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(foco)
            .semantics { contentDescription = "Código de 6 dígitos" },
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(6) { i ->
                    val digito = codigo.getOrNull(i)
                    val actual = i == codigo.length && enabled
                    Box(
                        Modifier
                            .weight(1f)
                            .height(56.dp)
                            .border(
                                if (actual) 2.dp else 1.dp,
                                if (actual) Colores.azul else Colores.borde,
                                MaterialTheme.shapes.medium,
                            )
                            .background(Colores.superficie, MaterialTheme.shapes.medium),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(digito?.toString().orEmpty(), style = MaterialTheme.typography.headlineSmall, color = Colores.textoFuerte)
                    }
                }
            }
        },
    )
}
