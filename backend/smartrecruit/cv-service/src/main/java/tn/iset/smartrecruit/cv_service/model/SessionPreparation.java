package tn.iset.smartrecruit.cv_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document("sessions_preparation")
@CompoundIndex(name = "candidate_created", def = "{'candidatId': 1, 'dateCreation': -1}")
public record SessionPreparation(
        @Id String id,
        String candidatId,
        String offreId,
        String offreTitre,
        Instant dateCreation,
        Instant dateFin,
        List<QuestionEntretien> questions,
        Integer scoreGlobal,
        EvaluationFinale evaluationFinale,
        StatutPreparation statut) {

    public enum StatutPreparation {
        EN_COURS, TERMINEE
    }

    public record EvaluationFinale(
            List<String> forces,
            List<String> faiblesses,
            List<String> axesAmelioration,
            String conseils) {}
}
