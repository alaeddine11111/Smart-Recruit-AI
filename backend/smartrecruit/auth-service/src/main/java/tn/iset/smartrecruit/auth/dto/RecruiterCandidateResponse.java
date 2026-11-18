package tn.iset.smartrecruit.auth.dto;

public record RecruiterCandidateResponse(
        String nom,
        String email,
        String telephone,
        String localisation
) {
}
