package tn.iset.smartrecruit.offers.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "candidatures")
@CompoundIndex(name = "candidate_offer_unique", def = "{'candidatId': 1, 'offreId': 1}", unique = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidature {
    @Id
    private String id;

    @Indexed
    private String candidatId;

    @Indexed
    private String offreId;

    private Instant dateCandidature;
    private ApplicationStatus statut;
}
