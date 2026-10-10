package cl.ventanago.app.ui.modulos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import cl.ventanago.app.datos.Cliente
import cl.ventanago.app.datos.Comuna
import cl.ventanago.app.datos.Recursos
import cl.ventanago.app.datos.Region
import cl.ventanago.app.datos.emailValido
import cl.ventanago.app.datos.formatearRut
import cl.ventanago.app.datos.mensaje
import cl.ventanago.app.datos.rutValido
import cl.ventanago.app.datos.telefonoValido
import cl.ventanago.app.ui.comun.CampoTexto
import cl.ventanago.app.ui.comun.FormularioPantalla
import cl.ventanago.app.ui.comun.Mantenedor
import cl.ventanago.app.ui.comun.Selector
import cl.ventanago.app.ui.comun.ejecutar
import cl.ventanago.app.ui.comun.rememberEstadoGuardado

@Composable
fun PantallaClientes() = Mantenedor(
    titulo = "Clientes",
    descripcion = "Clientes a los que se les emiten cotizaciones.",
    cargar = { Recursos.clientes.listar().sortedBy { it.nombre.lowercase() } },
    clave = { it.clienteId ?: it.rut },
    textoBusqueda = { "${it.nombre} ${it.rut} ${it.email.orEmpty()} ${it.telefono.orEmpty()} ${it.comuna?.nombre.orEmpty()}" },
    nombre = { it.nombre },
    textoNuevo = "Nuevo cliente",
    formulario = { item, terminar -> FormularioCliente(item, terminar) },
    eliminar = { Recursos.clientes.eliminar(it.clienteId!!) },
) { c ->
    TextosFila(
        c.nombre,
        listOfNotNull(c.rut.takeIf { it.isNotBlank() }, c.telefono, c.email).joinToString(" · "),
        listOfNotNull(c.direccion?.takeIf { it.isNotBlank() }, c.comuna?.nombre).joinToString(", ").takeIf { it.isNotBlank() },
    )
}

@Composable
private fun FormularioCliente(inicial: Cliente?, terminar: (String?) -> Unit) {
    val estado = rememberEstadoGuardado()
    val alcance = rememberCoroutineScope()

    var rut by remember { mutableStateOf(inicial?.rut.orEmpty()) }
    var nombre by remember { mutableStateOf(inicial?.nombre.orEmpty()) }
    var telefono by remember { mutableStateOf(inicial?.telefono.orEmpty()) }
    var email by remember { mutableStateOf(inicial?.email.orEmpty()) }
    var direccion by remember { mutableStateOf(inicial?.direccion.orEmpty()) }
    var region by remember { mutableStateOf<Region?>(inicial?.comuna?.region) }
    var comuna by remember { mutableStateOf<Comuna?>(inicial?.comuna) }
    var validar by remember { mutableStateOf(false) }

    var regiones by remember { mutableStateOf<List<Region>?>(null) }
    var comunas by remember { mutableStateOf<List<Comuna>?>(null) }
    LaunchedEffect(Unit) {
        regiones = try {
            Recursos.regiones()
        } catch (e: Exception) {
            estado.error = e.mensaje()
            emptyList()
        }
    }
    LaunchedEffect(region?.regionId) {
        val id = region?.regionId ?: return@LaunchedEffect
        comunas = null
        comunas = try {
            Recursos.comunas(id).sortedBy { it.nombre }
        } catch (e: Exception) {
            estado.error = e.mensaje()
            emptyList()
        }
    }

    val errorRut = when {
        rut.isBlank() -> REQUERIDO
        !rutValido(rut) -> "RUT inválido"
        else -> null
    }
    val errorTelefono = when {
        telefono.isBlank() -> REQUERIDO
        !telefonoValido(telefono) -> "Teléfono inválido (ej: +56 9 1234 5678)"
        else -> null
    }
    val errorEmail = when {
        email.isBlank() -> REQUERIDO
        !emailValido(email) -> "Email inválido"
        else -> null
    }

    FormularioPantalla(
        titulo = if (inicial == null) "Nuevo cliente" else "Editar cliente",
        guardando = estado.guardando,
        error = estado.error,
        alCerrar = { terminar(null) },
        alGuardar = {
            validar = true
            val incompleto = listOf(errorRut, errorTelefono, errorEmail).any { it != null } ||
                nombre.isBlank() || direccion.isBlank() || comuna == null
            if (incompleto) return@FormularioPantalla
            val cliente = Cliente(
                clienteId = inicial?.clienteId,
                rut = formatearRut(rut),
                nombre = nombre.trim(),
                telefono = telefono.trim(),
                email = email.trim(),
                direccion = direccion.trim(),
                comuna = comuna,
            )
            estado.ejecutar(alcance, {
                if (inicial == null) Recursos.clientes.agregar(cliente) else Recursos.clientes.modificar(cliente)
            }) { terminar(if (inicial == null) "Cliente creado" else "Cliente actualizado") }
        },
    ) {
        CampoTexto(
            "RUT", rut, { rut = it.filter { c -> c.isDigit() || c in ".-kK" }.take(12) },
            mayusculas = KeyboardCapitalization.Characters,
            teclado = KeyboardType.Ascii,
            error = errorRut.takeIf { validar },
            ayuda = if (rutValido(rut)) formatearRut(rut) else "Ej: 12.345.678-9",
        )
        CampoTexto("Nombre", nombre, { nombre = it }, mayusculas = KeyboardCapitalization.Words, error = REQUERIDO.takeIf { validar && nombre.isBlank() })
        CampoTexto("Teléfono", telefono, { telefono = it }, teclado = KeyboardType.Phone, error = errorTelefono.takeIf { validar })
        CampoTexto("Email", email, { email = it.trim() }, teclado = KeyboardType.Email, mayusculas = KeyboardCapitalization.None, error = errorEmail.takeIf { validar })
        CampoTexto("Dirección", direccion, { direccion = it }, error = REQUERIDO.takeIf { validar && direccion.isBlank() })
        Selector(
            "Región", regiones.orEmpty(), regiones?.find { it.regionId == region?.regionId } ?: region,
            texto = { it.nombre },
            alElegir = {
                if (it.regionId != region?.regionId) comuna = null
                region = it
            },
            cargando = regiones == null,
            error = REQUERIDO.takeIf { validar && region == null },
        )
        Selector(
            "Comuna", comunas.orEmpty(), comunas?.find { it.comunaId == comuna?.comunaId } ?: comuna,
            texto = { it.nombre },
            alElegir = { comuna = it },
            habilitado = region != null,
            cargando = region != null && comunas == null,
            error = REQUERIDO.takeIf { validar && comuna == null },
        )
    }
}
