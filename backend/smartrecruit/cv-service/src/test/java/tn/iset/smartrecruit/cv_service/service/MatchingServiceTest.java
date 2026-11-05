package tn.iset.smartrecruit.cv_service.service;

import org.junit.jupiter.api.Test;
import tn.iset.smartrecruit.cv_service.dto.OffreMatchingRequest;
import tn.iset.smartrecruit.cv_service.dto.ResultatMatching;
import tn.iset.smartrecruit.cv_service.model.Profil;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchingServiceTest {
    private final MatchingService service = new MatchingService();

    @Test
    void matchesSkillsWithoutCaseOrAccentDifferences() {
        Profil profil = profil("Java", "Développement");
        OffreMatchingRequest offre = offre("Java", "developpement", "SQL");

        ResultatMatching resultat = service.evaluer(profil, offre);

        assertEquals(67, resultat.score());
        assertEquals(List.of("Java", "developpement"), resultat.competencesCorrespondantes());
        assertEquals(List.of("SQL"), resultat.competencesManquantes());
    }

    @Test
    void countsRepeatedRequirementsOnlyOnce() {
        ResultatMatching resultat = service.evaluer(profil("Java"), offre("Java", " java ", "SQL"));

        assertEquals(50, resultat.score());
        assertEquals(List.of("Java"), resultat.competencesCorrespondantes());
        assertEquals(List.of("SQL"), resultat.competencesManquantes());
    }

    @Test
    void returnsZeroWhenOfferHasNoRequiredSkills() {
        ResultatMatching resultat = service.evaluer(profil("Java"), offre());

        assertEquals(0, resultat.score());
        assertEquals(List.of(), resultat.competencesCorrespondantes());
        assertEquals(List.of(), resultat.competencesManquantes());
    }

    @Test
    void ranksOffersFromBestMatchToWorst() {
        OffreMatchingRequest partial = offre("Java", "SQL");
        OffreMatchingRequest full = offre("Java");

        List<ResultatMatching> resultats = service.evaluerToutes(profil("Java"), List.of(partial, full));

        assertEquals(List.of(full, partial), resultats.stream().map(ResultatMatching::offre).toList());
    }

    private Profil profil(String... competences) {
        List<Profil.Competence> liste = java.util.Arrays.stream(competences)
                .map(nom -> new Profil.Competence(nom, ""))
                .toList();
        return new Profil("profile-id", "candidate-id", "", "", "", "",
                List.of(), List.of(), liste, List.of(), List.of(), true, null);
    }

    private OffreMatchingRequest offre(String... competences) {
        return new OffreMatchingRequest("offer-id", "Développeur", "INTERNE", null, List.of(competences));
    }
}
