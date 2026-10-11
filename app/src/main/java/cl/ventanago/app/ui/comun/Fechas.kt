package cl.ventanago.app.ui.comun

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "2026-10-11" → "11 de octubre de 2026"; si no se puede leer, el texto tal cual. */
fun fechaLegible(fecha: String?): String = try {
    LocalDate.parse(fecha).format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-CL")))
} catch (_: Exception) {
    fecha.orEmpty()
}
