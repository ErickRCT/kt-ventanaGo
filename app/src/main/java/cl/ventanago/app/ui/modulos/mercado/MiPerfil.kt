package cl.ventanago.app.ui.modulos.mercado

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.ventanago.app.datos.Comuna
import cl.ventanago.app.datos.ComunaNombre
import cl.ventanago.app.datos.PerfilProveedor
import cl.ventanago.app.datos.ProveedoresApi
import cl.ventanago.app.datos.Recursos
import cl.ventanago.app.datos.SERVICIOS
import cl.ventanago.app.datos.mensaje
import cl.ventanago.app.datos.telefonoValido
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.comun.CampoTexto
import cl.ventanago.app.ui.comun.EstadoCargando
import cl.ventanago.app.ui.comun.EstadoError
import cl.ventanago.app.ui.comun.LocalAvisos
import cl.ventanago.app.ui.comun.MensajeError
import cl.ventanago.app.ui.modulos.soloDigitos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Perfil del proveedor: nombre, descripción, servicios que presta y comunas donde trabaja.
 * Las solicitudes nuevas le llegan solo si son de sus comunas y de alguno de sus servicios.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PantallaMiPerfil() {
    val avisar = LocalAvisos.current
    val alcance = rememberCoroutineScope()
    var cargado by remember { mutableStateOf<PerfilProveedor?>(null) }
    var errorCarga by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableIntStateOf(0) }

    var nombre by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var anios by remember { mutableStateOf("") }
    val servicios = remember { mutableStateListOf<String>() }
    val comunas = remember { mutableStateListOf<ComunaNombre>() }
    var comunaElegida by remember { mutableStateOf<Comuna?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }
    var agregandoRegion by remember { mutableStateOf(false) }

    LaunchedEffect(recarga) {
        try {
            val p = ProveedoresApi.miPerfil()
            cargado = p
            nombre = p.nombreComercial
            descripcion = p.descripcion
            telefono = p.telefono
            anios = p.aniosExperiencia.takeIf { it > 0 }?.toString().orEmpty()
            servicios.clear(); servicios += p.servicios
            comunas.clear(); comunas += p.comunas
            errorCarga = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorCarga = e.mensaje()
        }
    }

    /** Agrega de una vez todas las comunas de la región de la comuna elegida. */
    fun agregarRegion() = alcance.launch {
        val regionId = comunaElegida?.region?.regionId ?: return@launch
        agregandoRegion = true
        try {
            Recursos.comunas(regionId).forEach { c ->
                val id = c.comunaId ?: return@forEach
                if (comunas.none { it.comunaId == id }) comunas += ComunaNombre(id, c.nombre)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.mensaje()
        }
        agregandoRegion = false
    }

    fun guardar() {
        error = when {
            nombre.isBlank() -> "Ingresa el nombre de tu empresa o el tuyo."
            telefono.isNotBlank() && !telefonoValido(telefono) -> "Ingresa un teléfono chileno válido."
            servicios.isEmpty() -> "Elige al menos un servicio."
            comunas.isEmpty() -> "Agrega al menos una comuna donde trabajas."
            comunas.size > 60 -> "Puedes elegir hasta 60 comunas."
            else -> null
        }
        if (error != null) return
        alcance.launch {
            guardando = true
            try {
                cargado = ProveedoresApi.guardarPerfil(
                    PerfilProveedor(nombre.trim(), descripcion.trim(), telefono.trim(), anios.toIntOrNull() ?: 0, servicios = servicios.toList(), comunas = comunas.toList()),
                )
                avisar("Perfil guardado")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.mensaje()
            }
            guardando = false
        }
    }

    when {
        cargado == null && errorCarga != null -> EstadoError(errorCarga!!) { errorCarga = null; recarga++ }
        cargado == null -> EstadoCargando("Cargando tu perfil…")
        else -> Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Así te ven los clientes al comparar ofertas. Te llegan las solicitudes de tus comunas y servicios." +
                    if (cargado?.verificado == true) " Tu perfil está verificado." else "",
                color = Colores.gris,
            )
            error?.let { MensajeError(it) }
            CampoTexto("Nombre de tu empresa o el tuyo", nombre, { nombre = it.take(255) }, mayusculas = KeyboardCapitalization.Words)
            CampoTexto(
                "Descripción", descripcion, { descripcion = it.take(1000) }, lineas = 3,
                ayuda = "Qué haces, con qué materiales trabajas, garantía…",
            )
            CampoTexto("Teléfono", telefono, { telefono = it }, teclado = KeyboardType.Phone, ayuda = "Lo recibe el cliente que elige tu oferta.")
            CampoTexto("Años de experiencia", anios, { anios = soloDigitos(it).take(2) }, teclado = KeyboardType.Number)

            Text("Servicios que prestas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SERVICIOS.forEach { (valor, etiqueta) ->
                    val elegido = valor in servicios
                    FilterChip(
                        selected = elegido,
                        onClick = { if (elegido) servicios.remove(valor) else servicios.add(valor) },
                        label = { Text(etiqueta) },
                        leadingIcon = if (elegido) ({ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }) else null,
                    )
                }
            }

            Text("Comunas donde trabajas (${comunas.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            SelectorComuna(comunaElegida, { c ->
                comunaElegida = c
                val id = c?.comunaId
                if (id != null && comunas.none { it.comunaId == id }) comunas += ComunaNombre(id, c.nombre)
            })
            if (comunaElegida != null) {
                TextButton(onClick = { agregarRegion() }, enabled = !agregandoRegion) {
                    Text(if (agregandoRegion) "Agregando…" else "Agregar todas las comunas de ${comunaElegida?.region?.nombre ?: "la región"}")
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                comunas.sortedBy { it.nombre }.forEach { c ->
                    InputChip(
                        selected = false,
                        onClick = { comunas.remove(c) },
                        label = { Text(c.nombre) },
                        trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Quitar ${c.nombre}", modifier = Modifier.size(16.dp)) },
                    )
                }
            }
            if (comunas.isNotEmpty()) {
                TextButton(onClick = { comunas.clear() }) { Text("Quitar todas", color = Colores.error) }
            }

            Button(onClick = { guardar() }, enabled = !guardando, modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)) {
                if (guardando) CircularProgressIndicator(Modifier.size(22.dp), color = LocalContentColor.current, strokeWidth = 2.dp) else Text("Guardar perfil")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
