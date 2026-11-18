package tn.iset.smartrecruit.cv_service.model;

import java.util.List;

public record QuestionEntretien(
        String id,
        String type,
        String question,
        String reponse,
        Evaluation evaluation) {

    public record Evaluation(
            int score,
            String pertinence,
            String exactitude,
            List<String> pointsManquants,
            String retour) {}
}
