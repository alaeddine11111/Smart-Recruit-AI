package tn.iset.smartrecruit.offers.service;

import tn.iset.smartrecruit.offers.dto.EntrepriseRequest;
import tn.iset.smartrecruit.offers.exception.ResourceNotFoundException;
import tn.iset.smartrecruit.offers.model.Entreprise;
import tn.iset.smartrecruit.offers.repository.EntrepriseRepository;
import tn.iset.smartrecruit.offers.repository.OffreEmploiRepository;
import tn.iset.smartrecruit.offers.security.AuthFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EntrepriseService {

    private final EntrepriseRepository repo;
    private final OffreEmploiRepository offreRepo;
    private final AuthFacade auth;

    public Entreprise getMine() {
        return repo.findByRecruteurId(auth.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("No company profile yet"));
    }

    public Entreprise getById(String id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Company not found"));
    }

    /** Create or update the caller's company (one company per recruiter). */
    public Entreprise upsertMine(EntrepriseRequest r) {
        String uid = auth.currentUserId();
        Instant now = Instant.now();
        Entreprise e = repo.findByRecruteurId(uid).orElseGet(() ->
                Entreprise.builder().recruteurId(uid).dateCreation(now).build());

        boolean renamed = e.getId() != null && !r.nom().equals(e.getNom());
        e.setNom(r.nom());
        e.setDescription(r.description());
        e.setSecteur(r.secteur());
        e.setSiteWeb(r.siteWeb());
        e.setVille(r.ville());
        e.setAdresse(r.adresse());
        e.setTelephone(r.telephone());
        e.setEmailContact(r.emailContact());
        e.setLogoUrl(r.logoUrl());
        e.setDateMiseAJour(now);
        Entreprise saved = repo.save(e);

        if (renamed) { // keep denormalised name in offers in sync
            var offers = offreRepo.findByEntrepriseId(saved.getId());
            offers.forEach(o -> o.setEntrepriseNom(saved.getNom()));
            offreRepo.saveAll(offers);
        }
        return saved;
    }
}
