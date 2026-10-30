package tn.iset.smartrecruit.auth.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "utilisateurs")
public class Candidat extends Utilisateur {

    private String telephone;
    private String localisation;

    public Candidat() {
        super(Role.CANDIDAT);
    }
}
