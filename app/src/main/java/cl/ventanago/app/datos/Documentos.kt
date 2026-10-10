package cl.ventanago.app.datos

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** PDF que genera el backend (/documentos/...): se descargan a la caché y se abren con el visor del teléfono. */
object Documentos {
    suspend fun descargar(context: Context, ruta: String, nombreArchivo: String): File {
        val bytes = Api.descargar(ruta)
        return withContext(Dispatchers.IO) {
            val carpeta = File(context.cacheDir, "documentos").apply { mkdirs() }
            File(carpeta, nombreArchivo).apply { writeBytes(bytes) }
        }
    }

    /** Abre el PDF; si no hay visor instalado, ofrece compartirlo (WhatsApp, correo, Drive…). */
    fun abrir(context: Context, archivo: File) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".archivos", archivo)
        val ver = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(ver)
        } catch (_: ActivityNotFoundException) {
            compartir(context, archivo)
        }
    }

    fun compartir(context: Context, archivo: File) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".archivos", archivo)
        val enviar = Intent(Intent.ACTION_SEND).setType("application/pdf")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(enviar, "Compartir PDF").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
