package cl.ventanago.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import cl.ventanago.app.datos.Autenticacion
import cl.ventanago.app.datos.CarritoLocal
import cl.ventanago.app.ui.PantallaLogin
import cl.ventanago.app.ui.TemaVentanaGo
import cl.ventanago.app.ui.navegacion.Panel
import cl.ventanago.app.ui.navegacion.Pantalla
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CarritoLocal.iniciar(applicationContext)
        // Barras del sistema transparentes con íconos oscuros (el encabezado es blanco, como en la web).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            TemaVentanaGo {
                val autenticacion = remember { Autenticacion(applicationContext) }
                val sesion = autenticacion.sesion
                // Sin sesión la app abre en Diseñar ventana; el login se muestra solo cuando se necesita.
                var pidiendoIngreso by rememberSaveable { mutableStateOf(false) }
                var motivoIngreso by rememberSaveable { mutableStateOf<String?>(null) }
                var volverA by rememberSaveable { mutableStateOf<Pantalla?>(null) }

                // Si la sesión venció, se pide entrar de nuevo.
                LaunchedEffect(autenticacion.aviso) {
                    if (autenticacion.aviso != null) pidiendoIngreso = true
                }

                if (sesion == null && pidiendoIngreso) {
                    PantallaLogin(autenticacion, motivo = motivoIngreso, alVolver = {
                        pidiendoIngreso = false
                        motivoIngreso = null
                    })
                } else {
                    if (sesion != null) {
                        // Al vencer el token la sesión se cierra sola.
                        LaunchedEffect(sesion) {
                            pidiendoIngreso = false
                            delay((sesion.expiraEn - System.currentTimeMillis()).coerceAtLeast(0))
                            autenticacion.cerrar()
                        }
                    }
                    // Cada cuenta (o el modo sin cuenta) parte con su propio estado de pantallas.
                    key(sesion?.cuenta?.cuentaId) {
                        Panel(
                            sesion = sesion,
                            alCerrarSesion = {
                                autenticacion.cerrar()
                                volverA = null
                            },
                            alPedirIngreso = { motivo, destino ->
                                motivoIngreso = motivo
                                volverA = destino
                                pidiendoIngreso = true
                            },
                            pantallaTrasIngreso = volverA,
                        )
                    }
                }
            }
        }
    }
}
