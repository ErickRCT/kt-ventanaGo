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

/** "2026-10-11T14:22:05" → "Hoy, 14:22", "Ayer, 09:10" o "3 oct, 18:40" (como lo manda el backend, sin zona horaria). */
fun fechaHoraCorta(fecha: String?): String {
    if (fecha == null) return ""
    val hora = fecha.drop(11).take(5)
    val dia = try {
        LocalDate.parse(fecha.take(10))
    } catch (_: Exception) {
        return fecha
    }
    val hoy = LocalDate.now()
    val texto = when (dia) {
        hoy -> "Hoy"
        hoy.minusDays(1) -> "Ayer"
        else -> dia.format(DateTimeFormatter.ofPattern(if (dia.year == hoy.year) "d MMM" else "d MMM yyyy", Locale.forLanguageTag("es-CL"))).replace(".", "")
    }
    return if (hora.isEmpty()) texto else "$texto, $hora"
}
