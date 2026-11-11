package tn.iset.smartrecruit.auth.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/** Request to become a recruiter. The account is only created when an admin accepts it. */
@Getter
@Setter
@Document(collection = "demandes_recruteur")
public class DemandeRecruteur {

    @Id
    private String id;

    // applicant
    private String nom;
    @Indexed
    private String email;
    private String motDePasseHash;
    private String poste;

    // company information
    private String entrepriseNom;
    private String entrepriseDescription;
    private String entrepriseSiteWeb;

    // workflow
    private StatutDemande statut = StatutDemande.EN_ATTENTE;
    private String motifRefus;
    private LocalDateTime dateDemande = LocalDateTime.now();
    private LocalDateTime dateTraitement;
}