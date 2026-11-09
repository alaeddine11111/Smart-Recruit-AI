package tn.iset.smartrecruit.offers.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** One company per recruiter (recruteurId is unique). */
@Document(collection = "entreprises")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Entreprise {
    @Id private String id;
    private String nom;
    private String description;
    private String secteur;
    private String siteWeb;
    private String ville;
    private String adresse;
    private String telephone;
    private String emailContact;
    private String logoUrl;

    /** ID of the owner (User.id of the RECRUTEUR). */
    @Indexed(unique = true)
    private String recruteurId;

    private Instant dateCreation;
    private Instant dateMiseAJour;
}
