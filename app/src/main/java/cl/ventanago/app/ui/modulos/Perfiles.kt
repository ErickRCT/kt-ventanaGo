package cl.ventanago.app.ui.modulos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cl.ventanago.app.datos.Perfil
import cl.ventanago.app.datos.Quincalleria
import cl.ventanago.app.datos.Recursos
import cl.ventanago.app.datos.Serie
import cl.ventanago.app.datos.TipoPauta
import cl.ventanago.app.datos.TipoPerfil
import cl.ventanago.app.datos.mensaje
import cl.ventanago.app.datos.pesos
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.comun.CampoTexto
import cl.ventanago.app.ui.comun.FormularioPantalla
import cl.ventanago.app.ui.comun.ImagenCatalogo
import cl.ventanago.app.ui.comun.Interruptor
import cl.ventanago.app.ui.comun.Mantenedor
import cl.ventanago.app.ui.comun.Opciones
import cl.ventanago.app.ui.comun.Selector
import cl.ventanago.app.ui.comun.SelectorImagen
import cl.ventanago.app.ui.comun.ejecutar
import cl.ventanago.app.ui.comun.rememberEstadoGuardado

@Composable
internal fun Etiqueta(texto: String, fondo: Color = Colores.azulClaro, color: Color = Colores.azul) {
    Surface(color = fondo, shape = RoundedCornerShape(6.dp)) {
        Text(
            texto,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

/** Opciones de un selector cargadas desde el backend al abrir el formulario. */
@Composable
internal fun <T> rememberOpciones(cargar: suspend () -> List<T>, alFallar: (String) -> Unit): List<T>? {
    var opciones by remember { mutableStateOf<List<T>?>(null) }
    LaunchedEffect(Unit) {
        opciones = try {
            cargar()
        } catch (e: Exception) {
            alFallar(e.mensaje())
            emptyList()
        }
    }
    return opciones
}

// ---------- Perfiles ----------

@Composable
fun PantallaPerfiles() = Mantenedor(
    titulo = "Perfiles",
    descripcion = "Perfiles de aluminio por serie, con su peso por metro.",
    cargar = { Recursos.perfiles.listar().sortedWith(compareBy({ it.serie?.nombre?.lowercase() }, { it.codigo.lowercase() })) },
    clave = { it.perfilId ?: it.codigo },
    textoBusqueda = { "${it.codigo} ${it.descripcion.orEmpty()} ${it.serie?.nombre.orEmpty()} ${it.tipoPerfil?.nombre.orEmpty()}" },
    nombre = { it.codigo },
    textoNuevo = "Nuevo perfil",
    formulario = { item, terminar -> FormularioPerfil(item, terminar) },
    eliminar = { Recursos.perfiles.eliminar(it.perfilId!!) },
) { perfil ->
    Column(Modifier.weight(1f)) {
        Text(
            listOfNotNull(perfil.codigo, perfil.descripcion?.takeIf { it.isNotBlank() }).joinToString(" · "),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            listOfNotNull(perfil.serie?.nombre, perfil.tipoPerfil?.nombre, perfil.peso?.let { "$it kg/m" }).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = Colores.gris,
        )
        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Etiqueta(if (perfil.orientacion == "V") "Vertical" else "Horizontal", Colores.fondo, Colores.textoSecundario)
            if (perfil.isBastidor == true) Etiqueta("Bastidor")
            if (perfil.reforzado) Etiqueta("Reforzado", Color(0xFFFFF4E5), Color(0xFF8A5300))
        }
    }
}

@Composable
private fun FormularioPerfil(inicial: Perfil?, terminar: (String?) -> Unit) {
    val estado = rememberEstadoGuardado()
    val alcance = rememberCoroutineScope()
    val series = rememberOpciones({ Recursos.series.listar().sortedBy { it.nombre.lowercase() } }) { estado.error = it }
    val tipos = rememberOpciones({ Recursos.tiposPerfil.listar().sortedBy { it.nombre.lowercase() } }) { estado.error = it }

    var codigo by remember { mutableStateOf(inicial?.codigo.orEmpty()) }
    var peso by remember { mutableStateOf(inicial?.peso.orEmpty()) }
    var descripcion by remember { mutableStateOf(inicial?.descripcion.orEmpty()) }
    var tipo by remember { mutableStateOf<TipoPerfil?>(inicial?.tipoPerfil) }
    var serie by remember { mutableStateOf<Serie?>(inicial?.serie) }
    var bastidor by remember { mutableStateOf(inicial?.isBastidor ?: false) }
    var reforzado by remember { mutableStateOf(inicial?.reforzado ?: false) }
    var orientacion by remember { mutableStateOf(inicial?.orientacion ?: "H") }
    var validar by remember { mutableStateOf(false) }
    val pesoValido = peso.replace(',', '.').toDoubleOrNull()?.let { it >= 0 } == true

    FormularioPantalla(
        titulo = if (inicial == null) "Nuevo perfil" else "Editar perfil",
        guardando = estado.guardando,
        error = estado.error,
        alCerrar = { terminar(null) },
        alGuardar = {
            validar = true
            if (codigo.isBlank() || !pesoValido || tipo == null || serie == null) return@FormularioPantalla
            val perfil = (inicial ?: Perfil()).copy(
                codigo = codigo.trim(),
                peso = peso.replace(',', '.').trim(),
                descripcion = descripcion.trim(),
                tipoPerfil = tipo,
                serie = serie,
                isBastidor = bastidor,
                reforzado = reforzado,
                orientacion = orientacion,
            )
            estado.ejecutar(alcance, {
                if (inicial == null) Recursos.perfiles.agregar(perfil) else Recursos.perfiles.modificar(perfil)
            }) { terminar(if (inicial == null) "Perfil creado" else "Perfil actualizado") }
        },
    ) {
        CampoTexto(
            "Código", codigo, { codigo = it },
            mayusculas = KeyboardCapitalization.Characters,
            error = REQUERIDO.takeIf { validar && codigo.isBlank() },
        )
        CampoTexto(
            "Peso (kg por metro)", peso, { peso = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
            teclado = KeyboardType.Decimal,
            error = (if (peso.isBlank()) REQUERIDO else "Ingresa un número válido").takeIf { validar && !pesoValido },
        )
        CampoTexto("Descripción", descripcion, { descripcion = it }, lineas = 2)
        Selector(
            "Tipo de perfil", tipos.orEmpty(), tipos?.find { it.tipoPerfilId == tipo?.tipoPerfilId } ?: tipo,
            texto = { it.nombre }, alElegir = { tipo = it },
            cargando = tipos == null,
            error = REQUERIDO.takeIf { validar && tipo == null },
        )
        Selector(
            "Serie", series.orEmpty(), series?.find { it.serieId == serie?.serieId } ?: serie,
            texto = { it.nombre }, alElegir = { serie = it },
            cargando = series == null,
            error = REQUERIDO.takeIf { validar && serie == null },
        )
        Opciones("Orientación", listOf("H" to "Horizontal", "V" to "Vertical"), orientacion) { orientacion = it }
        Interruptor("¿Es bastidor?", bastidor, { bastidor = it })
        Interruptor("¿Reforzado?", reforzado, { reforzado = it })
    }
}

// ---------- Quincallería ----------

@Composable
fun PantallaQuincalleria() = Mantenedor(
    titulo = "Quincallería",
    descripcion = "Accesorios y herrajes por serie (manillas, rodamientos, felpas…).",
    cargar = { Recursos.quincallerias.listar().sortedWith(compareBy({ it.serie?.nombre?.lowercase() }, { it.nombre.lowercase() })) },
    clave = { it.quincalleriaId ?: it.nombre },
    textoBusqueda = { "${it.nombre} ${it.serie?.nombre.orEmpty()}" },
    nombre = { it.nombre },
    textoNuevo = "Nueva",
    formulario = { item, terminar -> FormularioQuincalleria(item, terminar) },
    eliminar = { Recursos.quincallerias.eliminar(it.quincalleriaId!!) },
) { q ->
    ImagenCatalogo(q.rutaImagen)
    Spacer(Modifier.width(12.dp))
    TextosFila(
        q.nombre,
        q.serie?.nombre?.let { "Serie $it" },
        "${pesos(q.valor)} / ${if (q.unidad == "Mt") "metro" else "pieza"}",
    )
}

@Composable
private fun FormularioQuincalleria(inicial: Quincalleria?, terminar: (String?) -> Unit) {
    val estado = rememberEstadoGuardado()
    val alcance = rememberCoroutineScope()
    val series = rememberOpciones({ Recursos.series.listar().sortedBy { it.nombre.lowercase() } }) { estado.error = it }

    var nombre by remember { mutableStateOf(inicial?.nombre.orEmpty()) }
    var valor by remember { mutableStateOf(inicial?.valor?.toString().orEmpty()) }
    var unidad by remember { mutableStateOf(inicial?.unidad ?: "Pz") }
    var serie by remember { mutableStateOf<Serie?>(inicial?.serie) }
    var imagen by remember { mutableStateOf(inicial?.rutaImagen?.takeIf { it.isNotBlank() }) }
    var validar by remember { mutableStateOf(false) }

    FormularioPantalla(
        titulo = if (inicial == null) "Nueva quincallería" else "Editar quincallería",
        guardando = estado.guardando,
        error = estado.error,
        alCerrar = { terminar(null) },
        alGuardar = {
            validar = true
            val numero = valor.toLongOrNull()
            if (nombre.isBlank() || numero == null || serie == null) return@FormularioPantalla
            val quincalleria = Quincalleria(inicial?.quincalleriaId, nombre.trim(), unidad, numero, imagen.orEmpty(), serie)
            estado.ejecutar(alcance, {
                if (inicial == null) Recursos.quincallerias.agregar(quincalleria) else Recursos.quincallerias.modificar(quincalleria)
            }) { terminar(if (inicial == null) "Quincallería creada" else "Quincallería actualizada") }
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
        Opciones("Unidad", listOf("Pz" to "Pieza (Pz)", "Mt" to "Metro (Mt)"), unidad) { unidad = it }
        Selector(
            "Serie", series.orEmpty(), series?.find { it.serieId == serie?.serieId } ?: serie,
            texto = { it.nombre }, alElegir = { serie = it },
            cargando = series == null,
            error = REQUERIDO.takeIf { validar && serie == null },
        )
        SelectorImagen(imagen) { imagen = it }
    }
}

// ---------- Tipos de pauta ----------

@Composable
fun PantallaTiposPauta() = Mantenedor(
    titulo = "Tipos de pauta",
    descripcion = "Familias de productos (ventana corredera, abatible, puerta…). El servidor aún no permite editarlos ni eliminarlos.",
    cargar = { Recursos.tiposPauta.listar().sortedBy { it.nombre.lowercase() } },
    clave = { it.tipoPautaId ?: it.nombre },
    textoBusqueda = { it.nombre },
    nombre = { it.nombre },
    textoNuevo = "Nuevo tipo",
    formulario = { _, terminar -> FormularioTipoPauta(terminar) },
    editable = false,
) { tipo ->
    ImagenCatalogo(tipo.rutaImagen)
    Spacer(Modifier.width(12.dp))
    TextosFila(tipo.nombre)
}

@Composable
private fun FormularioTipoPauta(terminar: (String?) -> Unit) {
    val estado = rememberEstadoGuardado()
    val alcance = rememberCoroutineScope()
    var nombre by remember { mutableStateOf("") }
    var imagen by remember { mutableStateOf<String?>(null) }
    var validar by remember { mutableStateOf(false) }

    FormularioPantalla(
        titulo = "Nuevo tipo de pauta",
        guardando = estado.guardando,
        error = estado.error,
        alCerrar = { terminar(null) },
        alGuardar = {
            validar = true
            if (nombre.isBlank()) return@FormularioPantalla
            estado.ejecutar(alcance, { Recursos.tiposPauta.agregar(TipoPauta(nombre = nombre.trim(), rutaImagen = imagen)) }) {
                terminar("Tipo de pauta creado")
            }
        },
    ) {
        CampoTexto("Nombre", nombre, { nombre = it }, error = REQUERIDO.takeIf { validar && nombre.isBlank() })
        SelectorImagen(imagen) { imagen = it }
    }
}
