package tn.iset.smartrecruit.cv_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import tn.iset.smartrecruit.cv_service.dto.CvInfo;
import tn.iset.smartrecruit.cv_service.model.CvDocument;
import tn.iset.smartrecruit.cv_service.model.Profil;
import tn.iset.smartrecruit.cv_service.repository.CvRepository;
import tn.iset.smartrecruit.cv_service.repository.ProfilRepository;

import java.util.Optional;

@Service
public class RecruiterCandidateDataService {
    private final RestClient offerService;
    private final ProfilRepository profils;
    private final CvRepository cvs;

    public RecruiterCandidateDataService(
            RestClient.Builder restClientBuilder,
            ProfilRepository profils,
            CvRepository cvs,
            @Value("${app.offer-service-url:http://localhost:8084}") String offerServiceUrl) {
        this.offerService = restClientBuilder.baseUrl(offerServiceUrl).build();
        this.profils = profils;
        this.cvs = cvs;
    }

    public Optional<Profil> profileForApplication(String applicationId, String authorization) {
        String candidateId = candidateId(applicationId, authorization);
        return profils.findByCandidatId(candidateId);
    }

    public Optional<CvInfo> cvForApplication(String applicationId, String authorization) {
        String candidateId = candidateId(applicationId, authorization);
        return cvs.findByCandidatId(candidateId)
                .map(c -> new CvInfo(c.nomFichier(), c.taille(), c.dateDepot()));
    }

    private String candidateId(String applicationId, String authorization) {
        CandidateAccessResponse candidate;
        try {
            candidate = offerService.get()
                    .uri("/api/recruteur/candidatures/{id}/candidate", applicationId)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .body(CandidateAccessResponse.class);
        } catch (RestClientResponseException e) {
            throw new ResponseStatusException(
                    e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)
                            ? HttpStatus.BAD_GATEWAY
                            : e.getStatusCode(),
                    "Application access could not be verified");
        }
        if (candidate == null || candidate.candidatId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Offer service returned no candidate identity");
        }
        return candidate.candidatId();
    }

    private record CandidateAccessResponse(String candidatId) {
    }

}
