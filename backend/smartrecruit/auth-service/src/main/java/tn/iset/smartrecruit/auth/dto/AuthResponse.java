package tn.iset.smartrecruit.auth.dto;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs,
        UserResponse user
) {}
