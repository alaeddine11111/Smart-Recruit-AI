package tn.iset.smartrecruit.offers.service;

import tn.iset.smartrecruit.offers.dto.*;
import tn.iset.smartrecruit.offers.exception.BusinessException;
import tn.iset.smartrecruit.offers.exception.ResourceNotFoundException;
import tn.iset.smartrecruit.offers.model.*;
import tn.iset.smartrecruit.offers.repository.EntrepriseRepository;
import tn.iset.smartrecruit.offers.repository.OffreEmploiRepository;
import tn.iset.smartrecruit.offers.security.AuthFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OffreEmploiService {

    private final OffreEmploiRepository repo;
    private final EntrepriseRepository entrepriseRepo;
    private final AuthFacade auth;

    // ------------------------------------------------ candidate side (published only)
    public PageResponse<OffreEmploi> searchPublished(OffreFilter f, Pageable p) {
        var forced = new OffreFilter(f.titre(), f.localisation(), f.competences(), f.typeContrat(),
                OfferStatus.PUBLISHED, null);                     // never trust client statut / owner
        return PageResponse.of(repo.search(forced, p));
    }

    public OffreEmploi getPublished(String id) {
        return repo.findByIdAndStatut(id, OfferStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));
    }

    // ------------------------------------------------ recruiter side
    public PageResponse<OffreEmploi> searchMine(OffreFilter f, Pageable p) {
        var forced = new OffreFilter(f.titre(), f.localisation(), f.competences(), f.typeContrat(),
                f.statut(), auth.currentUserId());
        return PageResponse.of(repo.search(forced, p));
    }

    public OffreEmploi getMine(String id) { return owned(id); }

    public OffreEmploi create(OffreRequest r) {
        String uid = auth.currentUserId();
        Entreprise e = entrepriseRepo.findByRecruteurId(uid)
                .orElseThrow(() -> new BusinessException("Create your company profile before posting offers"));
        validateSalary(r);
        Instant now = Instant.now();
        OffreEmploi o = OffreEmploi.builder()
                .entrepriseId(e.getId()).entrepriseNom(e.getNom()).recruteurId(uid)
                .dateCreation(now).build();
        apply(o, r, now);
        return repo.save(o);
    }

    public OffreEmploi update(String id, OffreRequest r) {
        OffreEmploi o = owned(id);
        validateSalary(r);
        apply(o, r, Instant.now());
        return repo.save(o);
    }

    public OffreEmploi changeStatus(String id, OfferStatus s) {
        OffreEmploi o = owned(id);
        o.setStatut(s);
        if (s == OfferStatus.PUBLISHED && o.getDatePublication() == null) o.setDatePublication(Instant.now());
        o.setDateMiseAJour(Instant.now());
        return repo.save(o);
    }

    public void delete(String id) { repo.delete(owned(id)); }

    // ------------------------------------------------ helpers
    /** Loads an offer and enforces ownership (ADMIN bypasses). */
    private OffreEmploi owned(String id) {
        OffreEmploi o = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Offer not found"));
        if (!auth.isAdmin() && !o.getRecruteurId().equals(auth.currentUserId()))
            throw new AccessDeniedException("This offer belongs to another recruiter");
        return o;
    }

    private void apply(OffreEmploi o, OffreRequest r, Instant now) {
        o.setTitre(r.titre().trim());
        o.setDescription(r.description());
        o.setLocalisation(r.localisation().trim());
        o.setCompetences(normalise(r.competences()));
        o.setTypeContrat(r.typeContrat());
        o.setSalaireMin(r.salaireMin());
        o.setSalaireMax(r.salaireMax());
        o.setDevise(r.devise() == null || r.devise().isBlank() ? "TND" : r.devise());
        o.setStatut(r.statut());
        if (r.statut() == OfferStatus.PUBLISHED && o.getDatePublication() == null) o.setDatePublication(now);
        o.setDateMiseAJour(now);
    }

    private List<String> normalise(List<String> skills) {
        return skills.stream().map(s -> s.trim().toLowerCase()).filter(s -> !s.isEmpty()).distinct().toList();
    }

    private void validateSalary(OffreRequest r) {
        if (r.salaireMin() != null && r.salaireMax() != null && r.salaireMin() > r.salaireMax())
            throw new BusinessException("salaireMin must be <= salaireMax");
    }
}
