package tn.iset.smartrecruit.auth.dto;

import tn.iset.smartrecruit.auth.model.Role;
import tn.iset.smartrecruit.auth.model.Utilisateur;

import java.time.LocalDateTime;

public record UserResponse(
        String id,
        String nom,
        String email,
        Role role,
        LocalDateTime dateCreation
) {
    public static UserResponse from(Utilisateur u) {
        return new UserResponse(u.getId(), u.getNom(), u.getEmail(), u.getRole(), u.getDateCreation());
    }
}
