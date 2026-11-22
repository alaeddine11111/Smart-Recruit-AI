package tn.smartrecruit.data.remote

import com.google.gson.JsonParser
import retrofit2.HttpException
import java.io.IOException

/** Thrown when the recruiter tries to post an offer before creating the company. */
class NoCompanyException :
    Exception("Créez d'abord le profil de votre entreprise avant de publier une offre.")

/**
 * Converts offer-service errors to French messages.
 * The server answers with a Spring ProblemDetail: {"detail": "...", "errors": {"field": "message"}}.
 */
fun Throwable.toOfferError(): Exception {
    if (this is NoCompanyException) return this
    if (this is IOException) return Exception("Impossible de joindre le serveur.", this)
    if (this !is HttpException) return Exception(message ?: "Erreur inattendue.", this)

    val body = runCatching { response()?.errorBody()?.string() }.getOrNull()
    var detail: String? = null
    var fields: String? = null
    if (!body.isNullOrBlank()) {
        runCatching {
            val json = JsonParser.parseString(body).asJsonObject
            val d = json.get("detail")
            if (d != null && !d.isJsonNull) detail = d.asString
            val errors = json.getAsJsonObject("errors")
            if (errors != null) {
                fields = errors.entrySet().joinToString("\n") { "${it.key} : ${it.value.asString}" }
            }
        }
    }
    return when (code()) {
        400 -> when {
            detail?.contains("company profile", ignoreCase = true) == true -> NoCompanyException()
            fields != null -> Exception(fields, this)
            else -> Exception(detail ?: "Données invalides.", this)
        }
        401 -> Exception("Session expirée. Reconnectez-vous.", this)
        403 -> Exception("Action non autorisée pour votre compte.", this)
        404 -> Exception("Élément introuvable.", this)
        else -> Exception("Erreur du serveur (${code()}).", this)
    }
}
