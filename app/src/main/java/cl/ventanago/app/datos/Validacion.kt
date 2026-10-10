package cl.ventanago.app.datos

import java.text.NumberFormat
import java.util.Locale

// Mismas reglas que src/components/utils/validacion.ts del front web.

fun rutValido(rut: String): Boolean {
    val limpio = rut.filter { it.isDigit() || it == 'k' || it == 'K' }
    if (limpio.length < 2) return false
    val dv = limpio.last().lowercaseChar()
    var numero = limpio.dropLast(1).toLongOrNull() ?: return false
    var suma = 0L
    var multiplicador = 2
    while (numero > 0) {
        suma += (numero % 10) * multiplicador
        numero /= 10
        multiplicador = if (multiplicador == 7) 2 else multiplicador + 1
    }
    val esperado = 11 - (suma % 11)
    val calculado = when (esperado) {
        11L -> '0'
        10L -> 'k'
        else -> esperado.toString().first()
    }
    return dv == calculado
}

/** 12345678k -> 12.345.678-K */
fun formatearRut(rut: String): String {
    val limpio = rut.filter { it.isDigit() || it == 'k' || it == 'K' }.uppercase()
    if (limpio.length < 2) return limpio
    val cuerpo = limpio.dropLast(1).reversed().chunked(3).joinToString(".").reversed()
    return "$cuerpo-${limpio.last()}"
}

private val regexEmail = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
private val regexTelefono = Regex("^(\\+?56)?(\\s?)(0?9)(\\s?)[98765432]\\d{7}$")

fun emailValido(email: String) = regexEmail.matches(email.trim())
fun telefonoValido(telefono: String) = regexTelefono.matches(telefono.trim())

private val formatoPesos = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CL"))

/** 15000 -> $15.000 */
fun pesos(valor: Long): String = "$" + formatoPesos.format(valor)
