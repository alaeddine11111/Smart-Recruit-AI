package tn.smartrecruit.data.remote

/** Keycloak settings for the mobile login (the server address itself is in ApiConfig.KEYCLOAK_URL). */
object KeycloakConfig {
    /** false = old login through auth-service only (useful if Keycloak is not running). */
    const val USE_KEYCLOAK = false
    const val CLIENT_ID = "smartrecruit-mobile"
}
