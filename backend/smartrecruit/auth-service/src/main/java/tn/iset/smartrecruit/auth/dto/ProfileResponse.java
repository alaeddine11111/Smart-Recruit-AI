package tn.iset.smartrecruit.auth.dto;

import tn.iset.smartrecruit.auth.model.Role;

import java.time.LocalDateTime;

/** Full profile of the logged-in user. Fields that do not apply to the role are null. */
public record ProfileResponse(
        String id,
        String nom,
        String email,
        Role role,
        LocalDateTime dateCreation,
        // candidate
        String telephone,
        String localisation,
        // recruiter
        String poste,
        String entrepriseNom,
        String entrepriseDescription,
        String entrepriseSiteWeb
) {}
