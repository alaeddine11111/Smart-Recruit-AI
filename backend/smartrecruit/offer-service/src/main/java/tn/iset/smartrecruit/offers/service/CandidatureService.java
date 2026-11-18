package tn.iset.smartrecruit.offers.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import tn.iset.smartrecruit.offers.dto.CandidatureResponse;
import tn.iset.smartrecruit.offers.dto.CandidateAccessResponse;
import tn.iset.smartrecruit.offers.dto.RecruiterApplicationStats;
import tn.iset.smartrecruit.offers.dto.RecruiterCandidatureResponse;
import tn.iset.smartrecruit.offers.exception.ConflictException;
import tn.iset.smartrecruit.offers.exception.ResourceNotFoundException;
import tn.iset.smartrecruit.offers.model.ApplicationStatus;
import tn.iset.smartrecruit.offers.model.Candidature;
import tn.iset.smartrecruit.offers.model.OffreEmploi;
import tn.iset.smartrecruit.offers.model.OfferStatus;
import tn.iset.smartrecruit.offers.repository.CandidatureRepository;
import tn.iset.smartrecruit.offers.repository.OffreEmploiRepository;
import tn.iset.smartrecruit.offers.security.AuthFacade;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidatureService {
    private final CandidatureRepository candidatureRepository;
    private final OffreEmploiRepository offreRepository;
    private final AuthFacade auth;

    public CandidatureResponse apply(String offreId) {
        String candidatId = auth.currentUserId();
        OffreEmploi offre = offreRepository.findByIdAndStatut(offreId, OfferStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found or not published"));

        if (candidatureRepository.findByCandidatIdAndOffreId(candidatId, offreId).isPresent()) {
            throw new ConflictException("You have already applied to this offer");
        }

        Candidature candidature = Candidature.builder()
                .candidatId(candidatId)
                .offreId(offreId)
                .dateCandidature(Instant.now())
                .statut(ApplicationStatus.PENDING)
                .build();

        try {
            return CandidatureResponse.from(candidatureRepository.save(candidature), offre);
        } catch (DuplicateKeyException e) {
            throw new ConflictException("You have already applied to this offer");
        }
    }

    public List<CandidatureResponse> mesCandidatures() {
        String candidatId = auth.currentUserId();
        return candidatureRepository.findByCandidatIdOrderByDateCandidatureDesc(candidatId).stream()
                .map(candidature -> CandidatureResponse.from(
                        candidature,
                        offreRepository.findById(candidature.getOffreId()).orElse(null)
                ))
                .toList();
    }

    public CandidatureResponse getOwned(String id) {
        String candidatId = auth.currentUserId();
        Candidature candidature = candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        if (!candidatId.equals(candidature.getCandidatId())) {
            throw new AccessDeniedException("This application belongs to another candidate");
        }
        OffreEmploi offre = offreRepository.findById(candidature.getOffreId()).orElse(null);
        return CandidatureResponse.from(candidature, offre);
    }

    public List<RecruiterCandidatureResponse> recruiterApplications(
            String offreId, ApplicationStatus statut, String candidate) {
        Map<String, OffreEmploi> ownedOffers = ownedOffers().stream()
                .collect(Collectors.toMap(OffreEmploi::getId, Function.identity()));
        if (offreId != null && !ownedOffers.containsKey(offreId)) {
            throw new AccessDeniedException("This offer belongs to another recruiter");
        }
        if (ownedOffers.isEmpty()) return List.of();

        return candidatureRepository.findByOffreIdIn(new ArrayList<>(ownedOffers.keySet())).stream()
                .filter(c -> ownedOffers.containsKey(c.getOffreId()))
                .filter(c -> offreId == null || offreId.equals(c.getOffreId()))
                .filter(c -> statut == null || statut == c.getStatut())
                .filter(c -> candidate == null || candidate.isBlank()
                        || c.getCandidatId().toLowerCase(Locale.ROOT)
                        .contains(candidate.trim().toLowerCase(Locale.ROOT)))
                .map(c -> RecruiterCandidatureResponse.from(c, ownedOffers.get(c.getOffreId())))
                .toList();
    }

    public RecruiterCandidatureResponse recruiterApplication(String id) {
        Candidature candidature = findRecruiterOwned(id);
        OffreEmploi offre = offreRepository.findById(candidature.getOffreId())
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));
        return RecruiterCandidatureResponse.from(candidature, offre);
    }

    public RecruiterCandidatureResponse updateRecruiterStatus(String id, ApplicationStatus statut) {
        Candidature candidature = findRecruiterOwned(id);
        candidature.setStatut(statut);
        OffreEmploi offre = offreRepository.findById(candidature.getOffreId())
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));
        return RecruiterCandidatureResponse.from(candidatureRepository.save(candidature), offre);
    }

    public CandidateAccessResponse candidateForRecruiter(String id) {
        return new CandidateAccessResponse(findRecruiterOwned(id).getCandidatId());
    }

    public RecruiterApplicationStats recruiterStats() {
        List<OffreEmploi> offres = ownedOffers();
        if (offres.isEmpty()) return new RecruiterApplicationStats(0, 0, 0, 0, List.of());

        Map<String, List<Candidature>> byOffer = candidatureRepository
                .findByOffreIdIn(offres.stream().map(OffreEmploi::getId).toList()).stream()
                .collect(Collectors.groupingBy(Candidature::getOffreId));
        List<RecruiterApplicationStats.OfferApplicationStats> offerStats = offres.stream()
                .map(offre -> statsFor(offre, byOffer.getOrDefault(offre.getId(), List.of())))
                .toList();

        long total = offerStats.stream().mapToLong(RecruiterApplicationStats.OfferApplicationStats::total).sum();
        long pending = offerStats.stream().mapToLong(RecruiterApplicationStats.OfferApplicationStats::pending).sum();
        long accepted = offerStats.stream().mapToLong(RecruiterApplicationStats.OfferApplicationStats::accepted).sum();
        long rejected = offerStats.stream().mapToLong(RecruiterApplicationStats.OfferApplicationStats::rejected).sum();
        return new RecruiterApplicationStats(total, pending, accepted, rejected, offerStats);
    }

    private List<OffreEmploi> ownedOffers() {
        return auth.isAdmin() ? offreRepository.findAll() : offreRepository.findByRecruteurId(auth.currentUserId());
    }

    private Candidature findRecruiterOwned(String id) {
        Candidature candidature = candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        OffreEmploi offre = offreRepository.findById(candidature.getOffreId())
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));
        if (!auth.isAdmin() && !offre.getRecruteurId().equals(auth.currentUserId())) {
            throw new AccessDeniedException("This application belongs to another recruiter");
        }
        return candidature;
    }

    private RecruiterApplicationStats.OfferApplicationStats statsFor(
            OffreEmploi offre, List<Candidature> applications) {
        long pending = applications.stream().filter(c -> c.getStatut() == ApplicationStatus.PENDING).count();
        long accepted = applications.stream().filter(c -> c.getStatut() == ApplicationStatus.ACCEPTED).count();
        long rejected = applications.stream().filter(c -> c.getStatut() == ApplicationStatus.REJECTED).count();
        return new RecruiterApplicationStats.OfferApplicationStats(
                offre.getId(), offre.getTitre(), applications.size(), pending, accepted, rejected);
    }
}
