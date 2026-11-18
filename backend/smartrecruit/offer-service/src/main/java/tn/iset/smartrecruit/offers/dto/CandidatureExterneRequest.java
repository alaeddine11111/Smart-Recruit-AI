package tn.iset.smartrecruit.offers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CandidatureExterneRequest(
        @NotBlank String offreId,
        @NotBlank String titre,
        String entreprise,
        @NotBlank @Pattern(regexp = "^https?://.+") String urlExterne,
        @NotBlank String source
) {
}
