package cl.ventanago.app.datos

import cl.ventanago.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Error con un mensaje listo para mostrar al usuario. */
class ErrorApi(mensaje: String, val codigo: Int? = null) : Exception(mensaje)

/**
 * Llamadas al backend (ms-ventanaGo en Render). Agrega el token de la sesión a cada petición y, si el
 * servidor lo rechaza (vencido o revocado), cierra la sesión igual que el front web.
 */
object Api {
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        isLenient = true
    }

    // Render (plan gratuito) apaga el servidor sin uso: la primera petición puede tardar cerca de un minuto.
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()

    private val tipoJson = "application/json; charset=utf-8".toMediaType()

    @Volatile
    var token: String? = null

    /** Se llama cuando el backend responde 401 con una sesión abierta. */
    var alVencerSesion: () -> Unit = {}

    suspend fun <T> get(ruta: String, lector: DeserializationStrategy<T>): T =
        json.decodeFromString(lector, ejecutar("GET", ruta, null))

    suspend fun <T> lista(ruta: String, elemento: KSerializer<T>): List<T> = get(ruta, ListSerializer(elemento))

    /** POST/PUT con cuerpo JSON. Devuelve el texto de la respuesta (los mantenedores no lo necesitan). */
    suspend fun <B> enviar(metodo: String, ruta: String, cuerpo: B, escritor: SerializationStrategy<B>): String =
        ejecutar(metodo, ruta, json.encodeToString(escritor, cuerpo))

    suspend fun eliminar(ruta: String) {
        ejecutar("DELETE", ruta, null)
    }

    /** Descarga un archivo binario (PDF de cotización u orden de trabajo). */
    suspend fun descargar(ruta: String): ByteArray = withContext(Dispatchers.IO) {
        val peticion = Request.Builder()
            .url(BuildConfig.API_BASE_URL + ruta)
            .apply { token?.let { header("Authorization", "Bearer $it") } }
            .build()
        val respuesta = try {
            http.newCall(peticion).execute()
        } catch (e: IOException) {
            throw ErrorApi("No hay conexión con el servidor. Revisa tu internet e inténtalo nuevamente.")
        }
        respuesta.use {
            if (it.isSuccessful) return@withContext it.body?.bytes() ?: ByteArray(0)
            if (it.code == 401 && token != null) {
                withContext(Dispatchers.Main) { alVencerSesion() }
                throw ErrorApi("Tu sesión venció. Vuelve a iniciar sesión.", 401)
            }
            throw ErrorApi(mensajeDelServidor(it.body?.string().orEmpty()) ?: "No se pudo generar el documento.", it.code)
        }
    }

    private suspend fun ejecutar(metodo: String, ruta: String, cuerpoJson: String?): String = withContext(Dispatchers.IO) {
        val peticion = Request.Builder()
            .url(BuildConfig.API_BASE_URL + ruta)
            .method(metodo, cuerpoJson?.toRequestBody(tipoJson))
            .apply { token?.let { header("Authorization", "Bearer $it") } }
            .build()
        val respuesta = try {
            http.newCall(peticion).execute()
        } catch (e: IOException) {
            throw ErrorApi("No hay conexión con el servidor. Revisa tu internet e inténtalo nuevamente.")
        }
        respuesta.use {
            val texto = it.body?.string().orEmpty()
            if (it.isSuccessful) return@withContext texto
            if (it.code == 401 && token != null && !ruta.startsWith("/auth/")) {
                withContext(Dispatchers.Main) { alVencerSesion() }
                throw ErrorApi("Tu sesión venció. Vuelve a iniciar sesión.", 401)
            }
            throw ErrorApi(mensajeDelServidor(texto) ?: mensajePorCodigo(it.code), it.code)
        }
    }

    /** El backend responde los errores como {"message": "..."}. */
    private fun mensajeDelServidor(texto: String): String? = try {
        json.parseToJsonElement(texto).jsonObject["message"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }

    private fun mensajePorCodigo(codigo: Int) = when (codigo) {
        400 -> "Los datos enviados no son válidos."
        401 -> "Correo o contraseña incorrectos."
        403 -> "No tienes permiso para realizar esta acción."
        404 -> "No se encontró el registro."
        409 -> "El registro está en uso y no se puede modificar."
        in 500..599 -> "El servidor tuvo un problema. Si el registro está en uso por otros datos, no se puede eliminar."
        else -> "No se pudo completar la operación. Inténtalo nuevamente."
    }
}

/** Mensaje para mostrar de cualquier error. */
fun Throwable.mensaje(): String = (this as? ErrorApi)?.message ?: "No se pudo completar la operación. Inténtalo nuevamente."
