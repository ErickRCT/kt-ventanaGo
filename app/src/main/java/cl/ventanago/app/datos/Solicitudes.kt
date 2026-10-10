package cl.ventanago.app.datos

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

// Carrito, solicitudes de cotización y avisos del cliente (SolicitudDtos de ms-ventanaGo).

@Serializable
data class ItemVentana(
    val id: String? = null,
    /** Nombre de la pauta elegida. */
    val descripcion: String = "",
    val pautaId: Long? = null,
    val serieNombre: String? = null,
    /** Ruta relativa de la imagen de la pauta. */
    val imagenPauta: String? = null,
    val hojas: Int = 2,
    val anchoMm: Int = 0,
    val altoMm: Int = 0,
    val cantidad: Int = 1,
    val colorId: Long? = null,
    val colorNombre: String = "",
    val vidrioId: Long? = null,
    val vidrioNombre: String = "",
    val observaciones: String? = null,
    /** Lo define la empresa al responder. */
    val precioUnitario: Long? = null,
)

@Serializable
data class Contacto(val nombre: String = "", val email: String = "", val telefono: String = "", val direccion: String = "")

@Serializable
data class RespuestaEmpresa(
    val fecha: String? = null,
    val mensaje: String? = null,
    /** Total en pesos; null si se rechazó. */
    val total: Long? = null,
    val notificadoEnApp: Boolean = false,
    val notificadoPorCorreo: Boolean = false,
)

@Serializable
data class Solicitud(
    val numero: Long,
    val fecha: String? = null,
    val usuario: String? = null,
    val contacto: Contacto? = null,
    val servicios: List<String> = emptyList(),
    val observaciones: String? = null,
    val items: List<ItemVentana> = emptyList(),
    /** Solo si la empresa modificó la solicitud: lo que había pedido el cliente. */
    val itemsOriginales: List<ItemVentana>? = null,
    /** PENDIENTE, ACEPTADA, MODIFICADA o RECHAZADA. */
    val estado: String = "PENDIENTE",
    val respuesta: RespuestaEmpresa? = null,
)

@Serializable
data class NuevaSolicitud(val contacto: Contacto, val servicios: List<String>, val observaciones: String)

@Serializable
data class Aviso(
    val id: String,
    val fecha: String? = null,
    val solicitudNumero: Long? = null,
    val titulo: String = "",
    val mensaje: String = "",
    val leido: Boolean = false,
)

@Serializable
private data class CambioCantidad(val cantidad: Int)

@Serializable
private data class CorreoCodigo(val email: String)

@Serializable
private data class VerificacionCodigo(val email: String, val codigo: String)

val SERVICIOS = listOf("FABRICACION" to "Fabricación", "INSTALACION" to "Instalación", "FLETE" to "Flete")

fun nombreServicio(s: String) = SERVICIOS.firstOrNull { it.first == s }?.second ?: s

fun nombreEstadoSolicitud(e: String) = when (e) {
    "ACEPTADA" -> "Aceptada"
    "MODIFICADA" -> "Modificada"
    "RECHAZADA" -> "Rechazada"
    else -> "Pendiente"
}

object ClienteApi {
    private val listaItems = ListSerializer(ItemVentana.serializer())

    suspend fun carrito(): List<ItemVentana> = Api.lista("/carrito", ItemVentana.serializer())

    suspend fun agregar(item: ItemVentana): List<ItemVentana> =
        Api.json.decodeFromString(listaItems, Api.enviar("POST", "/carrito", item, ItemVentana.serializer()))

    suspend fun cambiarCantidad(id: String, cantidad: Int): List<ItemVentana> =
        Api.json.decodeFromString(listaItems, Api.enviar("PUT", "/carrito/$id/cantidad", CambioCantidad(cantidad), CambioCantidad.serializer()))

    suspend fun quitar(id: String): List<ItemVentana> = Api.json.decodeFromString(listaItems, Api.texto("DELETE", "/carrito/$id"))

    suspend fun solicitudes(): List<Solicitud> = Api.lista("/solicitudes", Solicitud.serializer())

    suspend fun enviarSolicitud(datos: NuevaSolicitud): Solicitud =
        Api.json.decodeFromString(Solicitud.serializer(), Api.enviar("POST", "/solicitudes", datos, NuevaSolicitud.serializer()))

    suspend fun avisos(): List<Aviso> = Api.lista("/avisos", Aviso.serializer())

    suspend fun marcarAvisosLeidos() {
        Api.texto("POST", "/avisos/leidos")
    }

    /** Envía al correo un código de 6 dígitos para entrar sin contraseña. */
    suspend fun solicitarCodigo(email: String) {
        Api.enviar("POST", "/auth/codigo", CorreoCodigo(email), CorreoCodigo.serializer())
    }

    suspend fun verificarCodigo(email: String, codigo: String): Sesion =
        Api.json.decodeFromString(
            Sesion.serializer(),
            Api.enviar("POST", "/auth/codigo/verificar", VerificacionCodigo(email, codigo), VerificacionCodigo.serializer()),
        )
}

/** Lo que la empresa decide sobre una solicitud. Solo se usan id, medidas, cantidad y precio de cada ventana. */
@Serializable
data class RespuestaSolicitud(
    /** ACEPTADA, MODIFICADA o RECHAZADA. */
    val estado: String,
    val mensaje: String,
    val items: List<ItemVentana>,
    val notificarEnApp: Boolean,
    val notificarPorCorreo: Boolean,
)

/** Panel de empresa (proveedores y administrador): solicitudes de los clientes. */
object EmpresaApi {
    /** Proveedores y administrador reciben todas. */
    suspend fun solicitudes(): List<Solicitud> = Api.lista("/solicitudes", Solicitud.serializer())

    suspend fun responder(numero: Long, respuesta: RespuestaSolicitud): Solicitud =
        Api.json.decodeFromString(
            Solicitud.serializer(),
            Api.enviar("POST", "/solicitudes/$numero/respuesta", respuesta, RespuestaSolicitud.serializer()),
        )

    suspend fun pauta(id: Long): Pauta = Api.get("/pauta/$id", Pauta.serializer())
}
