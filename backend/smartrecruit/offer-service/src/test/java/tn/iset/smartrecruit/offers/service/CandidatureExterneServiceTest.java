package tn.iset.smartrecruit.offers.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.iset.smartrecruit.offers.dto.CandidatureExterneRequest;
import tn.iset.smartrecruit.offers.model.CandidatureExterne;
import tn.iset.smartrecruit.offers.model.ExternalApplicationStatus;
import tn.iset.smartrecruit.offers.repository.CandidatureExterneRepository;
import tn.iset.smartrecruit.offers.security.AuthFacade;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidatureExterneServiceTest {
    @Mock CandidatureExterneRepository repository;
    @Mock AuthFacade auth;

    private CandidatureExterneService service;

    @BeforeEach
    void setUp() {
        service = new CandidatureExterneService(repository, auth);
        when(auth.currentUserId()).thenReturn("candidate@example.com");
    }

    @Test
    void recordsRedirectWithAuthenticatedCandidateAndRedirectedStatus() {
        when(repository.save(any(CandidatureExterne.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var request = new CandidatureExterneRequest(
                "provider-123", "Developer", "Example Co", "https://jobs.example/apply", "ExampleJobs");

        var result = service.recordRedirect(request);

        assertEquals("provider-123", result.offreId());
        assertEquals("Developer", result.titre());
        assertEquals("Example Co", result.entreprise());
        assertEquals("https://jobs.example/apply", result.urlExterne());
        assertEquals("ExampleJobs", result.source());
        assertEquals(ExternalApplicationStatus.REDIRECTED, result.statut());
        verify(repository).save(argThat(c -> "candidate@example.com".equals(c.getCandidatId())));
    }

    @Test
    void listsOnlyAuthenticatedCandidateRedirects() {
        CandidatureExterne candidateRedirect = CandidatureExterne.builder()
                .id("redirect-1")
                .candidatId("candidate@example.com")
                .offreId("provider-123")
                .statut(ExternalApplicationStatus.REDIRECTED)
                .build();
        when(repository.findByCandidatIdOrderByDateClicDesc("candidate@example.com"))
                .thenReturn(List.of(candidateRedirect));

        var result = service.mesCandidatures();

        assertEquals(1, result.size());
        assertEquals("redirect-1", result.get(0).id());
        assertEquals(ExternalApplicationStatus.REDIRECTED, result.get(0).statut());
        verify(repository).findByCandidatIdOrderByDateClicDesc("candidate@example.com");
    }
}
