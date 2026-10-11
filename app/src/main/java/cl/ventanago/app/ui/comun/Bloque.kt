package cl.ventanago.app.ui.comun

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cl.ventanago.app.ui.Colores

/** Tarjeta blanca con título (y descripción opcional) que agrupa una sección de un formulario o detalle. */
@Composable
fun Bloque(
    titulo: String,
    descripcion: String? = null,
    espacio: Dp = 10.dp,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Colores.superficie),
        border = BorderStroke(1.dp, Colores.borde),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(espacio)) {
            Column {
                Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                descripcion?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Colores.gris) }
            }
            contenido()
        }
    }
}
