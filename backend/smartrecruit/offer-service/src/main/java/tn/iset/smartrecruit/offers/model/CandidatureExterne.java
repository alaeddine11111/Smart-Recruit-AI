package tn.iset.smartrecruit.offers.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "candidatures_externes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidatureExterne {
    @Id
    private String id;

    @Indexed
    private String candidatId;

    private String offreId;
    private String titre;
    private String entreprise;
    private String urlExterne;
    private String source;
    private Instant dateClic;
    private ExternalApplicationStatus statut;
}
