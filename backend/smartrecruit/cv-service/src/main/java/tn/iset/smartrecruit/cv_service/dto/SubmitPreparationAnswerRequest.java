package tn.iset.smartrecruit.cv_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitPreparationAnswerRequest(
        @NotBlank @Size(max = 100, message = "Identifiant de question invalide") String questionId,
        @NotBlank @Size(max = 5000, message = "La réponse ne peut pas dépasser 5000 caractères") String reponse) {}
