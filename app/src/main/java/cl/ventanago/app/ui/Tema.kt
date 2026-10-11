package cl.ventanago.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.ventanago.app.R

/** Colores de la app con nombre según su uso; cada tema (claro u oscuro) tiene su versión. */
@Immutable
class Paleta(
    /** Color principal: botones, enlaces, íconos activos. */
    val azul: Color,
    /** Texto e íconos sobre [azul]. */
    val sobreAzul: Color,
    /** Fondo suave del color principal (avisos informativos, íconos destacados). */
    val azulClaro: Color,
    val fondo: Color,
    /** Tarjetas, barras y campos. */
    val superficie: Color,
    /** Fondo tenue dentro de una tarjeta (p. ej. detrás del dibujo de una ventana). */
    val superficieTenue: Color,
    val texto: Color,
    /** Títulos y montos destacados. */
    val textoFuerte: Color,
    val textoSecundario: Color,
    /** Texto de apoyo (descripciones, ayudas). Cumple el contraste mínimo para texto chico. */
    val gris: Color,
    val borde: Color,
    /** Barra superior de los formularios y detalles a pantalla completa. */
    val barra: Color,
    val sobreBarra: Color,
    val error: Color,
    val errorFondo: Color,
    val errorTexto: Color,
    val exito: Color,
    val exitoFondo: Color,
    /** Advertencias (pendiente, reforzado, cambios sin guardar). */
    val aviso: Color,
    val avisoFondo: Color,
    val avisoTexto: Color,
    val avisoBorde: Color,
    /** Etiquetas de estados neutros (cancelada, no tomada). */
    val neutroFondo: Color,
)

private val paletaClara = Paleta(
    azul = Color(0xFF2B4C7E),
    sobreAzul = Color.White,
    azulClaro = Color(0xFFDCE6F5),
    fondo = Color(0xFFF8F9FA),
    superficie = Color.White,
    superficieTenue = Color(0xFFEFF2F6),
    texto = Color(0xFF2D3748),
    textoFuerte = Color(0xFF1A2332),
    textoSecundario = Color(0xFF4A5568),
    gris = Color(0xFF5F6B7A),
    borde = Color(0xFFE2E8F0),
    barra = Color(0xFF2B4C7E),
    sobreBarra = Color.White,
    error = Color(0xFFC62828),
    errorFondo = Color(0xFFFDECEA),
    errorTexto = Color(0xFF5F2120),
    exito = Color(0xFF2E7D32),
    exitoFondo = Color(0xFFE8F5E9),
    aviso = Color(0xFF8A5300),
    avisoFondo = Color(0xFFFFF4E5),
    avisoTexto = Color(0xFF5C3A00),
    avisoBorde = Color(0xFFF2C46D),
    neutroFondo = Color(0xFFEDEFF2),
)

private val paletaOscura = Paleta(
    azul = Color(0xFF9DB9EA),
    sobreAzul = Color(0xFF0F2240),
    azulClaro = Color(0xFF233650),
    fondo = Color(0xFF0F141B),
    superficie = Color(0xFF182029),
    superficieTenue = Color(0xFF212A35),
    texto = Color(0xFFE3E8EF),
    textoFuerte = Color(0xFFF4F7FB),
    textoSecundario = Color(0xFFBAC3CE),
    gris = Color(0xFF9AA5B2),
    borde = Color(0xFF2C3644),
    barra = Color(0xFF1C2633),
    sobreBarra = Color(0xFFE3E8EF),
    error = Color(0xFFFF8A80),
    errorFondo = Color(0xFF3B1D1D),
    errorTexto = Color(0xFFFFD2CC),
    exito = Color(0xFF86CF98),
    exitoFondo = Color(0xFF1B3324),
    aviso = Color(0xFFF2C46D),
    avisoFondo = Color(0xFF3A2E14),
    avisoTexto = Color(0xFFF5DFAE),
    avisoBorde = Color(0xFF8A6A2A),
    neutroFondo = Color(0xFF26303B),
)

private val LocalPaleta = staticCompositionLocalOf { paletaClara }

/**
 * Acceso a los colores del tema actual (claro u oscuro). Los del menú lateral y la marca son iguales en ambos:
 * el menú siempre es oscuro.
 */
object Colores {
    /** Azul de la marca (logo, ícono de la app), igual en ambos temas. */
    val marca = Color(0xFF2B4C7E)
    val menuFondo = Color(0xFF1A2332)
    val menuActivo = Color(0xFFB3E5FC)
    val menuTexto = Color(0xB3FFFFFF)

    val azul: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.azul
    val sobreAzul: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.sobreAzul
    val azulClaro: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.azulClaro
    val fondo: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.fondo
    val superficie: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.superficie
    val superficieTenue: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.superficieTenue
    val texto: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.texto
    val textoFuerte: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.textoFuerte
    val textoSecundario: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.textoSecundario
    val gris: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.gris
    val borde: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.borde
    val barra: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.barra
    val sobreBarra: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.sobreBarra
    val error: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.error
    val errorFondo: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.errorFondo
    val errorTexto: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.errorTexto
    val exito: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.exito
    val exitoFondo: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.exitoFondo
    val aviso: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.aviso
    val avisoFondo: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.avisoFondo
    val avisoTexto: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.avisoTexto
    val avisoBorde: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.avisoBorde
    val neutroFondo: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.neutroFondo
}

private fun esquemaClaro(p: Paleta) = lightColorScheme(
    primary = p.azul,
    onPrimary = p.sobreAzul,
    primaryContainer = p.azulClaro,
    onPrimaryContainer = p.textoFuerte,
    secondary = p.gris,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8ECF2),
    onSecondaryContainer = p.texto,
    background = p.fondo,
    onBackground = p.texto,
    surface = p.superficie,
    onSurface = p.texto,
    surfaceVariant = Color(0xFFF1F4F8),
    onSurfaceVariant = p.textoSecundario,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFF1F4F8),
    outline = Color(0xFFCBD5E0),
    outlineVariant = p.borde,
    error = p.error,
    errorContainer = p.errorFondo,
    onErrorContainer = p.errorTexto,
)

private fun esquemaOscuro(p: Paleta) = darkColorScheme(
    primary = p.azul,
    onPrimary = p.sobreAzul,
    primaryContainer = p.azulClaro,
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = p.gris,
    onSecondary = Color(0xFF111821),
    secondaryContainer = Color(0xFF2A3441),
    onSecondaryContainer = p.texto,
    background = p.fondo,
    onBackground = p.texto,
    surface = p.superficie,
    onSurface = p.texto,
    surfaceVariant = Color(0xFF222B36),
    onSurfaceVariant = p.textoSecundario,
    surfaceContainerLowest = Color(0xFF0B1016),
    surfaceContainerLow = Color(0xFF151C24),
    surfaceContainer = p.superficie,
    surfaceContainerHigh = Color(0xFF1F2833),
    surfaceContainerHighest = Color(0xFF29333F),
    outline = Color(0xFF4A5666),
    outlineVariant = p.borde,
    error = p.error,
    onError = Color(0xFF4A0F0F),
    errorContainer = p.errorFondo,
    onErrorContainer = p.errorTexto,
)

/** Inter (variable): un solo archivo con todos los grosores. */
@OptIn(ExperimentalTextApi::class)
private fun inter(peso: Int) = Font(R.font.inter, FontWeight(peso), variationSettings = FontVariation.Settings(FontVariation.weight(peso)))

val Inter = FontFamily(inter(400), inter(500), inter(600), inter(700))

private val tipografia = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = Inter),
        displayMedium = displayMedium.copy(fontFamily = Inter),
        displaySmall = displaySmall.copy(fontFamily = Inter),
        headlineLarge = headlineLarge.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(fontFamily = Inter),
        bodyMedium = bodyMedium.copy(fontFamily = Inter),
        bodySmall = bodySmall.copy(fontFamily = Inter),
        labelLarge = labelLarge.copy(fontFamily = Inter),
        labelMedium = labelMedium.copy(fontFamily = Inter),
        labelSmall = labelSmall.copy(fontFamily = Inter),
    )
}

private val formas = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/** Tema de la app: sigue el modo claro u oscuro del teléfono. */
@Composable
fun TemaVentanaGo(oscuro: Boolean = isSystemInDarkTheme(), contenido: @Composable () -> Unit) {
    val paleta = if (oscuro) paletaOscura else paletaClara
    CompositionLocalProvider(LocalPaleta provides paleta) {
        MaterialTheme(
            colorScheme = if (oscuro) esquemaOscuro(paleta) else esquemaClaro(paleta),
            typography = tipografia,
            shapes = formas,
            content = contenido,
        )
    }
}
