package cl.ventanago.app.ui.comun

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cl.ventanago.app.ui.Colores

/** Diálogo a pantalla completa con barra superior y botón de cerrar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDialogo(titulo: String, alCerrar: () -> Unit, contenido: @Composable () -> Unit) {
    Dialog(onDismissRequest = alCerrar, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler { alCerrar() }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(titulo, maxLines = 1) },
                    navigationIcon = { IconButton(onClick = alCerrar) { Icon(Icons.Filled.Close, contentDescription = "Cerrar") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Colores.barra, titleContentColor = Colores.sobreBarra, navigationIconContentColor = Colores.sobreBarra, actionIconContentColor = Colores.sobreBarra),
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            Box(
                Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding()
                    .fillMaxSize(),
            ) { contenido() }
        }
    }
}
