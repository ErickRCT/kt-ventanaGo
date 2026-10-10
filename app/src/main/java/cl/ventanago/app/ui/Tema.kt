package cl.ventanago.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Colores del front web (components/theme.ts, MenuLateral y Encabezado).
object Colores {
    val azul = Color(0xFF2B4C7E)
    val azulClaro = Color(0xFFDCE6F5)
    val menuFondo = Color(0xFF1A2332)
    val menuActivo = Color(0xFFB3E5FC)
    val menuTexto = Color(0xB3FFFFFF)
    val fondo = Color(0xFFF8F9FA)
    val texto = Color(0xFF2D3748)
    val textoSecundario = Color(0xFF4A5568)
    val borde = Color(0xFFE2E8F0)
    val gris = Color(0xFF718096)
    val error = Color(0xFFC62828)
    val exito = Color(0xFF2E7D32)
}

private val esquema = lightColorScheme(
    primary = Colores.azul,
    onPrimary = Color.White,
    primaryContainer = Colores.azulClaro,
    onPrimaryContainer = Colores.menuFondo,
    secondary = Colores.gris,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8ECF2),
    onSecondaryContainer = Colores.texto,
    background = Colores.fondo,
    onBackground = Colores.texto,
    surface = Color.White,
    onSurface = Colores.texto,
    surfaceVariant = Color(0xFFF1F4F8),
    onSurfaceVariant = Colores.textoSecundario,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFF1F4F8),
    outline = Color(0xFFCBD5E0),
    outlineVariant = Colores.borde,
    error = Colores.error,
)

@Composable
fun TemaVentanaGo(contenido: @Composable () -> Unit) {
    MaterialTheme(colorScheme = esquema, content = contenido)
}
