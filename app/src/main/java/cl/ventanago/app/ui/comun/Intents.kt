package cl.ventanago.app.ui.comun

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Abre otra app (teléfono, correo…); si no hay ninguna que lo haga, no pasa nada. */
fun Context.abrirSiSePuede(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Sin app de teléfono o correo: no se hace nada.
    }
}

fun Context.llamar(telefono: String) = abrirSiSePuede(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$telefono")))

fun Context.escribirCorreo(email: String) = abrirSiSePuede(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
