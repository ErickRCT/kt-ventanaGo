package cl.ventanago.app.datos

import cl.ventanago.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import okhttp3.Request

/** Un mantenedor del backend: GET /ruta, POST /ruta/agregar, PUT /ruta/modificar y DELETE /ruta/eliminar/{id}. */
class Recurso<T>(private val ruta: String, private val serializador: KSerializer<T>) {
    suspend fun listar(): List<T> = Api.lista(ruta, serializador)
    suspend fun agregar(item: T) { Api.enviar("POST", "$ruta/agregar", item, serializador) }
    suspend fun modificar(item: T) { Api.enviar("PUT", "$ruta/modificar", item, serializador) }
    suspend fun eliminar(id: Long) = Api.eliminar("$ruta/eliminar/$id")
}

object Recursos {
    val series = Recurso("/serie", Serie.serializer())
    val tiposPerfil = Recurso("/tipo-perfil", TipoPerfil.serializer())
    val perfiles = Recurso("/perfil", Perfil.serializer())
    val colores = Recurso("/color", ColorVentana.serializer())
    val vidrios = Recurso("/vidrio", Vidrio.serializer())
    val quincallerias = Recurso("/quincalleria", Quincalleria.serializer())
    /** El backend solo permite listar y agregar tipos de pauta. */
    val tiposPauta = Recurso("/tipo-pauta", TipoPauta.serializer())
    val clientes = Recurso("/cliente", Cliente.serializer())

    suspend fun regiones(): List<Region> = Api.lista("/region", Region.serializer())
    suspend fun comunas(regionId: Long): List<Comuna> = Api.lista("/comuna/region/$regionId", Comuna.serializer())

    suspend fun login(email: String, password: String): Sesion =
        Api.json.decodeFromString(
            Sesion.serializer(),
            Api.enviar("POST", "/auth/login", Credenciales(email, password), Credenciales.serializer()),
        )
}

/** Imágenes de catálogo: se guardan en la BD como ruta relativa ("pautas/x.jpg") y las sirve el front en Vercel. */
object Imagenes {
    fun url(ruta: String?): String? = ruta?.takeIf { it.isNotBlank() }?.let { BuildConfig.IMAGEN_BASE_URL + it.trimStart('/') }

    private var cache: List<String>? = null

    /** Rutas disponibles para elegir (pautas/imagenes.json, la misma lista que usa el front web). */
    suspend fun disponibles(): List<String> = cache ?: withContext(Dispatchers.IO) {
        val peticion = Request.Builder().url(BuildConfig.IMAGEN_BASE_URL + "pautas/imagenes.json").build()
        val texto = try {
            Api.http.newCall(peticion).execute().use { if (it.isSuccessful) it.body?.string() else null }
        } catch (_: Exception) {
            null
        } ?: throw ErrorApi("No se pudo cargar la lista de imágenes.")
        Api.json.decodeFromString(ListSerializer(String.serializer()), texto).also { cache = it }
    }
}
