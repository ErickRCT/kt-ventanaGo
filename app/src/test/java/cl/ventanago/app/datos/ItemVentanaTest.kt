package cl.ventanago.app.datos

import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class ItemVentanaTest {
    /** El backend recibe estos campos como enteros: si se omiten quedan en 0 y rechaza la ventana. */
    @Test
    fun envia_siempre_medidas_cantidad_y_hojas_aunque_tengan_el_valor_por_defecto() {
        val json = Api.json.encodeToString(ItemVentana.serializer(), ItemVentana(descripcion = "AL-25", anchoMm = 1200, altoMm = 1000))
        val campos = Api.json.parseToJsonElement(json).jsonObject
        assertEquals(1, campos.getValue("cantidad").jsonPrimitive.int)
        assertEquals(2, campos.getValue("hojas").jsonPrimitive.int)
        assertEquals(1200, campos.getValue("anchoMm").jsonPrimitive.int)
        assertEquals(1000, campos.getValue("altoMm").jsonPrimitive.int)
    }
}
