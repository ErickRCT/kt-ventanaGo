package cl.ventanago.app.ui.modulos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import cl.ventanago.app.datos.ColorVentana
import cl.ventanago.app.datos.Recursos
import cl.ventanago.app.datos.Serie
import cl.ventanago.app.datos.TipoPerfil
import cl.ventanago.app.datos.Vidrio
import cl.ventanago.app.datos.pesos
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.comun.CampoTexto
import cl.ventanago.app.ui.comun.FormularioPantalla
import cl.ventanago.app.ui.comun.Mantenedor
import cl.ventanago.app.ui.comun.ejecutar
import cl.ventanago.app.ui.comun.rememberEstadoGuardado

const val REQUERIDO = "Este campo es requerido"

/** Textos de una tarjeta del mantenedor: título, detalle y un dato destacado. */
@Composable
fun RowScope.TextosFila(titulo: String, detalle: String? = null, destacado: String? = null) {
    Column(Modifier.weight(1f)) {
        Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        detalle?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = Colores.gris, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        destacado?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Colores.azul, fontWeight = FontWeight.SemiBold) }
    }
}

/** Solo dígitos, para los campos de valor en pesos. */
fun soloDigitos(texto: String) = texto.filter { it.isDigit() }.take(12)

// ---------- Series ----------

@Composable
fun PantallaSeries(soloLectura: Boolean = false) = Mantenedor(
    titulo = "Series",
    descripcion = "Líneas de perfiles de aluminio con las que se fabrican las ventanas.",
    cargar = { Recursos.series.listar().sortedBy { it.nombre.lowercase() } },
    clave = { it.serieId ?: it.nombre },
    textoBusqueda = { "${it.nombre} ${it.descripcion.orEmpty()}" },
    nombre = { it.nombre },
    textoNuevo = "Nueva serie",
    formulario = { item, terminar -> FormularioSerie(item, terminar) },
    eliminar = { Recursos.series.eliminar(it.serieId!!) },
    soloLectura = soloLectura,
) { TextosFila(it.nombre, it.descripcion) }

@Composable
private fun FormularioSerie(inicial: Serie?, terminar: (String?) -> Unit) {
    var nombre by remember { mutableStateOf(inicial?.nombre.orEmpty()) }
    var descripcion by remember { mutableStateOf(inicial?.descripcion.orEmpty()) }
    var validar by remember { mutableStateOf(false) }
    val estado = rememberEstadoGuardado()
    val alcance = rememberCoroutineScope()

    FormularioPantalla(
        titulo = if (inicial == null) "Nueva serie" else "Editar serie",
        guardando = estado.guardando,
        error = estado.error,
        alCerrar = { terminar(null) },
        alGuardar = {
            validar = true
            if (nombre.isBlank()) return@FormularioPantalla
            val serie = Serie(inicial?.serieId, nombre.trim(), descripcion.trim())
            estado.ejecutar(alcance, {
                if (inicial == null) Recursos.series.agregar(serie) else Recursos.series.modificar(serie)
            }) { terminar(if (inicial == null) "Serie creada" else "Serie actualizada") }
        },
    ) {
        CampoTexto("Nombre", nombre, { nombre = it }, error = REQUERIDO.takeIf { validar && nombre.isBlank() })
        CampoTexto("Descripción", descripcion, { descripcion = it }, lineas = 3)
    }
}

// ---------- Tipos de perfil ----------

@Composable
fun PantallaTiposPerfil(soloLectura: Boolean = false) = Mantenedor(
    titulo = "Tipos de perfil",
    descripcion = "Clasificación de los perfiles (marco, hoja, pilastra, etc.).",
    cargar = { Recursos.tiposPerfil.listar().sortedBy { it.nombre.lowercase() } },
    clave = { it.tipoPerfilId ?: it.nombre },
    textoBusqueda = { it.nombre },
    nombre = { it.nombre },
    textoNuevo = "Nuevo tipo",
    formulario = { item, terminar -> FormularioTipoPerfil(item, terminar) },
    eliminar = { Recursos.tiposPerfil.eliminar(it.tipoPerfilId!!) },
    soloLectura = soloLectura,
) { TextosFila(it.nombre) }

@Composable
private fun FormularioTipoPerfil(inicial: TipoPerfil?, terminar: (String?) -> Unit) {
    var nombre by remember { mutableStateOf(inicial?.nombre.orEmpty()) }
    var validar by remember { mutableStateOf(false) }
    val estado = rememberEstadoGuardado()
    val alcance = rememberCoroutineScope()

    FormularioPantalla(
        titulo = if (inicial == null) "Nuevo tipo de perfil" else "Editar tipo de perfil",
        guardando = estado.guardando,
        error = estado.error,
        alCerrar = { terminar(null) },
        alGuardar = {
            validar = true
            if (nombre.isBlank()) return@FormularioPantalla
            val tipo = TipoPerfil(inicial?.tipoPerfilId, nombre.trim())
            estado.ejecutar(alcance, {
                if (inicial == null) Recursos.tiposPerfil.agregar(tipo) else Recursos.tiposPerfil.modificar(tipo)
            }) { terminar(if (inicial == null) "Tipo de perfil creado" else "Tipo de perfil actualizado") }
        },
    ) {
        CampoTexto("Nombre", nombre, { nombre = it }, error = REQUERIDO.takeIf { validar && nombre.isBlank() })
    }
}

// ---------- Colores y vidrios (nombre + valor) ----------

@Composable
fun PantallaColores(soloLectura: Boolean = false) = Mantenedor(
    titulo = "Colores",
    descripcion = "Colores del aluminio y su valor para la cotización.",
    cargar = { Recursos.colores.listar().sortedBy { it.nombre.lowercase() } },
    clave = { it.colorId ?: it.nombre },
    textoBusqueda = { it.nombre },
    nombre = { it.nombre },
    textoNuevo = "Nuevo color",
    formulario = { item, terminar ->
        FormularioNombreValor(
            titulo = if (item == null) "Nuevo color" else "Editar color",
            nombreInicial = item?.nombre,
            valorInicial = item?.valor,
            terminar = terminar,
            mensajeExito = if (item == null) "Color creado" else "Color actualizado",
        ) { nombre, valor ->
            val color = ColorVentana(item?.colorId, nombre, valor)
            if (item == null) Recursos.colores.agregar(color) else Recursos.colores.modificar(color)
        }
    },
    eliminar = { Recursos.colores.eliminar(it.colorId!!) },
    soloLectura = soloLectura,
) { TextosFila(it.nombre, destacado = pesos(it.valor)) }

@Composable
fun PantallaVidrios(soloLectura: Boolean = false) = Mantenedor(
    titulo = "Vidrios",
    descripcion = "Tipos de vidrio disponibles y su valor.",
    cargar = { Recursos.vidrios.listar().sortedBy { it.nombre.lowercase() } },
    clave = { it.vidrioId ?: it.nombre },
    textoBusqueda = { it.nombre },
    nombre = { it.nombre },
    textoNuevo = "Nuevo vidrio",
    formulario = { item, terminar ->
        FormularioNombreValor(
            titulo = if (item == null) "Nuevo vidrio" else "Editar vidrio",
            nombreInicial = item?.nombre,
            valorInicial = item?.valor,
            terminar = terminar,
            mensajeExito = if (item == null) "Vidrio creado" else "Vidrio actualizado",
        ) { nombre, valor ->
            val vidrio = Vidrio(item?.vidrioId, nombre, valor)
            if (item == null) Recursos.vidrios.agregar(vidrio) else Recursos.vidrios.modificar(vidrio)
        }
    },
    eliminar = { Recursos.vidrios.eliminar(it.vidrioId!!) },
    soloLectura = soloLectura,
) { TextosFila(it.nombre, destacado = pesos(it.valor)) }

@Composable
private fun FormularioNombreValor(
    titulo: String,
    nombreInicial: String?,
    valorInicial: Long?,
    terminar: (String?) -> Unit,
    mensajeExito: String,
    guardar: suspend (nombre: String, valor: Long) -> Unit,
) {
    var nombre by remember { mutableStateOf(nombreInicial.orEmpty()) }
    var valor by remember { mutableStateOf(valorInicial?.toString().orEmpty()) }
    var validar by remember { mutableStateOf(false) }
    val estado = rememberEstadoGuardado()
    val alcance = rememberCoroutineScope()

    FormularioPantalla(
        titulo = titulo,
        guardando = estado.guardando,
        error = estado.error,
        alCerrar = { terminar(null) },
        alGuardar = {
            validar = true
            val numero = valor.toLongOrNull()
            if (nombre.isBlank() || numero == null) return@FormularioPantalla
            estado.ejecutar(alcance, { guardar(nombre.trim(), numero) }) { terminar(mensajeExito) }
        },
    ) {
        CampoTexto("Nombre", nombre, { nombre = it }, error = REQUERIDO.takeIf { validar && nombre.isBlank() })
        CampoTexto(
            "Valor", valor, { valor = soloDigitos(it) },
            teclado = KeyboardType.Number,
            prefijo = "$ ",
            error = REQUERIDO.takeIf { validar && valor.toLongOrNull() == null },
            ayuda = valor.toLongOrNull()?.let { pesos(it) },
        )
    }
}
