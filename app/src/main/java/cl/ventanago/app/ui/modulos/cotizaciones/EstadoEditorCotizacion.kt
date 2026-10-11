package cl.ventanago.app.ui.modulos.cotizaciones

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import cl.ventanago.app.datos.Cliente
import cl.ventanago.app.datos.Cotizacion
import cl.ventanago.app.datos.Cotizaciones
import cl.ventanago.app.datos.Documentos
import cl.ventanago.app.datos.Recursos
import cl.ventanago.app.datos.Ventana
import cl.ventanago.app.datos.estadosCotizacion
import cl.ventanago.app.datos.mensaje
import cl.ventanago.app.ui.modulos.soloDigitos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

fun numero(texto: String) = texto.toIntOrNull() ?: 0

/** Porcentaje entre 0 y 100. */
fun porcentaje(texto: String) = soloDigitos(texto).take(3)

/** Estado del editor de cotizaciones (vive mientras el diálogo está abierto). */
@Composable
fun rememberEstadoEditorCotizacion(id: Long?): EstadoEditorCotizacion {
    val alcance = rememberCoroutineScope()
    val context = LocalContext.current.applicationContext
    return remember(id) { EstadoEditorCotizacion(id, alcance, context).also { it.cargar() } }
}

/**
 * Datos y acciones del editor de una cotización: las ventanas se guardan en el servidor apenas se agregan; los
 * datos y precios, con [guardar] (o antes de generar un PDF).
 */
class EstadoEditorCotizacion(private val id: Long?, private val alcance: CoroutineScope, private val context: Context) {
    var cotizacion by mutableStateOf<Cotizacion?>(null)
        private set
    var errorCarga by mutableStateOf<String?>(null)
        private set
    var error by mutableStateOf<String?>(null)
    /** Texto de la tarea en curso ("Guardando…"); null si no hay ninguna. */
    var trabajando by mutableStateOf<String?>(null)
        private set
    /** Se guardó algo: al cerrar se recarga la lista. */
    var huboCambios by mutableStateOf(false)
        private set
    var sinGuardar by mutableStateOf(false)

    // Campos editables
    var nombre by mutableStateOf("")
    var cliente by mutableStateOf<Cliente?>(null)
    var estadoCot by mutableStateOf("CREADA")
    var ganancia by mutableStateOf("50")
    var descuento by mutableStateOf("0")
    var flete by mutableStateOf("")
    var valorFlete by mutableStateOf("")
    var instalacion by mutableStateOf("")
    var valorInstalacion by mutableStateOf("")
    var valorManoDeObra by mutableStateOf("")
    var otrosGastos by mutableStateOf("")
    var valorOtrosGastos by mutableStateOf("")
    var condiciones by mutableStateOf("")

    // Diálogos
    var agregandoVentana by mutableStateOf(false)
    var eligiendoCliente by mutableStateOf(false)
    var aEliminar by mutableStateOf<Ventana?>(null)
    var confirmarSalida by mutableStateOf(false)
    var clientes by mutableStateOf<List<Cliente>?>(null)
        private set

    /** Cambia un campo y marca que hay cambios sin guardar. */
    fun editar(cambio: EstadoEditorCotizacion.() -> Unit) {
        cambio()
        sinGuardar = true
    }

    private fun cargarCampos(c: Cotizacion) {
        nombre = c.nombreCotizacion.orEmpty()
        cliente = c.cliente
        estadoCot = estadosCotizacion.firstOrNull { it.first.equals(c.estado, true) }?.first ?: "CREADA"
        ganancia = (c.ganancia ?: 0).toString()
        descuento = (c.descuento ?: 0).toString()
        flete = c.flete.orEmpty()
        valorFlete = c.valorFlete?.takeIf { it > 0 }?.toString().orEmpty()
        instalacion = c.instalacion.orEmpty()
        valorInstalacion = c.valorInstalacion?.takeIf { it > 0 }?.toString().orEmpty()
        valorManoDeObra = c.valorManoDeObra?.takeIf { it > 0 }?.toString().orEmpty()
        otrosGastos = c.otrosGastos.orEmpty()
        valorOtrosGastos = c.valorOtrosGastos?.takeIf { it > 0 }?.toString().orEmpty()
        condiciones = c.condiciones.orEmpty()
        sinGuardar = false
    }

    fun cargar() {
        if (id == null && cotizacion == null) {
            val nueva = Cotizacion(fecha = LocalDate.now().toString(), ganancia = 50, descuento = 0, estado = "CREADA", ventanas = emptyList())
            cotizacion = nueva
            cargarCampos(nueva)
            return
        }
        val idCargar = cotizacion?.cotizacionId ?: id ?: return
        errorCarga = null
        alcance.launch {
            try {
                val c = Cotizaciones.obtener(idCargar)
                cotizacion = c
                cargarCampos(c)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorCarga = e.mensaje()
            }
        }
    }

    /** La cotización con lo que hay en pantalla (para el resumen y para guardar). */
    fun actual(): Cotizacion = (cotizacion ?: Cotizacion()).copy(
        nombreCotizacion = nombre.trim().ifBlank { null },
        cliente = cliente,
        estado = estadoCot,
        ganancia = numero(ganancia),
        descuento = numero(descuento).coerceAtMost(100),
        flete = flete.trim().ifBlank { null },
        valorFlete = numero(valorFlete),
        instalacion = instalacion.trim().ifBlank { null },
        valorInstalacion = numero(valorInstalacion),
        valorManoDeObra = numero(valorManoDeObra),
        otrosGastos = otrosGastos.trim().ifBlank { null },
        valorOtrosGastos = numero(valorOtrosGastos),
        condiciones = condiciones.trim().ifBlank { null },
    )

    /** Guarda los datos y precios; si la cotización aún no existe, la crea. Devuelve el id. */
    suspend fun guardar(): Long {
        val datos = actual()
        val idGuardado = if (datos.cotizacionId == null) {
            Cotizaciones.agregar(datos).cotizacionId
                ?: throw IllegalStateException("El servidor no devolvió el número de la cotización.")
        } else {
            Cotizaciones.modificar(datos)
            datos.cotizacionId
        }
        val fresca = Cotizaciones.obtener(idGuardado)
        cotizacion = fresca
        cargarCampos(fresca)
        huboCambios = true
        return idGuardado
    }

    fun tarea(descripcion: String, accion: suspend () -> Unit) {
        if (trabajando != null) return
        alcance.launch {
            trabajando = descripcion
            error = null
            try {
                accion()
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalStateException) {
                error = e.message
            } catch (e: Exception) {
                error = e.mensaje()
            } finally {
                trabajando = null
            }
        }
    }

    fun documento(ruta: (Long) -> String, archivo: (Long) -> String, compartir: Boolean = false) = tarea("Generando PDF…") {
        if (actual().ventanas.isNullOrEmpty()) {
            error = "Agrega al menos una ventana antes de generar el documento."
            return@tarea
        }
        val idDoc = if (sinGuardar || cotizacion?.cotizacionId == null) guardar() else cotizacion!!.cotizacionId!!
        val pdf: File = Documentos.descargar(context, ruta(idDoc), archivo(idDoc))
        if (compartir) Documentos.compartir(context, pdf) else Documentos.abrir(context, pdf)
    }

    /** La ventana necesita una cotización guardada a la cual pertenecer. */
    suspend fun agregarVentana(ventana: Ventana) {
        val idCot = cotizacion?.cotizacionId ?: guardar()
        Cotizaciones.agregarVentana(ventana.copy(cotizacionId = idCot))
        cotizacion = Cotizaciones.obtener(idCot)
        huboCambios = true
    }

    fun quitarVentana(v: Ventana) = tarea("Quitando ventana…") {
        Cotizaciones.eliminarVentana(v.ventanaId!!)
        cotizacion = Cotizaciones.obtener(cotizacion!!.cotizacionId!!)
        huboCambios = true
        aEliminar = null
    }

    fun abrirClientes() {
        eligiendoCliente = true
        if (clientes != null) return
        alcance.launch {
            clientes = try {
                Recursos.clientes.listar().sortedBy { it.nombre.lowercase() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.mensaje()
                emptyList()
            }
        }
    }

    fun elegirCliente(c: Cliente) = editar {
        cliente = c
        eligiendoCliente = false
    }
}
