package cl.ventanago.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
                        OutlinedButton(
                            onClick = { ejecutar { IngresoGoogle.ingresar(context)?.let { entrar(it) } } },
                            enabled = !cargando,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                        ) {
                            Text("G", fontWeight = FontWeight.Bold, color = Color(0xFF4285F4), style = MaterialTheme.typography.titleLarge)
                            Text("   Continuar con Google")
                        }
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
                            OutlinedTextField(
                                value = codigo,
                                onValueChange = { codigo = it.filter(Char::isDigit).take(6) },
                                label = { Text("Código de 6 dígitos") },
                                leadingIcon = { Icon(Icons.Outlined.Pin, contentDescription = null) },
                                singleLine = true,
                                enabled = !cargando,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { verificar() }),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Boton(if (codigoEnviado) "Ingresar" else "Recibir código por correo", cargando) {
                            if (codigoEnviado) verificar() else enviarCodigo()
                        }
                        if (codigoEnviado) {
                            TextButton(onClick = { enviarCodigo() }, enabled = !cargando, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                Text("Reenviar código")
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
