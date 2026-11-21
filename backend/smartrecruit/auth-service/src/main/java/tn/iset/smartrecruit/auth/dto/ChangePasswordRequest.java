package tn.iset.smartrecruit.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String ancienMotDePasse,
        @NotBlank @Size(min = 8, max = 72) String nouveauMotDePasse
) {}
