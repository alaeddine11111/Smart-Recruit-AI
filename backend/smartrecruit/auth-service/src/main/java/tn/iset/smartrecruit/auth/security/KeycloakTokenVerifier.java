package tn.iset.smartrecruit.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Verifies a Keycloak access token: signature (public keys fetched from Keycloak), expiry and realm.
 * The issuer is only checked by its "/realms/<realm>" suffix, because the host in the token depends on
 * how the client reached Keycloak (localhost, 10.0.2.2, LAN IP of the PC...).
 */
@Component
public class KeycloakTokenVerifier {

    private final NimbusJwtDecoder decoder;

    public KeycloakTokenVerifier(@Value("${app.keycloak.server-url:http://localhost:8180}") String serverUrl,
                                 @Value("${app.keycloak.realm:smartrecruit}") String realm) {
        NimbusJwtDecoder d = NimbusJwtDecoder
                .withJwkSetUri(serverUrl + "/realms/" + realm + "/protocol/openid-connect/certs")
                .build();

        OAuth2TokenValidator<Jwt> realmValidator = jwt -> {
            String issuer = jwt.getIssuer() == null ? "" : jwt.getIssuer().toString();
            return issuer.endsWith("/realms/" + realm)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_issuer", "Wrong realm", null));
        };
        d.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), realmValidator));
        this.decoder = d;
    }

    /** @throws org.springframework.security.oauth2.jwt.JwtException if the token is invalid or expired */
    public Jwt verify(String accessToken) {
        return decoder.decode(accessToken);
    }
}
