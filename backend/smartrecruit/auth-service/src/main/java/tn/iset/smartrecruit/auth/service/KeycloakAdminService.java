package tn.iset.smartrecruit.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tn.iset.smartrecruit.auth.model.Utilisateur;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Keeps Keycloak in sync with our own users (Keycloak is only used by the mobile app).
 * Everything is a no-op when app.keycloak.enabled=false, and a Keycloak failure never blocks
 * the normal web flow: it is only logged.
 */
@Slf4j
@Service
public class KeycloakAdminService {

    private final boolean enabled;
    private final String serverUrl;
    private final String realm;
    private final String clientId;
    private final String clientSecret;
    private final String adminUsername;
    private final String adminPassword;
    private final RestClient http = RestClient.create();

    public KeycloakAdminService(@Value("${app.keycloak.enabled:false}") boolean enabled,
                                @Value("${app.keycloak.server-url:http://localhost:8180}") String serverUrl,
                                @Value("${app.keycloak.realm:smartrecruit}") String realm,
                                @Value("${app.keycloak.admin-client-id:smartrecruit-backend}") String clientId,
                                @Value("${app.keycloak.admin-client-secret:}") String clientSecret,
                                @Value("${app.keycloak.admin-username:}") String adminUsername,
                                @Value("${app.keycloak.admin-password:}") String adminPassword) {
        this.enabled = enabled;
        this.serverUrl = serverUrl;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    public boolean isEnabled() {
        return enabled;
    }

    // ------------------------------------------------------------------ public operations

    /** Creates the user in Keycloak. role may be null (recruiter request: disabled user, no role yet). */
    public void createUser(String email, String nom, String password, String role, boolean userEnabled) {
        if (!enabled) return;
        try {
            String token = adminToken();
            String[] parts = nom.trim().split("\\s+", 2);
            Map<String, Object> user = Map.of(
                    "username", email,
                    "email", email,
                    "firstName", parts[0],
                    "lastName", parts.length > 1 ? parts[1] : parts[0],
                    "enabled", userEnabled,
                    "emailVerified", true,
                    "credentials", List.of(Map.of("type", "password", "value", password, "temporary", false)));

            ResponseEntity<Void> response;
            try {
                response = http.post()
                        .uri(adminBase() + "/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(user)
                        .retrieve()
                        .toBodilessEntity();
            } catch (HttpClientErrorException.Conflict e) {
                log.info("Keycloak: user {} already exists", email);
                return;
            }

            URI location = response.getHeaders().getLocation();
            if (role != null && location != null) {
                String path = location.getPath();
                assignRole(path.substring(path.lastIndexOf('/') + 1), role, token);
            }
        } catch (RuntimeException e) {
            log.warn("Keycloak: could not create user {}: {}", email, e.getMessage());
        }
    }

    /** Recruiter request accepted: enable the Keycloak user and give it the role. */
    public void enableAndAssignRole(String email, String role) {
        if (!enabled) return;
        try {
            String token = adminToken();
            String id = findUserId(email, token);
            if (id == null) {
                log.warn("Keycloak: user {} not found, cannot enable it", email);
                return;
            }
            Map<String, Object> representation = http.get()
                    .uri(adminBase() + "/users/{id}", id)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            if (representation != null) {
                representation.put("enabled", true);
                http.put()
                        .uri(adminBase() + "/users/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(representation)
                        .retrieve()
                        .toBodilessEntity();
            }
            assignRole(id, role, token);
        } catch (RuntimeException e) {
            log.warn("Keycloak: could not enable user {}: {}", email, e.getMessage());
        }
    }

    /** Recruiter request refused: remove the pending Keycloak user (so a new request can be made later). */
    public void deleteUser(String email) {
        if (!enabled) return;
        try {
            String token = adminToken();
            String id = findUserId(email, token);
            if (id == null) return;
            http.delete()
                    .uri(adminBase() + "/users/{id}", id)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException e) {
            log.warn("Keycloak: could not delete user {}: {}", email, e.getMessage());
        }
    }

    /**
     * Called after a successful web login (we know the plain password then): accounts created before
     * the Keycloak integration are copied to Keycloak the first time their owner logs in.
     */
    public void syncOnLogin(Utilisateur user, String password) {
        if (!enabled) return;
        try {
            String token = adminToken();
            if (findUserId(user.getEmail(), token) != null) return;
        } catch (RuntimeException e) {
            log.warn("Keycloak: sync skipped for {}: {}", user.getEmail(), e.getMessage());
            return;
        }
        createUser(user.getEmail(), user.getNom(), password, user.getRole().name(), true);
    }

    // ------------------------------------------------------------------ helpers

    private String adminBase() {
        return serverUrl + "/admin/realms/" + realm;
    }

    /**
     * Token for the Keycloak admin API. If an admin password is configured we use the master realm admin
     * (admin-cli, simplest for development); otherwise the service account of the client "smartrecruit-backend".
     */
    private String adminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        String tokenRealm;
        if (adminPassword != null && !adminPassword.isBlank()) {
            tokenRealm = "master";
            form.add("grant_type", "password");
            form.add("client_id", "admin-cli");
            form.add("username", adminUsername);
            form.add("password", adminPassword);
        } else {
            tokenRealm = realm;
            form.add("grant_type", "client_credentials");
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);
        }
        Map<String, Object> body = http.post()
                .uri(serverUrl + "/realms/" + tokenRealm + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        return String.valueOf(body.get("access_token"));
    }

    private String findUserId(String email, String token) {
        List<Map<String, Object>> users = http.get()
                .uri(adminBase() + "/users?email={email}&exact=true", email)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        if (users == null || users.isEmpty()) return null;
        return String.valueOf(users.get(0).get("id"));
    }

    private void assignRole(String userId, String roleName, String token) {
        Map<String, Object> role = http.get()
                .uri(adminBase() + "/roles/{role}", roleName)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        http.post()
                .uri(adminBase() + "/users/{id}/role-mappings/realm", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role))
                .retrieve()
                .toBodilessEntity();
    }
}
