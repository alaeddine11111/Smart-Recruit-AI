package tn.iset.smartrecruit.auth.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/** Abstract parent from the class diagram. Candidat, Recruteur and Administrateur share one collection. */
@Getter
@Setter
@Document(collection = "utilisateurs")
public abstract class Utilisateur {

    @Id
    private String id;

    private String nom;

    @Indexed(unique = true)
    private String email;

    private String motDePasseHash;

    private LocalDateTime dateCreation = LocalDateTime.now();

    @Setter(AccessLevel.NONE)
    private Role role;

    protected Utilisateur(Role role) {
        this.role = role;
    }
}
