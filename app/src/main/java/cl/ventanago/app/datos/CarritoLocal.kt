package cl.ventanago.app.datos

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.util.UUID

/** Datos del formulario de solicitud que se recuerdan mientras el cliente aún no entra a su cuenta. */
@Serializable
data class BorradorSolicitud(
    val contacto: Contacto = Contacto(),
    val servicios: List<String> = listOf("FABRICACION"),
    val observaciones: String = "",
    val comuna: Comuna? = null,
    /** Proveedores elegidos; vacía: todos los de la comuna. */
    val proveedores: List<Long> = emptyList(),
    /** true: pide un servicio sin ventanas (reparación, visita técnica…). */
    val sinVentanas: Boolean = false,
)

/**
 * Carrito de quien diseña sin cuenta, guardado en el teléfono. Al entrar como cliente, sus ventanas se pasan
 * al carrito de la cuenta (backend) y este se vacía.
 */
object CarritoLocal {
    private const val CLAVE_ITEMS = "carrito_local"
    private const val CLAVE_BORRADOR = "borrador_solicitud"
    private lateinit var preferencias: SharedPreferences
    private val serializador = ListSerializer(ItemVentana.serializer())

    var items by mutableStateOf<List<ItemVentana>>(emptyList())
        private set

    fun iniciar(context: Context) {
        if (::preferencias.isInitialized) return
        preferencias = context.getSharedPreferences("ventanago", Context.MODE_PRIVATE)
        items = try {
            preferencias.getString(CLAVE_ITEMS, null)?.let { Api.json.decodeFromString(serializador, it) }.orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun guardar(nuevos: List<ItemVentana>) {
        items = nuevos
        preferencias.edit { putString(CLAVE_ITEMS, Api.json.encodeToString(serializador, nuevos)) }
    }

    fun agregar(item: ItemVentana): List<ItemVentana> {
        guardar(items + item.copy(id = UUID.randomUUID().toString()))
        return items
    }

    fun cambiarCantidad(id: String, cantidad: Int): List<ItemVentana> {
        guardar(items.map { if (it.id == id) it.copy(cantidad = cantidad.coerceIn(1, 99)) else it })
        return items
    }

    fun quitar(id: String): List<ItemVentana> {
        guardar(items.filterNot { it.id == id })
        return items
    }

    fun vaciar() = guardar(emptyList())

    var borrador: BorradorSolicitud?
        get() = try {
            preferencias.getString(CLAVE_BORRADOR, null)?.let { Api.json.decodeFromString(BorradorSolicitud.serializer(), it) }
        } catch (_: Exception) {
            null
        }
        set(valor) = preferencias.edit {
            if (valor == null) remove(CLAVE_BORRADOR) else putString(CLAVE_BORRADOR, Api.json.encodeToString(BorradorSolicitud.serializer(), valor))
        }

    /**
     * Pasa las ventanas guardadas en el teléfono al carrito de la cuenta. Las que fallen quedan en el teléfono
     * para reintentar. Devuelve cuántas se pasaron.
     */
    suspend fun pasarACuenta(): Int {
        var pasadas = 0
        val pendientes = mutableListOf<ItemVentana>()
        for (item in items) {
            try {
                ClienteApi.agregar(item.copy(id = null))
                pasadas++
            } catch (_: Exception) {
                pendientes += item
            }
        }
        guardar(pendientes)
        return pasadas
    }
}
