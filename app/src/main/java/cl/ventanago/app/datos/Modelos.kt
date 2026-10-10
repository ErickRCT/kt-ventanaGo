package cl.ventanago.app.datos

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// Modelos con los mismos nombres de campos que los DTO de ms-ventanaGo.

@Serializable
data class Serie(
    val serieId: Long? = null,
    val nombre: String = "",
    val descripcion: String? = null,
)

@Serializable
data class TipoPerfil(
    val tipoPerfilId: Long? = null,
    val nombre: String = "",
)

@Serializable
data class Perfil(
    val perfilId: Long? = null,
    val codigo: String = "",
    val descripcion: String? = null,
    /** El backend lo guarda como texto (kg por metro). */
    val peso: String? = null,
    val isBastidor: Boolean? = false,
    val tipoPerfil: TipoPerfil? = null,
    val serie: Serie? = null,
    val reforzado: Boolean = false,
    val rutaImagen: String? = null,
    /** "H" horizontal o "V" vertical. */
    val orientacion: String? = "H",
)

@Serializable
data class ColorVentana(
    val colorId: Long? = null,
    val nombre: String = "",
    val valor: Long = 0,
)

@Serializable
data class Vidrio(
    val vidrioId: Long? = null,
    val nombre: String = "",
    val valor: Long = 0,
)

@Serializable
data class Quincalleria(
    val quincalleriaId: Long? = null,
    val nombre: String = "",
    /** "Pz" (pieza) o "Mt" (metro). */
    val unidad: String = "Pz",
    val valor: Long = 0,
    val rutaImagen: String? = null,
    val serie: Serie? = null,
)

@Serializable
data class TipoPauta(
    val tipoPautaId: Long? = null,
    val nombre: String = "",
    val rutaImagen: String? = null,
    /** Se conserva tal como llega; las pantallas no lo usan. */
    val tipoProducto: JsonElement? = null,
)

@Serializable
data class Region(
    val regionId: Long? = null,
    val nombre: String = "",
    val codigo: String? = null,
)

@Serializable
data class Comuna(
    val comunaId: Long? = null,
    val nombre: String = "",
    val region: Region? = null,
)

@Serializable
data class Cliente(
    val clienteId: Long? = null,
    val rut: String = "",
    val nombre: String = "",
    val telefono: String? = null,
    val email: String? = null,
    val direccion: String? = null,
    val comuna: Comuna? = null,
)

// Sesión (respuesta de /auth/login)

@Serializable
data class CuentaSesion(
    val cuentaId: Long,
    val email: String,
    val nombre: String? = null,
    /** ADMIN, CLIENTE o PROVEEDOR. */
    val rol: String,
)

@Serializable
data class Sesion(
    val token: String,
    /** Vencimiento en milisegundos. */
    val expiraEn: Long,
    val cuenta: CuentaSesion,
)

@Serializable
data class Credenciales(val email: String, val password: String)
