package tn.iset.smartrecruit.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRecruteurRequest(
        @NotBlank String nom,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String motDePasse,
        String poste,
        @NotBlank String entrepriseNom,
        String entrepriseDescription,
        String entrepriseSiteWeb
) {}
