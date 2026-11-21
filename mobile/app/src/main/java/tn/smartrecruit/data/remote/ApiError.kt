package tn.smartrecruit.data.remote

import com.google.gson.JsonParser
import retrofit2.HttpException
import java.io.IOException

/** Messages for the errors returned by Keycloak ({"error":"invalid_grant","error_description":"..."}). */
private fun keycloakMessage(description: String): String = when {
    description.contains("Invalid user credentials", ignoreCase = true) ->
        "Email ou mot de passe incorrect."
    description.contains("Account disabled", ignoreCase = true) ->
        "Votre compte n'est pas encore activé : demande recruteur en attente ou refusée."
    description.contains("not fully set up", ignoreCase = true) ->
        "Compte Keycloak incomplet (prénom et nom obligatoires)."
    else -> "Connexion refusée : $description"
}

/** Turns any exception into a message that can be shown to the user. */
fun Throwable.toUserMessage(): String = when (this) {
    is HttpException -> {
        val json = runCatching {
            JsonParser.parseString(response()?.errorBody()?.string()).asJsonObject
        }.getOrNull()
        val serverMessage = runCatching { json?.get("message")?.asString }.getOrNull()
        val keycloakDescription = runCatching { json?.get("error_description")?.asString }.getOrNull()
        // validation errors look like {"message":"Validation échouée","errors":{"nom":"must not be blank"}}
        val firstFieldError = runCatching {
            json?.getAsJsonObject("errors")?.entrySet()?.firstOrNull()
                ?.let { "${it.key} : ${it.value.asString}" }
        }.getOrNull()

        if (keycloakDescription != null) {
            keycloakMessage(keycloakDescription)
        } else when (code()) {
            401 -> serverMessage ?: "Session expirée, veuillez vous reconnecter."
            403 -> serverMessage ?: "Accès refusé."
            409 -> serverMessage ?: "Cet email est déjà utilisé."
            400 -> listOfNotNull(serverMessage, firstFieldError).joinToString(" — ").ifBlank { "Requête invalide." }
            else -> serverMessage ?: "Erreur serveur (${code()})."
        }
    }
    is IOException -> "Impossible de joindre le serveur. Vérifiez la connexion."
    else -> message ?: "Une erreur est survenue."
}
