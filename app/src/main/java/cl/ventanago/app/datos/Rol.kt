package cl.ventanago.app.datos

/** Quién usa la app: los roles del backend más [INVITADO], que diseña sin haber entrado. */
enum class Rol(val nombre: String) {
    ADMIN("Administrador"),
    CLIENTE("Cliente"),
    PROVEEDOR("Empresa"),
    INVITADO("Sin cuenta");

    /** Clientes y administrador guardan su carrito en la cuenta (backend); una empresa no tiene carrito. */
    val tieneCarrito get() = this == CLIENTE || this == ADMIN

    /** El cliente recibe las respuestas y la empresa las solicitudes nuevas (el administrador no recibe avisos). */
    val recibeAvisos get() = this == CLIENTE || this == PROVEEDOR

    companion object {
        /** Rol que viene del backend; null si la app no lo conoce. */
        fun delBackend(rol: String?): Rol? = entries.firstOrNull { it != INVITADO && it.name == rol }
    }
}

/** Rol de la sesión; sin sesión, [Rol.INVITADO]. */
val Sesion?.rol: Rol get() = this?.let { Rol.delBackend(it.cuenta.rol) } ?: Rol.INVITADO
