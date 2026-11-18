package tn.iset.smartrecruit.cv_service.dto;

import jakarta.validation.constraints.Size;

public record StartPreparationRequest(
        @Size(max = 100, message = "L’identifiant de l’offre est trop long") String offreId) {}
