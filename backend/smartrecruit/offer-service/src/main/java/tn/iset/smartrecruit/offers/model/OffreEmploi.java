package tn.iset.smartrecruit.offers.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "offres_emploi")
@CompoundIndex(name = "statut_date", def = "{'statut': 1, 'dateCreation': -1}")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OffreEmploi {
    @Id private String id;

    private String titre;
    private String description;
    private String localisation;
    /** Stored lowercase/trimmed so filtering is simple and index friendly. */
    @Indexed private List<String> competences;
    @Indexed private ContractType typeContrat;

    private Double salaireMin;
    private Double salaireMax;
    private String devise;            // "TND" by default

    @Indexed private OfferStatus statut;

    @Indexed private String entrepriseId;
    private String entrepriseNom;     // denormalised for fast list display
    @Indexed private String recruteurId;

    private Instant dateCreation;
    private Instant dateMiseAJour;
    private Instant datePublication;
}
