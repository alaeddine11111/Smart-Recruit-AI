package tn.iset.smartrecruit.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import tn.iset.smartrecruit.auth.dto.RecruiterCandidateResponse;
import tn.iset.smartrecruit.auth.model.Candidat;
import tn.iset.smartrecruit.auth.repository.UtilisateurRepository;

@Service
public class RecruiterCandidateService {
    private final RestClient offerService;
    private final UtilisateurRepository users;

    public RecruiterCandidateService(
            RestClient.Builder restClientBuilder,
            UtilisateurRepository users,
            @Value("${app.offer-service-url:http://localhost:8084}") String offerServiceUrl) {
        this.offerService = restClientBuilder.baseUrl(offerServiceUrl).build();
        this.users = users;
    }

    public RecruiterCandidateResponse forApplication(String applicationId, String authorization) {
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
        Candidat user = users.findByEmail(candidate.candidatId())
                .filter(Candidat.class::isInstance)
                .map(Candidat.class::cast)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidate not found"));
        return new RecruiterCandidateResponse(
                user.getNom(), user.getEmail(), user.getTelephone(), user.getLocalisation());
    }

    private record CandidateAccessResponse(String candidatId) {
    }
}
