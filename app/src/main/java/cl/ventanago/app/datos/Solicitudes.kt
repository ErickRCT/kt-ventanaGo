package cl.ventanago.app.datos

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.net.URLEncoder

// Carrito, solicitudes, ofertas de los proveedores, chat y avisos (SolicitudDtos y ProveedorDtos de ms-ventanaGo).

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
    /** Lo define el proveedor en su oferta. */
    val precioUnitario: Long? = null,
)

@Serializable
data class Contacto(val nombre: String = "", val email: String = "", val telefono: String = "", val direccion: String = "")

@Serializable
data class ComunaNombre(val comunaId: Long, val nombre: String = "")

@Serializable
data class ComunaRef(val comunaId: Long, val nombre: String = "", val region: String? = null)

/** Lo que el cliente ve de un proveedor al comparar ofertas o buscar en su zona. */
@Serializable
data class ProveedorResumen(
    val cuentaId: Long,
    val nombre: String = "",
    val descripcion: String? = null,
    val verificado: Boolean = false,
    val aniosExperiencia: Int = 0,
    /** Promedio de estrellas (1 a 5); null si aún no tiene valoraciones. */
    val promedio: Double? = null,
    val valoraciones: Long = 0,
    val trabajos: Long = 0,
    val servicios: List<String> = emptyList(),
    val comunas: List<ComunaNombre> = emptyList(),
)

@Serializable
data class Valoracion(val estrellas: Int, val comentario: String? = null, val fecha: String? = null, val autor: String? = null)

@Serializable
data class ProveedorDetalle(val proveedor: ProveedorResumen, val valoraciones: List<Valoracion> = emptyList())

@Serializable
data class Oferta(
    val id: Long,
    val numero: Long = 0,
    val proveedor: ProveedorResumen? = null,
    val fecha: String? = null,
    /** ACEPTADA (lo pedido con precio), MODIFICADA (con cambios) o RECHAZADA (no toma el trabajo). */
    val tipo: String = "ACEPTADA",
    /** VIGENTE, ELEGIDA o DESCARTADA. */
    val estado: String = "VIGENTE",
    val mensaje: String? = null,
    val total: Long? = null,
    val plazoDias: Int? = null,
    val items: List<ItemVentana> = emptyList(),
    /** Solo cuando el cliente eligió esta oferta. */
    val contactoProveedor: Contacto? = null,
    val mensajes: Long = 0,
)

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
    /** Para el proveedor, solo el primer nombre hasta que el cliente elija su oferta. */
    val contacto: Contacto? = null,
    val servicios: List<String> = emptyList(),
    val observaciones: String? = null,
    /** Lo que pidió el cliente; vacía si describe un trabajo sin ventanas. */
    val items: List<ItemVentana> = emptyList(),
    /** PENDIENTE (recibiendo ofertas), ADJUDICADA, TERMINADA o CANCELADA. */
    val estado: String = "PENDIENTE",
    val respuesta: RespuestaEmpresa? = null,
    val comuna: ComunaRef? = null,
    val invitados: List<Long> = emptyList(),
    /** Solo para el cliente (y el administrador). */
    val ofertas: List<Oferta>? = null,
    /** Solo para el proveedor: la oferta que envió. */
    val miOferta: Oferta? = null,
    val cantidadOfertas: Int = 0,
    val fotos: List<Long> = emptyList(),
    val valoracion: Valoracion? = null,
)

@Serializable
data class FotoNueva(val tipoContenido: String, val datos: String)

@Serializable
data class NuevaSolicitud(
    val contacto: Contacto,
    val servicios: List<String>,
    val observaciones: String,
    val comunaId: Long?,
    /** Vacía: todos los proveedores de la comuna. */
    val proveedores: List<Long>,
    val fotos: List<FotoNueva>,
)

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
data class MensajeChat(val id: Long, val fecha: String? = null, val autor: String = "", val mio: Boolean = false, val texto: String = "")

/** Precio por m² de las ventanas ofrecidas; los valores son null con menos de 3 muestras. */
@Serializable
data class PrecioReferencia(val muestras: Int = 0, val p25PorM2: Long? = null, val medianaPorM2: Long? = null, val p75PorM2: Long? = null)

@Serializable
data class PerfilProveedor(
    val nombreComercial: String = "",
    val descripcion: String = "",
    val telefono: String = "",
    val aniosExperiencia: Int = 0,
    val verificado: Boolean = false,
    val servicios: List<String> = emptyList(),
    val comunas: List<ComunaNombre> = emptyList(),
)

@Serializable
private data class PerfilRequest(
    val nombreComercial: String,
    val descripcion: String,
    val telefono: String,
    val aniosExperiencia: Int,
    val servicios: List<String>,
    val comunas: List<Long>,
)

@Serializable
private data class CambioCantidad(val cantidad: Int)

@Serializable
private data class CorreoCodigo(val email: String)

@Serializable
private data class VerificacionCodigo(val email: String, val codigo: String)

@Serializable
private data class NuevaValoracion(val estrellas: Int, val comentario: String)

@Serializable
private data class NuevoMensaje(val texto: String)

@Serializable
private data class Verificado(val verificado: Boolean)

/** Los cuatro últimos no necesitan ventanas diseñadas: el cliente describe el trabajo. */
val SERVICIOS = listOf(
    "FABRICACION" to "Fabricación",
    "INSTALACION" to "Instalación",
    "FLETE" to "Flete",
    "REPARACION" to "Reparación",
    "CAMBIO_VIDRIO" to "Cambio de vidrio",
    "MANTENCION" to "Mantención",
    "MEDICION" to "Visita técnica para medir",
)

/** Servicios que se pueden pedir sin ventanas en el carrito. */
val SERVICIOS_SIN_VENTANAS = setOf("REPARACION", "CAMBIO_VIDRIO", "MANTENCION", "MEDICION", "INSTALACION")

fun nombreServicio(s: String) = SERVICIOS.firstOrNull { it.first == s }?.second ?: s

fun nombreEstadoSolicitud(e: String) = when (e) {
    "ADJUDICADA" -> "Adjudicada"
    "TERMINADA" -> "Terminada"
    "CANCELADA" -> "Cancelada"
    else -> "Recibiendo ofertas"
}

/** Ofertas que el cliente puede comparar (las de quienes sí toman el trabajo). */
fun Solicitud.ofertasValidas(): List<Oferta> = ofertas.orEmpty().filter { it.tipo != "RECHAZADA" }

fun Solicitud.ofertaElegida(): Oferta? = ofertas?.firstOrNull { it.estado == "ELEGIDA" }

private fun codificar(texto: String) = URLEncoder.encode(texto, "UTF-8")

object ClienteApi {
    private val listaItems = ListSerializer(ItemVentana.serializer())

    /** Ventanas en el carrito de la cuenta según la última respuesta del backend (el número de la barra inferior). */
    var unidadesCarrito by mutableStateOf<Int?>(null)
        private set

    private fun List<ItemVentana>.contadas() = also { unidadesCarrito = sumOf { it.cantidad } }

    suspend fun carrito(): List<ItemVentana> = Api.lista("/carrito", ItemVentana.serializer()).contadas()

    suspend fun agregar(item: ItemVentana): List<ItemVentana> =
        Api.json.decodeFromString(listaItems, Api.enviar("POST", "/carrito", item, ItemVentana.serializer())).contadas()

    suspend fun cambiarCantidad(id: String, cantidad: Int): List<ItemVentana> =
        Api.json.decodeFromString(listaItems, Api.enviar("PUT", "/carrito/$id/cantidad", CambioCantidad(cantidad), CambioCantidad.serializer())).contadas()

    suspend fun quitar(id: String): List<ItemVentana> = Api.json.decodeFromString(listaItems, Api.texto("DELETE", "/carrito/$id")).contadas()

    suspend fun solicitudes(): List<Solicitud> = Api.lista("/solicitudes", Solicitud.serializer())

    /** Al enviarla, el backend vacía el carrito. */
    suspend fun enviarSolicitud(datos: NuevaSolicitud): Solicitud =
        Api.json.decodeFromString(Solicitud.serializer(), Api.enviar("POST", "/solicitudes", datos, NuevaSolicitud.serializer())).also { unidadesCarrito = 0 }

    suspend fun elegirOferta(numero: Long, ofertaId: Long): Solicitud =
        Api.json.decodeFromString(Solicitud.serializer(), Api.texto("POST", "/solicitudes/$numero/ofertas/$ofertaId/elegir"))

    suspend fun cancelar(numero: Long): Solicitud =
        Api.json.decodeFromString(Solicitud.serializer(), Api.texto("POST", "/solicitudes/$numero/cancelar"))

    suspend fun valorar(numero: Long, estrellas: Int, comentario: String): Solicitud =
        Api.json.decodeFromString(
            Solicitud.serializer(),
            Api.enviar("POST", "/solicitudes/$numero/valoracion", NuevaValoracion(estrellas, comentario), NuevaValoracion.serializer()),
        )

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

/** Proveedores (público) y el perfil propio del proveedor. */
object ProveedoresApi {
    suspend fun listar(comunaId: Long? = null, servicio: String? = null): List<ProveedorResumen> {
        val filtros = listOfNotNull(comunaId?.let { "comunaId=$it" }, servicio?.let { "servicio=${codificar(it)}" })
        return Api.lista("/proveedores" + (if (filtros.isEmpty()) "" else "?" + filtros.joinToString("&")), ProveedorResumen.serializer())
    }

    suspend fun detalle(id: Long): ProveedorDetalle = Api.get("/proveedores/$id", ProveedorDetalle.serializer())

    suspend fun miPerfil(): PerfilProveedor = Api.get("/proveedor/perfil", PerfilProveedor.serializer())

    suspend fun guardarPerfil(p: PerfilProveedor): PerfilProveedor =
        Api.json.decodeFromString(
            PerfilProveedor.serializer(),
            Api.enviar(
                "PUT", "/proveedor/perfil",
                PerfilRequest(p.nombreComercial, p.descripcion, p.telefono, p.aniosExperiencia, p.servicios, p.comunas.map { it.comunaId }),
                PerfilRequest.serializer(),
            ),
        )

    /** Solo el administrador. */
    suspend fun verificar(id: Long, verificado: Boolean) {
        Api.enviar("PUT", "/proveedores/$id/verificado", Verificado(verificado), Verificado.serializer())
    }

    suspend fun precioReferencia(): PrecioReferencia = Api.get("/precios/referencia", PrecioReferencia.serializer())
}

/** Chat entre el cliente y el proveedor de una oferta. */
object ChatApi {
    suspend fun mensajes(ofertaId: Long): List<MensajeChat> = Api.lista("/ofertas/$ofertaId/mensajes", MensajeChat.serializer())

    suspend fun enviar(ofertaId: Long, texto: String): MensajeChat =
        Api.json.decodeFromString(MensajeChat.serializer(), Api.enviar("POST", "/ofertas/$ofertaId/mensajes", NuevoMensaje(texto), NuevoMensaje.serializer()))

    /** Foto adjunta a una solicitud (JPEG, PNG o WebP). */
    suspend fun foto(numero: Long, fotoId: Long): ByteArray = Api.descargar("/solicitudes/$numero/fotos/$fotoId")
}

/** Oferta del proveedor. Solo se usan id, medidas, cantidad y precio de cada ventana. */
@Serializable
data class RespuestaSolicitud(
    /** ACEPTADA, MODIFICADA o RECHAZADA. */
    val estado: String,
    val mensaje: String,
    val items: List<ItemVentana>,
    val notificarEnApp: Boolean,
    val notificarPorCorreo: Boolean,
    /** Solo en solicitudes sin ventanas. */
    val total: Long? = null,
    val plazoDias: Int? = null,
)

/** Panel de empresa (proveedores y administrador): solicitudes que les llegan y sus ofertas. */
object EmpresaApi {
    /** Cada proveedor recibe las de su zona y servicios (o a las que lo invitaron); el administrador, todas. */
    suspend fun solicitudes(): List<Solicitud> = Api.lista("/solicitudes", Solicitud.serializer())

    suspend fun responder(numero: Long, respuesta: RespuestaSolicitud): Solicitud =
        Api.json.decodeFromString(
            Solicitud.serializer(),
            Api.enviar("POST", "/solicitudes/$numero/respuesta", respuesta, RespuestaSolicitud.serializer()),
        )

    suspend fun pauta(id: Long): Pauta = Api.get("/pauta/$id", Pauta.serializer())
}
