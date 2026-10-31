package tn.iset.smartrecruit.cv_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document("profils")
public record Profil(
        @Id String id,
        @Indexed(unique = true) String candidatId,
        String titre, String resume, String telephone, String localisation,
        List<Experience> experiences, List<Formation> formations,
        List<Competence> competences, List<Langue> langues, List<SectionPerso> sections,
        boolean valide, Instant misAJourLe) {

    public record Experience(String poste, String entreprise, String dateDebut, String dateFin, String description) {}
    public record Formation(String diplome, String etablissement, String dateDebut, String dateFin) {}
    public record Competence(String nom, String categorie) {}
    public record Langue(String nom, String niveau) {}
    public record SectionPerso(String id, String titre, List<String> elements) {}

    public static Profil vide(String candidatId) {
        return new Profil(null, candidatId, "", "", "", "", List.of(), List.of(), List.of(),
                List.of(), List.of(), false, Instant.now());
    }
}