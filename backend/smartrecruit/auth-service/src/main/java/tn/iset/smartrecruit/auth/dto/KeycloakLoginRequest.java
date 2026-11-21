package tn.iset.smartrecruit.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** The mobile app sends the access token it got from Keycloak. */
public record KeycloakLoginRequest(@NotBlank String accessToken) {}
