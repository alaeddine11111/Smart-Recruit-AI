package tn.iset.smartrecruit.offers.dto;

import tn.iset.smartrecruit.offers.model.ContractType;
import tn.iset.smartrecruit.offers.model.OfferStatus;
import jakarta.validation.constraints.*;

import java.util.List;

public record OffreRequest(
        @NotBlank @Size(max = 150) String titre,
        @NotBlank @Size(max = 5000) String description,
        @NotBlank @Size(max = 120) String localisation,
        @NotEmpty List<@NotBlank String> competences,
        @NotNull ContractType typeContrat,
        @PositiveOrZero Double salaireMin,
        @PositiveOrZero Double salaireMax,
        @Size(max = 5) String devise,
        @NotNull OfferStatus statut) {}

