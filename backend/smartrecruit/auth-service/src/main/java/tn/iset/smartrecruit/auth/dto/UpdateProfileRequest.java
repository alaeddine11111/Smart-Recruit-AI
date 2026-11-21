package tn.iset.smartrecruit.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Only the fields that match the user's role are used (candidate: telephone/localisation, recruiter: poste). */
public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String nom,
        @Size(max = 30) String telephone,
        @Size(max = 100) String localisation,
        @Size(max = 100) String poste
) {}
