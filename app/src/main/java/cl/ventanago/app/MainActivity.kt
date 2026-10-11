package cl.ventanago.app

import android.content.Intent
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import cl.ventanago.app.datos.Autenticacion
import cl.ventanago.app.datos.CarritoLocal
import cl.ventanago.app.notificaciones.Push
import cl.ventanago.app.ui.PantallaLogin
import cl.ventanago.app.ui.TemaVentanaGo
import cl.ventanago.app.ui.navegacion.Panel
import cl.ventanago.app.ui.navegacion.Pantalla
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    /** Solicitud de la notificación que se tocó (al abrir la app o con la app ya abierta). */
    private var solicitudNotificada by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        CarritoLocal.iniciar(applicationContext)
        Push.crearCanal(applicationContext)
        // Al recrearse (p. ej. al girar) el intent es el mismo: la notificación ya se atendió.
        if (savedInstanceState == null) leerNotificacion(intent)
        // Barras del sistema transparentes; los íconos son oscuros o claros según el modo del teléfono.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
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
                // Una notificación sin sesión abierta (venció): se pide entrar y después se abre su solicitud.
                LaunchedEffect(solicitudNotificada, sesion) {
                    if (solicitudNotificada != null && sesion == null) {
                        motivoIngreso = "Ingresa para ver el aviso de tu solicitud."
                        pidiendoIngreso = true
                    }
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
                            autenticacion.cerrar(vencida = true)
                        }
                    }
                    // Cada cuenta (o el modo sin cuenta) parte con su propia navegación y estado de pantallas.
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
                            solicitudNotificada = solicitudNotificada,
                            alAtenderNotificacion = { solicitudNotificada = null },
                        )
                    }
                }
            }
        }
    }

    /** Se tocó una notificación con la app abierta (singleTop: no se crea otra pantalla). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        leerNotificacion(intent)
    }

    private fun leerNotificacion(intent: Intent?) {
        intent?.takeIf { it.hasExtra(Push.EXTRA_SOLICITUD) }?.let {
            solicitudNotificada = it.getLongExtra(Push.EXTRA_SOLICITUD, 0).takeIf { n -> n > 0 }
            it.removeExtra(Push.EXTRA_SOLICITUD)
        }
    }
}
