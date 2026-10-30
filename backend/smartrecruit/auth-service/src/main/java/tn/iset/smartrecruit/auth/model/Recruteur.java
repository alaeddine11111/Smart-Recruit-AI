package tn.iset.smartrecruit.auth.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "utilisateurs")
public class Recruteur extends Utilisateur {

    private String poste;
    /** Reference to the Entreprise this recruiter belongs to. */
    private String entrepriseId;

    public Recruteur() {
        super(Role.RECRUTEUR);
    }
}
