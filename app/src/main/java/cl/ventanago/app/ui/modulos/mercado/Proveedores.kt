package cl.ventanago.app.ui.modulos.mercado

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.ventanago.app.datos.Comuna
import cl.ventanago.app.datos.ProveedorResumen
import cl.ventanago.app.datos.ProveedoresApi
import cl.ventanago.app.datos.SERVICIOS
import cl.ventanago.app.datos.mensaje
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.comun.EstadoCargando
import cl.ventanago.app.ui.comun.EstadoError
import cl.ventanago.app.ui.comun.EstadoMensaje
import cl.ventanago.app.ui.comun.LocalAvisos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Proveedores por comuna y servicio, con su nota y trabajos. Cualquiera la puede ver (también sin cuenta);
 * el administrador además marca a los proveedores como verificados.
 */
@Composable
fun PantallaProveedores(esAdmin: Boolean = false) {
    val avisar = LocalAvisos.current
    val alcance = rememberCoroutineScope()
    var comuna by remember { mutableStateOf<Comuna?>(null) }
    var servicio by remember { mutableStateOf<String?>(null) }
    var proveedores by remember { mutableStateOf<List<ProveedorResumen>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableIntStateOf(0) }
    var abierto by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(comuna?.comunaId, servicio, recarga) {
        proveedores = null
        try {
            proveedores = ProveedoresApi.listar(comuna?.comunaId, servicio)
            error = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.mensaje()
        }
    }

    fun verificar(p: ProveedorResumen, si: Boolean) = alcance.launch {
        try {
            ProveedoresApi.verificar(p.cuentaId, si)
            proveedores = proveedores?.map { if (it.cuentaId == p.cuentaId) it.copy(verificado = si) else it }
            avisar(if (si) "${p.nombre} quedó verificado" else "${p.nombre} ya no está verificado")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            avisar(e.mensaje())
        }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (esAdmin) "Revisa a los proveedores y marca como verificados a los que cumplan (RUT y trabajos comprobados)."
                    else "Empresas y maestros que trabajan en tu zona. Al pedir cotización puedes enviarla a todos o elegir algunos.",
                    color = Colores.gris,
                )
                SelectorComuna(comuna, { comuna = it })
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = servicio == null, onClick = { servicio = null }, label = { Text("Todos") })
                    SERVICIOS.forEach { (valor, etiqueta) ->
                        FilterChip(selected = servicio == valor, onClick = { servicio = if (servicio == valor) null else valor }, label = { Text(etiqueta) })
                    }
                }
            }
        }
        val lista = proveedores
        when {
            lista == null && error != null -> item { EstadoError(error!!) { error = null; recarga++ } }
            lista == null -> item { EstadoCargando("Buscando proveedores…") }
            lista.isEmpty() -> item {
                EstadoMensaje(Icons.Outlined.Storefront, "No hay proveedores con esos filtros", "Prueba con otra comuna o servicio.")
            }
            else -> items(lista, key = { it.cuentaId }) { p ->
                TarjetaProveedor(p, alTocar = { abierto = p.cuentaId }) {
                    if (esAdmin) {
                        Text("Verificado ", style = MaterialTheme.typography.labelSmall, color = Colores.gris)
                        Switch(checked = p.verificado, onCheckedChange = { verificar(p, it) }, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        }
    }

    abierto?.let { DialogoProveedor(it) { abierto = null } }
}
