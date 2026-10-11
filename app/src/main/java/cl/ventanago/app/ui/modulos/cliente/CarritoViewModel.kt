package cl.ventanago.app.ui.modulos.cliente

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.viewModelScope
import cl.ventanago.app.datos.BorradorSolicitud
import cl.ventanago.app.datos.CarritoLocal
import cl.ventanago.app.datos.ClienteApi
import cl.ventanago.app.datos.Comuna
import cl.ventanago.app.datos.Contacto
import cl.ventanago.app.datos.FotoNueva
import cl.ventanago.app.datos.ItemVentana
import cl.ventanago.app.datos.NuevaSolicitud
import cl.ventanago.app.datos.PrecioReferencia
import cl.ventanago.app.datos.ProveedorResumen
import cl.ventanago.app.datos.ProveedoresApi
import cl.ventanago.app.datos.SERVICIOS
import cl.ventanago.app.datos.emailValido
import cl.ventanago.app.datos.mensaje
import cl.ventanago.app.datos.telefonoValido
import cl.ventanago.app.ui.comun.ModeloBase
import cl.ventanago.app.ui.modulos.mercado.comprimirFoto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

const val MAX_FOTOS = 4
const val MAX_PROVEEDORES = 10

/**
 * Carrito y formulario de solicitud. Con [conCuenta] el carrito está en el backend; sin cuenta vive en el
 * teléfono (CarritoLocal) y al enviar se pide entrar. Lo escrito en el formulario se guarda como borrador
 * mientras se escribe, para no perderlo al entrar a la cuenta o cerrar la app.
 */
class CarritoViewModel(val conCuenta: Boolean, emailCuenta: String) : ModeloBase() {
    var itemsCuenta by mutableStateOf<List<ItemVentana>?>(null)
        private set
    var errorCarga by mutableStateOf<String?>(null)
        private set
    /** Texto de la acción en curso ("Enviando…"); null si no hay ninguna. */
    var ocupado by mutableStateOf<String?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    // Formulario: parte del borrador guardado (si se escribió antes de entrar).
    private val borrador = CarritoLocal.borrador
    var nombre by mutableStateOf(borrador?.contacto?.nombre.orEmpty())
    var email by mutableStateOf(borrador?.contacto?.email?.takeIf { it.isNotBlank() } ?: emailCuenta)
    var telefono by mutableStateOf(borrador?.contacto?.telefono.orEmpty())
    var direccion by mutableStateOf(borrador?.contacto?.direccion.orEmpty())
    val servicios = mutableStateListOf(*(borrador?.servicios ?: listOf("FABRICACION")).toTypedArray())
    var observaciones by mutableStateOf(borrador?.observaciones.orEmpty())
    var comuna by mutableStateOf(borrador?.comuna)
        private set
    var elegirProveedores by mutableStateOf(borrador?.proveedores?.isNotEmpty() == true)
    val elegidos = mutableStateListOf(*(borrador?.proveedores ?: emptyList()).toTypedArray())
    var sinVentanas by mutableStateOf(borrador?.sinVentanas == true)
    val fotos = mutableStateListOf<Pair<FotoNueva, ImageBitmap>>()
    /** true después del primer intento de envío: desde ahí se marcan los errores en rojo. */
    var validar by mutableStateOf(false)
        private set
    /** Número de la solicitud recién enviada, para el diálogo de confirmación. */
    var enviada by mutableStateOf<Long?>(null)

    /** Proveedores que atienden la comuna: para contar a cuántos llega la solicitud o elegir algunos. */
    var proveedoresZona by mutableStateOf<List<ProveedorResumen>?>(null)
        private set
    var precioReferencia by mutableStateOf<PrecioReferencia?>(null)
        private set

    init {
        cargar()
        viewModelScope.launch { recordarContacto() }
        viewModelScope.launch { precioReferencia = intentar { ProveedoresApi.precioReferencia() } }
        viewModelScope.launch {
            snapshotFlow { borradorActual() }.collect { if (enviada == null) CarritoLocal.borrador = it }
        }
        viewModelScope.launch {
            snapshotFlow { comuna?.comunaId }.collectLatest { id ->
                proveedoresZona = null
                if (id != null) proveedoresZona = intentar { ProveedoresApi.listar(comunaId = id) } ?: emptyList()
            }
        }
    }

    private fun borradorActual() = BorradorSolicitud(
        Contacto(nombre, email, telefono, direccion), servicios.toList(), observaciones, comuna,
        if (elegirProveedores) elegidos.toList() else emptyList(), sinVentanas,
    )

    fun cargar() {
        if (!conCuenta) return
        errorCarga = null
        viewModelScope.launch {
            try {
                itemsCuenta = ClienteApi.carrito()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorCarga = e.mensaje()
            }
        }
    }

    /** Los datos de contacto se recuerdan de la última solicitud, como en la web. */
    private suspend fun recordarContacto() {
        if (!conCuenta) return
        val c = intentar { ClienteApi.solicitudes() }?.maxByOrNull { it.numero }?.contacto ?: return
        if (nombre.isBlank()) nombre = c.nombre
        if (telefono.isBlank()) telefono = c.telefono
        if (direccion.isBlank()) direccion = c.direccion
        if (c.email.isNotBlank()) email = c.email
    }

    /** Sin conexión o sin datos: null (son datos de ayuda, la pantalla funciona igual). */
    private suspend fun <T> intentar(bloque: suspend () -> T): T? = try {
        bloque()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    // ---------- Lo que se muestra ----------

    val lista: List<ItemVentana>? get() = if (conCuenta) itemsCuenta else CarritoLocal.items
    val conVentanas get() = !lista.isNullOrEmpty()

    /** Con ventanas en el carrito el formulario es para cotizarlas; sin ventanas, para pedir un servicio. */
    val modoServicio get() = !conVentanas && sinVentanas
    val serviciosVisibles get() = if (modoServicio) SERVICIOS.filter { it.first != "FABRICACION" } else SERVICIOS
    val serviciosElegidos get() = servicios.filter { s -> serviciosVisibles.any { it.first == s } }
    val proveedoresAptos get() = proveedoresZona?.filter { p -> p.servicios.isEmpty() || serviciosElegidos.any { it in p.servicios } }
    val necesitaDireccion get() = serviciosElegidos.any { it != "FABRICACION" }

    val errorNombre get() = "Ingresa tu nombre.".takeIf { nombre.isBlank() }
    val errorEmail get() = "Ingresa un correo válido.".takeIf { !emailValido(email) }
    val errorTelefono get() = "Ingresa un teléfono chileno válido.".takeIf { telefono.isNotBlank() && !telefonoValido(telefono) }
    val errorDireccion get() = "Indica dónde se hará el trabajo.".takeIf { necesitaDireccion && direccion.isBlank() }
    val errorServicios get() = "Elige al menos un servicio.".takeIf { serviciosElegidos.isEmpty() }
    val errorComuna get() = "Elige la comuna donde se hará el trabajo.".takeIf { comuna == null }
    val errorDescripcion get() = "Describe el trabajo (al menos 10 caracteres).".takeIf { modoServicio && observaciones.trim().length < 10 }
    val errorProveedores get() = "Elige al menos un proveedor, o envíala a todos los de tu zona.".takeIf { elegirProveedores && elegidos.isEmpty() }

    // ---------- Acciones ----------

    private fun accion(texto: String, bloque: suspend () -> Unit) {
        if (ocupado != null) return
        viewModelScope.launch {
            ocupado = texto
            error = null
            try {
                bloque()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.mensaje()
            }
            ocupado = null
        }
    }

    fun cambiarCantidad(item: ItemVentana, cantidad: Int) = accion("Actualizando…") {
        if (conCuenta) itemsCuenta = ClienteApi.cambiarCantidad(item.id!!, cantidad) else CarritoLocal.cambiarCantidad(item.id!!, cantidad)
    }

    /** Quita la ventana; [alQuitar] se llama si se pudo (la pantalla ofrece deshacer). */
    fun quitar(item: ItemVentana, alQuitar: () -> Unit = {}) = accion("Quitando…") {
        if (conCuenta) itemsCuenta = ClienteApi.quitar(item.id!!) else CarritoLocal.quitar(item.id!!)
        alQuitar()
    }

    /** Deshacer: vuelve a agregar una ventana recién quitada (queda al final del carrito). */
    fun restaurar(item: ItemVentana) = accion("Restaurando…") {
        if (conCuenta) itemsCuenta = ClienteApi.agregar(item.copy(id = null)) else CarritoLocal.agregar(item)
    }

    fun alternarServicio(valor: String) {
        if (valor in servicios) servicios.remove(valor) else servicios.add(valor)
    }

    /** Sin ventanas: pedir una reparación u otro servicio. */
    fun pedirServicio() {
        sinVentanas = true
        servicios.remove("FABRICACION")
        if (servicios.isEmpty()) servicios += "REPARACION"
    }

    fun elegirComuna(nueva: Comuna?) {
        comuna = nueva
        elegidos.clear()
    }

    fun marcarProveedor(id: Long, marcado: Boolean) {
        if (!marcado) elegidos -= id else if (elegidos.size < MAX_PROVEEDORES) elegidos += id
    }

    fun agregarFotos(context: Context, uris: List<Uri>) = accion("Preparando fotos…") {
        var fallidas = 0
        for (uri in uris.take(MAX_FOTOS - fotos.size)) {
            comprimirFoto(context.applicationContext, uri)?.let { fotos += it } ?: fallidas++
        }
        if (fallidas > 0) error = "No se pudieron leer $fallidas foto(s)."
    }

    /**
     * Envía la solicitud. Sin cuenta devuelve false: las ventanas y los datos ya están guardados en el teléfono y
     * pasan a la cuenta al entrar.
     */
    fun enviar(): Boolean {
        validar = true
        if (listOf(errorNombre, errorEmail, errorTelefono, errorDireccion, errorServicios, errorComuna, errorDescripcion, errorProveedores).any { it != null }) {
            error = "Revisa los datos marcados en rojo."
            return true
        }
        if (!conCuenta) return false
        accion("Enviando…") {
            val solicitud = ClienteApi.enviarSolicitud(
                NuevaSolicitud(
                    Contacto(nombre.trim(), email.trim(), telefono.trim(), direccion.trim()),
                    serviciosElegidos,
                    observaciones.trim(),
                    comuna?.comunaId,
                    if (elegirProveedores) elegidos.toList() else emptyList(),
                    fotos.map { it.first },
                ),
            )
            enviada = solicitud.numero
            CarritoLocal.borrador = null
            itemsCuenta = emptyList()
            sinVentanas = false
            fotos.clear()
        }
        return true
    }
}
