package cl.ventanago.app.ui.comun

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Base de los ViewModel de las pantallas: el estado vive aquí (sobrevive a girar el teléfono y a ir a otra
 * pantalla y volver) y los mensajes cortos para el snackbar se envían con [avisar].
 */
abstract class ModeloBase : ViewModel() {
    private val _mensajes = Channel<String>(Channel.BUFFERED)
    val mensajes: Flow<String> = _mensajes.receiveAsFlow()

    protected fun avisar(texto: String) {
        _mensajes.trySend(texto)
    }
}

/** Muestra en el snackbar los mensajes del [modelo]. */
@Composable
fun MostrarAvisos(modelo: ModeloBase) {
    val avisar = LocalAvisos.current
    LaunchedEffect(modelo) { modelo.mensajes.collect { avisar(it) } }
}
