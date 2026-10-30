package tn.iset.smartrecruit.auth.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "entreprises")
public class Entreprise {

    @Id
    private String id;

    @Indexed(unique = true)
    private String nom;

    private String description;
    private String siteWeb;
}
