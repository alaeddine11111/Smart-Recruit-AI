package tn.iset.smartrecruit.auth.model;

import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "utilisateurs")
public class Administrateur extends Utilisateur {

    public Administrateur() {
        super(Role.ADMIN);
    }
}
