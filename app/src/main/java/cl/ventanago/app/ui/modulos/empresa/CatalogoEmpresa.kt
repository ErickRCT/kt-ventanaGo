package cl.ventanago.app.ui.modulos.empresa

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.ventanago.app.ui.Colores
import cl.ventanago.app.ui.navegacion.Pantalla
import cl.ventanago.app.ui.navegacion.catalogoEmpresa

/**
 * Catálogo de VentanaGo para la empresa (series, pautas, perfiles…), por ahora solo para consultarlo.
 * [alAbrir] abre una parte, que se ve como subpantalla con flecha para volver aquí.
 */
@Composable
fun PantallaCatalogoEmpresa(alAbrir: (Pantalla) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Series, pautas, perfiles y materiales con que se calculan las ventanas que diseñan los clientes. " +
                "Por ahora solo puedes consultarlos.",
            color = Colores.gris,
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = Colores.superficie),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            catalogoEmpresa.forEachIndexed { i, p ->
                if (i > 0) HorizontalDivider(Modifier.padding(start = 64.dp), color = Colores.borde)
                FilaCatalogo(p) { alAbrir(p) }
            }
        }
    }
}

/** Una parte del catálogo: icono, nombre y flecha; al tocarla se abre. */
@Composable
private fun FilaCatalogo(pantalla: Pantalla, alTocar: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = alTocar)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .background(Colores.azulClaro, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(pantalla.icono, contentDescription = null, tint = Colores.azul, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(14.dp))
        Text(pantalla.titulo, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Colores.gris)
    }
}
