package tn.iset.smartrecruit.offers.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.iset.smartrecruit.offers.dto.CandidatureExterneRequest;
import tn.iset.smartrecruit.offers.dto.CandidatureExterneResponse;
import tn.iset.smartrecruit.offers.model.CandidatureExterne;
import tn.iset.smartrecruit.offers.model.ExternalApplicationStatus;
import tn.iset.smartrecruit.offers.repository.CandidatureExterneRepository;
import tn.iset.smartrecruit.offers.security.AuthFacade;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CandidatureExterneService {
    private final CandidatureExterneRepository repository;
    private final AuthFacade auth;

    public CandidatureExterneResponse recordRedirect(CandidatureExterneRequest request) {
        CandidatureExterne candidature = CandidatureExterne.builder()
                .candidatId(auth.currentUserId())
                .offreId(request.offreId())
                .titre(request.titre())
                .entreprise(request.entreprise())
                .urlExterne(request.urlExterne())
                .source(request.source())
                .dateClic(Instant.now())
                .statut(ExternalApplicationStatus.REDIRECTED)
                .build();
        return CandidatureExterneResponse.from(repository.save(candidature));
    }

    public List<CandidatureExterneResponse> mesCandidatures() {
        return repository.findByCandidatIdOrderByDateClicDesc(auth.currentUserId()).stream()
                .map(CandidatureExterneResponse::from)
                .toList();
    }
}
