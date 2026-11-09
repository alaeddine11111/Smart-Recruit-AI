package tn.iset.smartrecruit.cv_service.service;

import org.springframework.stereotype.Service;
import tn.iset.smartrecruit.cv_service.model.Profil;
import tn.iset.smartrecruit.cv_service.repository.ProfilRepository;

import java.time.Instant;
import java.util.List;

@Service
public class ProfilService {
    private final ProfilRepository profils;

    public ProfilService(ProfilRepository profils) { this.profils = profils; }

    public Profil get(String candidatId) {
        return profils.findByCandidatId(candidatId)
                .orElseGet(() -> profils.save(Profil.vide(candidatId)));
    }

    public Profil enregistrer(String candidatId, Profil in, boolean valide) {
        Profil p = get(candidatId);
        return profils.save(new Profil(p.id(), candidatId, in.titre(), in.resume(), in.telephone(),
                in.localisation(), nz(in.experiences()), nz(in.formations()), p.competences(),
                p.competencesDetectees(), in.competencesValidees() == null
                        ? p.competencesValidees() : nz(in.competencesValidees()),
                nz(in.langues()), nz(in.sections()), valide, Instant.now()));
    }

    public Profil valider(String candidatId) {
        Profil p = get(candidatId);
        return profils.save(new Profil(p.id(), candidatId, p.titre(), p.resume(), p.telephone(),
                p.localisation(), p.experiences(), p.formations(), p.competences(),
                p.competencesDetectees(), p.competencesValidees(), p.langues(), p.sections(),
                true, Instant.now()));
    }

    private static <T> List<T> nz(List<T> l) { return l == null ? List.of() : l; }
}