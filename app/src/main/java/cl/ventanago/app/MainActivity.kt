package cl.ventanago.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import cl.ventanago.app.datos.Autenticacion
import cl.ventanago.app.ui.PantallaLogin
import cl.ventanago.app.ui.TemaVentanaGo
import cl.ventanago.app.ui.navegacion.PanelAdministrador
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Barras del sistema transparentes con íconos oscuros (el encabezado es blanco, como en la web).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            TemaVentanaGo {
                val autenticacion = remember { Autenticacion(applicationContext) }
                val sesion = autenticacion.sesion
                if (sesion == null) {
                    PantallaLogin(autenticacion)
                } else {
                    // Al vencer el token la sesión se cierra sola.
                    LaunchedEffect(sesion) {
                        delay((sesion.expiraEn - System.currentTimeMillis()).coerceAtLeast(0))
                        autenticacion.cerrar()
                    }
                    PanelAdministrador(sesion, alCerrarSesion = autenticacion::cerrar)
                }
            }
        }
    }
}
