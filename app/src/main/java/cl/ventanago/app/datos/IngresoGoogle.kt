package cl.ventanago.app.datos

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import cl.ventanago.app.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.serialization.Serializable

@Serializable
private data class TokenGoogle(val idToken: String)

/**
 * "Continuar con Google" para clientes, con el Credential Manager de Android. Google entrega un ID token firmado
 * para el client ID web ([BuildConfig.GOOGLE_CLIENT_ID], el mismo app.google.client-id del backend) y el backend
 * lo valida en /auth/google: si el correo no tiene cuenta, la crea.
 */
object IngresoGoogle {
    /** false si la app se compiló sin el client ID (ventanago.googleClientId en gradle.properties). */
    val disponible get() = BuildConfig.GOOGLE_CLIENT_ID.isNotBlank()

    /** Abre la hoja de cuentas de Google. Devuelve null si el usuario la cerró sin elegir. [context] debe ser la Activity. */
    suspend fun ingresar(context: Context): Sesion? {
        val solicitud = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_CLIENT_ID).build())
            .build()
        val credencial = try {
            CredentialManager.create(context).getCredential(context, solicitud).credential
        } catch (_: GetCredentialCancellationException) {
            return null
        } catch (_: NoCredentialException) {
            throw ErrorApi("No hay cuentas de Google en este teléfono. Agrega una en Ajustes o entra con tu correo.")
        } catch (e: GetCredentialException) {
            throw ErrorApi("No se pudo abrir el inicio de sesión con Google. Inténtalo de nuevo o entra con tu correo.")
        }
        if (credencial !is CustomCredential || credencial.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw ErrorApi("Google no entregó una cuenta válida. Entra con tu correo.")
        }
        val idToken = GoogleIdTokenCredential.createFrom(credencial.data).idToken
        return Api.json.decodeFromString(Sesion.serializer(), Api.enviar("POST", "/auth/google", TokenGoogle(idToken), TokenGoogle.serializer()))
    }
}
