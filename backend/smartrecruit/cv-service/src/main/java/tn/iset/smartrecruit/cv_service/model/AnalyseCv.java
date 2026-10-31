package tn.iset.smartrecruit.cv_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("analyses")
public record AnalyseCv(
        @Id String id,
        @Indexed String candidatId,
        String cvId,
        Statut statut,
        String texteOCR,
        boolean ocrUtilise,
        String messageErreur,
        Instant date) {

    public enum Statut { EN_COURS, TERMINEE, ECHEC }
}