package tn.iset.smartrecruit.cv_service.service;

import org.springframework.stereotype.Service;
import tn.iset.smartrecruit.cv_service.dto.OffreMatchingRequest;
import tn.iset.smartrecruit.cv_service.dto.ResultatMatching;
import tn.iset.smartrecruit.cv_service.model.Profil;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class MatchingService {

    public ResultatMatching evaluer(Profil profil, OffreMatchingRequest offre) {
        Set<String> competencesCandidat = indexer(profil.competencesValidees());
        Set<String> competencesDejaTraitees = new HashSet<>();
        List<String> correspondantes = new ArrayList<>();
        List<String> manquantes = new ArrayList<>();

        for (String requise : offre.competencesRequises()) {
            String cle = normaliser(requise);
            if (cle.isEmpty() || !competencesDejaTraitees.add(cle)) {
                continue;
            }

            if (competencesCandidat.contains(cle)) {
                correspondantes.add(requise.trim());
            } else {
                manquantes.add(requise.trim());
            }
        }

        int total = correspondantes.size() + manquantes.size();
        int score = total == 0 ? 0 : (int) Math.round(100.0 * correspondantes.size() / total);
        String explication = total == 0
                ? "Cette offre ne précise aucune compétence requise."
                : correspondantes.size() + " compétence(s) sur " + total + " correspondent à votre profil.";

        return new ResultatMatching(offre, score, List.copyOf(correspondantes),
                List.copyOf(manquantes), explication);
    }

    public List<ResultatMatching> evaluerToutes(Profil profil, List<OffreMatchingRequest> offres) {
        return offres.stream()
                .map(offre -> evaluer(profil, offre))
                .sorted((a, b) -> Integer.compare(b.score(), a.score()))
                .toList();
    }

    private Set<String> indexer(List<Profil.Competence> competences) {
        Set<String> index = new HashSet<>();
        if (competences == null) {
            return index;
        }
        for (Profil.Competence competence : competences) {
            if (competence != null) {
                String cle = normaliser(competence.nom());
                if (!cle.isEmpty()) {
                    index.add(cle);
                }
            }
        }
        return index;
    }

    private String normaliser(String valeur) {
        if (valeur == null) {
            return "";
        }
        return Normalizer.normalize(valeur.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
    }
}
