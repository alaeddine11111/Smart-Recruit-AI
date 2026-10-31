package tn.iset.smartrecruit.cv_service.model;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("cvs")
public record CvDocument(
        @Id String id,
        @Indexed(unique = true) String candidatId,
        String nomFichier,
        long taille,
        String cheminFichier,
        Instant dateDepot) {}